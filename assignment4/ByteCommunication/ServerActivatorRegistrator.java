public class ServerActivatorRegistrator {
    public static void main(String[] args) {
        new Configuration();
        
        // Register InfoServer and MathServer with the Activator
        ActivatorClient.registerServer("InfoServer", "InfoServer");
        ActivatorClient.registerServer("MathServer", "MathServer");
        
        System.out.println("Servers registered with Activator");
    }
}