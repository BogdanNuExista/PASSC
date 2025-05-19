public class MathClient {
    public static void main(String args[]) {
        new Configuration();
        
        // Create the dynamic proxy for the remote service
        IMathService mathService = DynamicClientProxy.createProxy(IMathService.class, "MathServer");
        
        // Use the proxy to call remote methods - as simple as calling local methods!
        float addResult = mathService.do_add(5.5f, 3.2f);
        System.out.println("Addition result: " + addResult);
        
        float sqrResult = mathService.do_sqr(16.0f);
        System.out.println("Square root result: " + sqrResult);
    }
}