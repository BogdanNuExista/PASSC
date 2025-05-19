public class InfoClient {
    public static void main(String args[]) {
        new Configuration();
        
        // Create the dynamic proxy for the remote service
        IInfoService infoService = DynamicClientProxy.createProxy(IInfoService.class, "InfoServer");
        
        // Use the proxy to call remote methods - as simple as calling local methods!
        String roadInfo = infoService.get_road_info(1);
        System.out.println("Road info: " + roadInfo);
        
        String tempInfo = infoService.get_temp("London");
        System.out.println("Temperature: " + tempInfo);
    }
}