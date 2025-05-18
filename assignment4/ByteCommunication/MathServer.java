import RequestReply.*;
import MessageMarshaller.*;
import Registry.*;

class MathTransformer implements ByteStreamTransformer {
    private MathService service;

    public MathTransformer(MathService s) {
        service = s;
    }

    public byte[] transform(byte[] in) {
        Message requestMsg;
        Marshaller m = new Marshaller();
        requestMsg = m.unmarshal(in);

        // Parse the request format: operation:param1,param2
        String data = requestMsg.data;
        String operation = data.substring(0, data.indexOf(":"));
        String params = data.substring(data.indexOf(":")+1);
        
        Message responseMsg;
        
        if (operation.equals("do_add")) {
            String[] values = params.split(",");
            float a = Float.parseFloat(values[0]);
            float b = Float.parseFloat(values[1]);
            float result = service.do_add(a, b);
            responseMsg = new Message("MathServer", String.valueOf(result));
        } else if (operation.equals("do_sqr")) {
            float a = Float.parseFloat(params);
            float result = service.do_sqr(a);
            responseMsg = new Message("MathServer", String.valueOf(result));
        } else {
            responseMsg = new Message("MathServer", "Unknown operation");
        }

        return m.marshal(responseMsg);
    }
}

class MathService {
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
        
        ByteStreamTransformer transformer = new MathTransformer(new MathService());
        
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