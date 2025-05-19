import RequestReply.*;
import MessageMarshaller.*;
import java.lang.reflect.*;
import java.util.Arrays;

public class GenericServerProxy implements ByteStreamTransformer {
    private Object serviceObject;
    private Class<?> serviceClass;
    
    public GenericServerProxy(Object service) {
        this.serviceObject = service;
        this.serviceClass = service.getClass();
        System.out.println("Generic proxy created for: " + serviceClass.getName());
        System.out.println("Available methods:");
        for (Method m : serviceClass.getMethods()) {
            if (!m.getDeclaringClass().equals(Object.class)) {
                System.out.println(" - " + m.getName() + Arrays.toString(m.getParameterTypes()));
            }
        }
    }
    
    public byte[] transform(byte[] in) {
        Message requestMsg;
        Marshaller m = new Marshaller();
        requestMsg = m.unmarshal(in);
        
        // Parse the request format: operation:param1,param2,...
        String data = requestMsg.data;
        String operation = data.substring(0, data.indexOf(":"));
        String paramStr = data.substring(data.indexOf(":")+1);
        
        Message responseMsg;
        
        try {
            // Find all methods with the requested name
            Method[] methods = serviceClass.getMethods();
            Method targetMethod = null;
            
            for (Method method : methods) {
                if (method.getName().equals(operation)) {
                    targetMethod = method;
                    break;
                }
            }
            
            if (targetMethod == null) {
                responseMsg = new Message(serviceClass.getSimpleName(), 
                                         "Method not found: " + operation);
            } else {
                // Process parameters based on method's parameter types
                Object[] params = parseParameters(paramStr, targetMethod.getParameterTypes());
                
                // Invoke the method
                Object result = targetMethod.invoke(serviceObject, params);
                
                // Return the result
                responseMsg = new Message(serviceClass.getSimpleName(), 
                                         result != null ? result.toString() : "null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseMsg = new Message(serviceClass.getSimpleName(), 
                                     "Error: " + e.getMessage());
        }
        
        return m.marshal(responseMsg);
    }
    
    private Object[] parseParameters(String paramStr, Class<?>[] paramTypes) {
        if (paramStr.isEmpty() && paramTypes.length == 0) {
            return new Object[0];
        }
        
        String[] paramStrs = paramStr.split(",");
        Object[] params = new Object[paramTypes.length];
        
        for (int i = 0; i < paramTypes.length; i++) {
            String value = i < paramStrs.length ? paramStrs[i] : null;
            
            if (paramTypes[i] == int.class || paramTypes[i] == Integer.class) {
                params[i] = Integer.parseInt(value);
            } else if (paramTypes[i] == float.class || paramTypes[i] == Float.class) {
                params[i] = Float.parseFloat(value);
            } else if (paramTypes[i] == double.class || paramTypes[i] == Double.class) {
                params[i] = Double.parseDouble(value);
            } else if (paramTypes[i] == boolean.class || paramTypes[i] == Boolean.class) {
                params[i] = Boolean.parseBoolean(value);
            } else {
                params[i] = value; // String or null
            }
        }
        
        return params;
    }
}