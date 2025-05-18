package folder11;

// ==== News Events ====
class NewsEvent {
    protected final String content;
    public NewsEvent(String content) { this.content = content; }
    public String getContent() { return content; }
}

class SportsNewsEvent extends NewsEvent {
    public SportsNewsEvent(String content) { super(content); }
}

class PoliticsNewsEvent extends NewsEvent {
    public PoliticsNewsEvent(String content) { super(content); }
}

// ==== Publisher (News Agency) ====
class NewsAgency {
    private MethodNameEventBus eventBus;
    private String name;

    public NewsAgency(String name, MethodNameEventBus eventBus) {
        this.name = name;
        this.eventBus = eventBus;
    }

    public void publishNews(String domain, String content) {
        switch(domain) {
            case "sports":
                eventBus.post(new SportsNewsEvent(name + ": " + content));
                break;
            case "politics":
                eventBus.post(new PoliticsNewsEvent(name + ": " + content));
                break;
        }
    }
}

// ==== Subscribers (People) ====
class Person {
    private String name;
    private MethodNameEventBus eventBus;

    public Person(String name, MethodNameEventBus eventBus) {
        this.name = name;
        this.eventBus = eventBus;
    }

    public void subscribeToDomain(String domain) {
        switch(domain) {
            case "sports":
                eventBus.register(SportsNewsEvent.class, this, "handleSports");
                break;
            case "politics":
                eventBus.register(PoliticsNewsEvent.class, this, "handlePolitics");
                break;
        }
    }

    public void unsubscribeFromDomain(String domain) {
        switch(domain) {
            case "sports":
                eventBus.unregister(SportsNewsEvent.class, this);
                break;
            case "politics":
                eventBus.unregister(PoliticsNewsEvent.class, this);
                break;
        }
    }

    public void handleSports(SportsNewsEvent event) {
        System.out.println(name + " [Sports] >> " + event.getContent());
    }

    public void handlePolitics(PoliticsNewsEvent event) {
        System.out.println(name + " [Politics] >> " + event.getContent());
    }
}

// ==== Main Class ====
public class Main22 {
    public static void main(String[] args) {
        MethodNameEventBus eventBus = new MethodNameEventBus();
        NewsAgency agency = new NewsAgency("Global News", eventBus);

        Person alice = new Person("Alice", eventBus);
        Person bob = new Person("Bob", eventBus);

        alice.subscribeToDomain("sports");
        bob.subscribeToDomain("politics");

        System.out.println("\n=== News Publishing (Method Name Approach) ===");
        agency.publishNews("sports", "Team wins championship!");
        agency.publishNews("politics", "New election announced.");
        agency.publishNews("sports", "Player breaks record!");

        alice.unsubscribeFromDomain("sports");
        agency.publishNews("sports", "Another sports update!"); // Alice won't receive
    }
}