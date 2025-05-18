import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;
import Commons.Address;
import java.util.HashMap;
import java.util.Map;

class NamingServiceTransformer implements ByteStreamTransformer {
    private Map<String, Address> serverRegistry = new HashMap<>();
    
    public byte[] transform(byte[] in) {
        Message requestMsg;
        Marshaller m = new Marshaller();
        requestMsg = m.unmarshal(in);
        
        String data = requestMsg.data;
        String operation = data.substring(0, data.indexOf(":"));
        String param = data.substring(data.indexOf(":")+1);
        
        Message responseMsg;
        
        if (operation.equals("register")) {
            // Format: register:serverName:host:port
            String[] parts = param.split(":");
            String serverName = parts[0];
            String host = parts[1];
            int port = Integer.parseInt(parts[2]);
            
            Address serverAddr = new Entry(host, port);
            serverRegistry.put(serverName, serverAddr);
            
            responseMsg = new Message("NamingService", "OK");
        } 
        else if (operation.equals("lookup")) {
            // Format: lookup:serverName
            String serverName = param;
            Address serverAddr = serverRegistry.get(serverName);
            
            if (serverAddr != null) {
                responseMsg = new Message("NamingService", 
                        serverAddr.dest() + ":" + serverAddr.port());
            } else {
                responseMsg = new Message("NamingService", "NOT_FOUND");
            }
        } 
        else {
            responseMsg = new Message("NamingService", "UNKNOWN_OPERATION");
        }
        
        return m.marshal(responseMsg);
    }
}

public class NamingService {
    public static void main(String[] args) {
        new Configuration();
        
        ByteStreamTransformer transformer = new NamingServiceTransformer();
        
        // NamingService has a fixed well-known address
        Entry myAddr = new Entry("localhost", 9000);
        Registry.instance().put("NamingService", myAddr);
        
        Replyer r = new Replyer("NamingService", myAddr);
        
        System.out.println("NamingService started at " + myAddr.dest() + ":" + myAddr.port());
        
        while (true) {
            r.receive_transform_and_send_feedback(transformer);
        }
    }
}