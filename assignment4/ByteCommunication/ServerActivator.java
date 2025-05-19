import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;
import java.util.*;
import java.io.*;

class ServerInfo {
    String serverName;
    String className;
    boolean isActive;
    Process process;
    Entry serverAddress;
    
    public ServerInfo(String name, String className) {
        this.serverName = name;
        this.className = className;
        this.isActive = false;
        this.process = null;
    }
}

class ActivatorTransformer implements ByteStreamTransformer {
    private Map<String, ServerInfo> registeredServers = new HashMap<>();
    private Entry namingServiceAddr;
    private Requestor requestor;
    private Marshaller marshaller;
    
    public ActivatorTransformer(Entry namingServiceAddr) {
        this.namingServiceAddr = namingServiceAddr;
        this.requestor = new Requestor("Activator");
        this.marshaller = new Marshaller();
    }
    
    public byte[] transform(byte[] in) {
        Message requestMsg;
        requestMsg = marshaller.unmarshal(in);
        
        String data = requestMsg.data;
        String operation = data.substring(0, data.indexOf(":"));
        String param = data.substring(data.indexOf(":")+1);
        
        Message responseMsg;
        
        if (operation.equals("register_server")) {
            // Format: register_server:serverName:serverClassName
            String[] parts = param.split(":");
            String serverName = parts[0];
            String className = parts[1];
            
            ServerInfo info = new ServerInfo(serverName, className);
            registeredServers.put(serverName, info);
            
            responseMsg = new Message("Activator", "OK");
        } 
        else if (operation.equals("activate")) {
            // Format: activate:serverName
            String serverName = param;
            ServerInfo info = registeredServers.get(serverName);
            
            if (info == null) {
                responseMsg = new Message("Activator", "SERVER_NOT_REGISTERED");
            } else {
                try {
                    if (!info.isActive || (info.process != null && !isProcessAlive(info.process))) {
                        // Start the server
                        activateServer(info);
                        
                        // Wait for server to start and register with NamingService
                        Thread.sleep(2000);
                        
                        // Lookup server address from NamingService
                        Message lookupMsg = new Message("Activator", "lookup:" + serverName);
                        byte[] lookupBytes = marshaller.marshal(lookupMsg);
                        byte[] lookupResponse = requestor.deliver_and_wait_feedback(namingServiceAddr, lookupBytes);
                        Message lookupAnswer = marshaller.unmarshal(lookupResponse);
                        
                        if (!lookupAnswer.data.equals("NOT_FOUND")) {
                            responseMsg = new Message("Activator", "ACTIVATED:" + lookupAnswer.data);
                        } else {
                            responseMsg = new Message("Activator", "ACTIVATION_FAILED");
                        }
                    } else {
                        responseMsg = new Message("Activator", "ALREADY_ACTIVE");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    responseMsg = new Message("Activator", "ACTIVATION_ERROR:" + e.getMessage());
                }
            }
        } 
        else if (operation.equals("deactivate")) {
            // Format: deactivate:serverName
            String serverName = param;
            ServerInfo info = registeredServers.get(serverName);
            
            if (info == null) {
                responseMsg = new Message("Activator", "SERVER_NOT_REGISTERED");
            } else if (!info.isActive) {
                responseMsg = new Message("Activator", "SERVER_NOT_ACTIVE");
            } else {
                try {
                    info.process.destroy();
                    info.isActive = false;
                    responseMsg = new Message("Activator", "DEACTIVATED");
                } catch (Exception e) {
                    responseMsg = new Message("Activator", "DEACTIVATION_ERROR:" + e.getMessage());
                }
            }
        }
        else {
            responseMsg = new Message("Activator", "UNKNOWN_OPERATION");
        }
        
        return marshaller.marshal(responseMsg);
    }
    
    private boolean isProcessAlive(Process process) {
        try {
            process.exitValue();
            return false; // If we get here, the process has exited
        } catch (IllegalThreadStateException e) {
            return true; // Process is still running
        }
    }
    
    private void activateServer(ServerInfo info) throws IOException {
        // Java command to launch the server
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = info.className;
        
        ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className);
        builder.redirectErrorStream(true);
        
        // Start the server process
        info.process = builder.start();
        info.isActive = true;
        
        // Optional: track output from the server
        final Process process = info.process;
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(info.serverName + ": " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

public class ServerActivator {
    public static void main(String[] args) {
        new Configuration();
        
        // Activator has a fixed well-known address
        Entry myAddr = new Entry("localhost", 9010);
        Registry.instance().put("Activator", myAddr);
        
        // Get NamingService address
        Entry namingServiceAddr = new Entry("localhost", 9000);
        Registry.instance().put("NamingService", namingServiceAddr);
        
        // Initialize activator transformer
        ByteStreamTransformer transformer = new ActivatorTransformer(namingServiceAddr);
        
        // Register with the NamingService
        Requestor requestor = new Requestor("Activator");
        Marshaller m = new Marshaller();
        
        Message regMsg = new Message("Activator", 
                "register:Activator:" + myAddr.dest() + ":" + myAddr.port());
        byte[] regBytes = m.marshal(regMsg);
        byte[] regResponse = requestor.deliver_and_wait_feedback(namingServiceAddr, regBytes);
        
        Message regAnswer = m.unmarshal(regResponse);
        System.out.println("Registration response: " + regAnswer.data);
        
        Replyer r = new Replyer("Activator", myAddr);
        
        System.out.println("ServerActivator started at " + myAddr.dest() + ":" + myAddr.port());
        
        while (true) {
            r.receive_transform_and_send_feedback(transformer);
        }
    }
}