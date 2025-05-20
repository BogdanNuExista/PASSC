import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;
import Commons.Address;

public class JavaClientForDotNetServer {
    public static void main(String args[]) {
        new Configuration();
        
        System.out.println("Java client accessing .NET server...");
        
        try {
            // Connect directly to NamingService to lookup server
            Entry namingServiceAddr = new Entry("localhost", 9000);
            Requestor requestor = new Requestor("JavaClient");
            Marshaller m = new Marshaller();
            
            // Lookup the .NET server
            System.out.println("Looking up DotNetInfoServer...");
            Message lookupMsg = new Message("JavaClient", "lookup:DotNetInfoServer");
            byte[] lookupBytes = m.marshal(lookupMsg);
            byte[] lookupResponse = requestor.deliver_and_wait_feedback(namingServiceAddr, lookupBytes);
            Message lookupAnswer = m.unmarshal(lookupResponse);
            
            if (lookupAnswer.data.equals("NOT_FOUND")) {
                System.out.println("DotNetInfoServer not found in NamingService");
                return;
            }
            
            // Parse the response
            System.out.println("Server found: " + lookupAnswer.data);
            String[] parts = lookupAnswer.data.split(":");
            String serverHost = parts[0];
            int serverPort = Integer.parseInt(parts[1]);
            Entry serverAddr = new Entry(serverHost, serverPort);
            
            // Now make direct calls to the server
            // Call road info
            Message roadRequest = new Message("JavaClient", "get_road_info:1");
            byte[] roadBytes = m.marshal(roadRequest);
            System.out.println("Sending request to " + serverAddr.dest() + ":" + serverAddr.port());
            byte[] roadResponse = requestor.deliver_and_wait_feedback(serverAddr, roadBytes);
            Message roadAnswer = m.unmarshal(roadResponse);
            System.out.println("Road info from .NET server: " + roadAnswer.data);
            
            // Call temperature info
            Message tempRequest = new Message("JavaClient", "get_temp:London");
            byte[] tempBytes = m.marshal(tempRequest);
            byte[] tempResponse = requestor.deliver_and_wait_feedback(serverAddr, tempBytes);
            Message tempAnswer = m.unmarshal(tempResponse);
            System.out.println("Temperature from .NET server: " + tempAnswer.data);
            
        } catch (Exception e) {
            System.out.println("Error calling .NET service: " + e.getMessage());
            e.printStackTrace();
        }
    }
}