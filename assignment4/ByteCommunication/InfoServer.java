import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;

class InfoService implements IInfoService {
    public String get_road_info(int road_ID) {
        // Simple implementation
        switch(road_ID) {
            case 1: return "Highway, good condition";
            case 2: return "County road, construction work";
            case 3: return "Mountain road, icy conditions";
            default: return "Unknown road";
        }
    }
    
    public String get_temp(String city) {
        // Simple implementation
        switch(city.toLowerCase()) {
            case "london": return "15°C";
            case "paris": return "18°C";
            case "berlin": return "12°C";
            default: return "No data available";
        }
    }
}

public class InfoServer {
    public static void main(String args[]) {
        new Configuration();
        
        // Create service object
        InfoService service = new InfoService();
        
        // Use the generic server proxy instead of a custom transformer
        ByteStreamTransformer transformer = new GenericServerProxy(service);
        
        Entry myAddr = new Entry("localhost", 9001);
        Registry.instance().put("InfoServer", myAddr);
        
        // Register with the NamingService
        Entry namingServiceAddr = new Entry("localhost", 9000);
        Registry.instance().put("NamingService", namingServiceAddr);
        
        Requestor requestor = new Requestor("InfoServer");
        Marshaller m = new Marshaller();
        
        // Registration message: register:serverName:host:port
        Message regMsg = new Message("InfoServer", 
                "register:InfoServer:" + myAddr.dest() + ":" + myAddr.port());
        byte[] regBytes = m.marshal(regMsg);
        byte[] regResponse = requestor.deliver_and_wait_feedback(namingServiceAddr, regBytes);
        
        Message regAnswer = m.unmarshal(regResponse);
        System.out.println("Registration response: " + regAnswer.data);
        
        Replyer r = new Replyer("InfoServer", myAddr);
        
        System.out.println("InfoServer started at " + myAddr.dest() + ":" + myAddr.port());
        
        while (true) {
            r.receive_transform_and_send_feedback(transformer);
        }
    }
}