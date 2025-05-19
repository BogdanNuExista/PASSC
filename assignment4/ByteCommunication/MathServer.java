import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;

class MathService implements IMathService {
    public float do_add(float a, float b) {
        return a + b;
    }
    
    public float do_sqr(float a) {
        return (float)Math.sqrt(a);
    }
}

public class MathServer {
    public static void main(String args[]) {
        new Configuration();
        
        // Create service object
        MathService service = new MathService();
        
        // Use the generic server proxy instead of a custom transformer
        ByteStreamTransformer transformer = new GenericServerProxy(service);
        
        Entry myAddr = new Entry("localhost", 9002);
        Registry.instance().put("MathServer", myAddr);
        
        // Register with the NamingService
        Entry namingServiceAddr = new Entry("localhost", 9000);
        Registry.instance().put("NamingService", namingServiceAddr);
        
        Requestor requestor = new Requestor("MathServer");
        Marshaller m = new Marshaller();
        
        // Registration message: register:serverName:host:port
        Message regMsg = new Message("MathServer", 
                "register:MathServer:" + myAddr.dest() + ":" + myAddr.port());
        byte[] regBytes = m.marshal(regMsg);
        byte[] regResponse = requestor.deliver_and_wait_feedback(namingServiceAddr, regBytes);
        
        Message regAnswer = m.unmarshal(regResponse);
        System.out.println("Registration response: " + regAnswer.data);
        
        Replyer r = new Replyer("MathServer", myAddr);
        
        System.out.println("MathServer started at " + myAddr.dest() + ":" + myAddr.port());
        
        while (true) {
            r.receive_transform_and_send_feedback(transformer);
        }
    }
}