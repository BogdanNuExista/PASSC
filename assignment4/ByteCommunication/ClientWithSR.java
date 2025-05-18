import ByteSendReceive.*;
import MessageMarshaller.*;
import Registry.*;
import Commons.Address;

public class ClientWithSR
{
	public static void main(String args[])
	{
		new Configuration();

                Address dest=Registry.instance().get("Server");

		Message msg= new Message("Client1","How are you");

		ByteSender cs = new ByteSender("Client1");
		
		Marshaller m = new Marshaller();
			
		byte[] bytes = m.marshal(msg);

		cs.deliver(dest, bytes);

                Address myAddr = Registry.instance().get("Client1");
	
		ByteReceiver cr = new ByteReceiver("Client1", myAddr);
            
                bytes=cr.receive(); 

		Message answer = m.unmarshal(bytes);

		System.out.println("Client received message "+answer.data+" from "+answer.sender);
	}

}