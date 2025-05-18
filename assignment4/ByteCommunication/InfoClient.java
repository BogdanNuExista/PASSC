import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;

public class InfoClient {
    public static void main(String args[]) {
        new Configuration();
        
        // Get NamingService address
        Entry namingServiceAddr = new Entry("localhost", 9000);
        Registry.instance().put("NamingService", namingServiceAddr);
        
        Requestor requestor = new Requestor("Client");
        Marshaller m = new Marshaller();
        
        // Lookup InfoServer address from NamingService
        Message lookupMsg = new Message("Client", "lookup:InfoServer");
        byte[] lookupBytes = m.marshal(lookupMsg);
        byte[] lookupResponse = requestor.deliver_and_wait_feedback(namingServiceAddr, lookupBytes);
        Message lookupAnswer = m.unmarshal(lookupResponse);
        
        // Parse the response: host:port
        String response = lookupAnswer.data;
        if (response.equals("NOT_FOUND")) {
            System.out.println("InfoServer not found in NamingService.");
            return;
        }
        
        String[] parts = response.split(":");
        String serverHost = parts[0];
        int serverPort = Integer.parseInt(parts[1]);
        
        Entry serverAddr = new Entry(serverHost, serverPort);
        
        // Request road info
        Message roadRequest = new Message("Client", "get_road_info:1");
        byte[] bytes = m.marshal(roadRequest);
        byte[] roadResponse = requestor.deliver_and_wait_feedback(serverAddr, bytes);
        Message roadAnswer = m.unmarshal(roadResponse);
        System.out.println("Road info: " + roadAnswer.data);

        // Request temperature
        Message tempRequest = new Message("Client", "get_temp:London");
        bytes = m.marshal(tempRequest);
        byte[] tempResponse = requestor.deliver_and_wait_feedback(serverAddr, bytes);
        Message tempAnswer = m.unmarshal(tempResponse);
        System.out.println("Temperature: " + tempAnswer.data);
    }
}