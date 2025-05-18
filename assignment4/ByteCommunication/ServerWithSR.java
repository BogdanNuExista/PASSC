import ByteSendReceive.*;
import MessageMarshaller.*;
import Registry.*;
import Commons.Address;

public class ServerWithSR
{
	public static void main(String args[])
	{
		new Configuration();
		
		Address myAddr = Registry.instance().get("Server");
		
		ByteReceiver sr = new ByteReceiver("Server", myAddr);
            
             Marshaller m = new Marshaller();

		byte[] bytes;

		while (true) {
		  
               bytes=sr.receive(); 

		   Message request = m.unmarshal(bytes);

		System.out.println("Server received message "+request.data+" from "+request.sender);

		ByteSender ss=new ByteSender("Server");
		
		Message answer = new Message("Server", "I am alive");

		bytes=m.marshal(answer);
	
		Address cliAddr = Registry.instance().get(request.sender);


		ss.deliver(cliAddr, bytes);
		}
		

	}

}