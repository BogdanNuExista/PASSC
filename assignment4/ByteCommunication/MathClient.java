import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;

public class MathClient {
    public static void main(String args[]) {
        new Configuration();
        
        // Get NamingService address
        Entry namingServiceAddr = new Entry("localhost", 9000);
        Registry.instance().put("NamingService", namingServiceAddr);
        
        Requestor requestor = new Requestor("Client");
        Marshaller m = new Marshaller();
        
        // Lookup MathServer address from NamingService
        Message lookupMsg = new Message("Client", "lookup:MathServer");
        byte[] lookupBytes = m.marshal(lookupMsg);
        byte[] lookupResponse = requestor.deliver_and_wait_feedback(namingServiceAddr, lookupBytes);
        Message lookupAnswer = m.unmarshal(lookupResponse);
        
        // Parse the response: host:port
        String response = lookupAnswer.data;
        if (response.equals("NOT_FOUND")) {
            System.out.println("MathServer not found in NamingService.");
            return;
        }
        
        String[] parts = response.split(":");
        String serverHost = parts[0];
        int serverPort = Integer.parseInt(parts[1]);
        
        Entry serverAddr = new Entry(serverHost, serverPort);
        
        // Request addition
        Message addRequest = new Message("Client", "do_add:5.5,3.2");
        byte[] bytes = m.marshal(addRequest);
        byte[] addResponse = requestor.deliver_and_wait_feedback(serverAddr, bytes);
        Message addAnswer = m.unmarshal(addResponse);
        System.out.println("Addition result: " + addAnswer.data);

        // Request square root
        Message sqrRequest = new Message("Client", "do_sqr:16.0");
        bytes = m.marshal(sqrRequest);
        byte[] sqrResponse = requestor.deliver_and_wait_feedback(serverAddr, bytes);
        Message sqrAnswer = m.unmarshal(sqrResponse);
        System.out.println("Square root result: " + sqrAnswer.data);
    }
}