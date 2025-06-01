package bonus;

public class TestBooks {
    public static void main(String[] args) {
        try {
            // Generate classes from schema if not already generated
            // SchemaToJava.main(new String[]{"books.xsd"});
            
            // Load test data
            BookShop shop = MyXMLDataBinder.CreateObjectFromXMLfile("books1.xml", BookShop.class);
            
            // Print loaded data
            System.out.println("Loaded BookShop contains " + shop.book.size() + " books");
            for (Bookdata book : shop.book) {
                System.out.println("\nBook ID: " + book.id);
                System.out.println("Title: " + book.title);
                System.out.println("Price: " + book.price);
                System.out.println("Description: " + book.description);
                System.out.println("Authors:");
                for (Persondata author : book.author) {
                    System.out.println("  - " + author.name + " " + author.surname);
                }
            }
            
            // Save modified data
            shop.book.get(0).title = "Updated Book Title";
            MyXMLDataBinder.CreateXMLFromObject(shop, "BookShop", "books_updated.xml");
            System.out.println("\nUpdated XML saved to books_updated.xml");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}