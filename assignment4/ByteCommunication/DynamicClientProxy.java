import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class DynamicClientProxy {
    // Create a proxy for a remote service interface
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass, String serverName) {
        // Get NamingService address
        Entry namingServiceAddr = new Entry("localhost", 9000);
        
        // Create a dynamic proxy with an invocation handler
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[] { interfaceClass },
            new RemoteInvocationHandler(serverName, namingServiceAddr)
        );
    }
    
    // InvocationHandler that handles method calls on the dynamic proxy
    private static class RemoteInvocationHandler implements InvocationHandler {
        private String serverName;
        private Entry namingServiceAddr;
        private Entry serverAddr;
        private Requestor requestor;
        private Marshaller marshaller;
        
        public RemoteInvocationHandler(String serverName, Entry namingServiceAddr) {
            this.serverName = serverName;
            this.namingServiceAddr = namingServiceAddr;
            this.requestor = new Requestor("Client");
            this.marshaller = new Marshaller();
        }
        
        private boolean lookupServerAddress() {
            // First, try to activate the server if needed
            boolean activated = ActivatorClient.activateServer(serverName);
            if (!activated) {
                return false;
            }
            
            // Lookup Server address from NamingService
            Message lookupMsg = new Message("Client", "lookup:" + serverName);
            byte[] lookupBytes = marshaller.marshal(lookupMsg);
            byte[] lookupResponse = requestor.deliver_and_wait_feedback(namingServiceAddr, lookupBytes);
            Message lookupAnswer = marshaller.unmarshal(lookupResponse);
            
            // Parse the response: host:port
            String response = lookupAnswer.data;
            if (response.equals("NOT_FOUND")) {
                return false;
            }
            
            String[] parts = response.split(":");
            String serverHost = parts[0];
            int serverPort = Integer.parseInt(parts[1]);
            
            serverAddr = new Entry(serverHost, serverPort);
            return true;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Don't proxy Object methods like toString(), equals(), etc.
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            // Ensure we have a valid server address
            if (serverAddr == null) {
                boolean success = lookupServerAddress();
                if (!success) {
                    throw new RuntimeException("Failed to locate or activate server: " + serverName);
                }
            }
            
            // Create the request message format: methodName:arg1,arg2,...
            StringBuilder requestData = new StringBuilder();
            requestData.append(method.getName()).append(":");
            
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) requestData.append(",");
                    requestData.append(args[i]);
                }
            }
            
            Message request = new Message("Client", requestData.toString());
            byte[] requestBytes = marshaller.marshal(request);
            
            try {
                // Send the request and get the response
                byte[] responseBytes = requestor.deliver_and_wait_feedback(serverAddr, requestBytes);
                Message response = marshaller.unmarshal(responseBytes);
                
                // Convert the response string to the actual return type
                String resultStr = response.data;
                Class<?> returnType = method.getReturnType();
                
                if (returnType == void.class) {
                    return null;
                } else if (returnType == String.class) {
                    return resultStr;
                } else if (returnType == int.class || returnType == Integer.class) {
                    return Integer.parseInt(resultStr);
                } else if (returnType == float.class || returnType == Float.class) {
                    return Float.parseFloat(resultStr);
                } else if (returnType == double.class || returnType == Double.class) {
                    return Double.parseDouble(resultStr);
                } else if (returnType == boolean.class || returnType == Boolean.class) {
                    return Boolean.parseBoolean(resultStr);
                }
                
                return null;
            } catch (Exception e) {
                // If connection fails, try to reactivate the server
                serverAddr = null;
                boolean success = lookupServerAddress();
                if (success) {
                    // Retry the request
                    return invoke(proxy, method, args);
                } else {
                    throw new RuntimeException("Failed to connect to server: " + serverName, e);
                }
            }
        }
    }
}