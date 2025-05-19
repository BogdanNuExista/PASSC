import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;

public class ActivatorClient {
    private static Entry activatorAddr = new Entry("localhost", 9010);
    private static Requestor requestor = new Requestor("ActivatorClient");
    private static Marshaller m = new Marshaller();
    
    public static void registerServer(String serverName, String serverClassName) {
        // Register a server with the Activator
        Message regMsg = new Message("ActivatorClient", 
                "register_server:" + serverName + ":" + serverClassName);
        byte[] regBytes = m.marshal(regMsg);
        byte[] regResponse = requestor.deliver_and_wait_feedback(activatorAddr, regBytes);
        
        Message regAnswer = m.unmarshal(regResponse);
        System.out.println("Server registration response: " + regAnswer.data);
    }
    
    public static boolean activateServer(String serverName) {
        // Request server activation
        Message activateMsg = new Message("ActivatorClient", "activate:" + serverName);
        byte[] activateBytes = m.marshal(activateMsg);
        byte[] activateResponse = requestor.deliver_and_wait_feedback(activatorAddr, activateBytes);
        
        Message activateAnswer = m.unmarshal(activateResponse);
        System.out.println("Activation response: " + activateAnswer.data);
        
        return activateAnswer.data.startsWith("ACTIVATED") || 
               activateAnswer.data.equals("ALREADY_ACTIVE");
    }
    
    public static void deactivateServer(String serverName) {
        // Request server deactivation
        Message deactivateMsg = new Message("ActivatorClient", "deactivate:" + serverName);
        byte[] deactivateBytes = m.marshal(deactivateMsg);
        byte[] deactivateResponse = requestor.deliver_and_wait_feedback(activatorAddr, deactivateBytes);
        
        Message deactivateAnswer = m.unmarshal(deactivateResponse);
        System.out.println("Deactivation response: " + deactivateAnswer.data);
    }
}