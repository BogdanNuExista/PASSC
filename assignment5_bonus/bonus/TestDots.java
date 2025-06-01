public class TestDots {
    public static void main(String[] args) {
        try {
            // Generate classes from schema
            SchemaToJava.main(new String[]{"dots.xsd"});
            
            // Load test data
            Dots dots = MyXMLDataBinder.CreateObjectFromXMLfile("dots.xml", Dots.class);
            
            // Print loaded data
            System.out.println("Loaded Dots contains " + dots.dot.size() + " points");
            for (Dot dot : dots.dot) {
                System.out.println("Point: (" + dot.x + ", " + dot.y + ")");
            }
            
            // Modify and save data
            Dot newDot = new Dot();
            newDot.x = 100;
            newDot.y = 200;
            dots.dot.add(newDot);
            
            MyXMLDataBinder.CreateXMLFromObject(dots, "dots", "dots_updated.xml");
            System.out.println("\nUpdated XML saved to dots_updated.xml");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}