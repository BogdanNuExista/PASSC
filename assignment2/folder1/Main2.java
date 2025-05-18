package folder1;

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
    private ImprovedEventBus eventBus;
    private String name;

    public NewsAgency(String name, ImprovedEventBus eventBus) {
        this.name = name;
        this.eventBus = eventBus;
    }

    public void publishNews(String domain, String content) {
        switch (domain) {
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
    private ImprovedEventBus eventBus;

    public Person(String name, ImprovedEventBus eventBus) {
        this.name = name;
        this.eventBus = eventBus;
    }

    public void subscribeToDomain(String domain) {
        switch (domain) {
            case "sports":
                eventBus.register(this, SportsNewsEvent.class); 
                break;
            case "politics":
                eventBus.register(this, PoliticsNewsEvent.class);
                break;
        }
    }

    public void unsubscribeFromDomain(String domain) {
        switch (domain) {
            case "sports":
                eventBus.unregister(this, SportsNewsEvent.class);
                break;
            case "politics":
                eventBus.unregister(this, PoliticsNewsEvent.class);
                break;
        }
    }

    @Subscribe
    public void receiveSportsNews(SportsNewsEvent event) {
        System.out.println(name + " [Sports] >> " + event.getContent());
    }

    @Subscribe
    public void receivePoliticsNews(PoliticsNewsEvent event) {
        System.out.println(name + " [Politics] >> " + event.getContent());
    }
}

// ==== Main Class ====
public class Main2 {
    public static void main(String[] args) {
        ImprovedEventBus eventBus = new ImprovedEventBus();
        NewsAgency agency = new NewsAgency("Global News", eventBus);

        Person alice = new Person("Alice", eventBus);
        Person bob = new Person("Bob", eventBus);

        alice.subscribeToDomain("sports");
        bob.subscribeToDomain("politics");

        System.out.println("\n=== News Publishing Simulation ===");
        agency.publishNews("sports", "Team wins championship!");
        agency.publishNews("politics", "New election announced.");
        agency.publishNews("sports", "Player breaks record!");

        alice.unsubscribeFromDomain("sports");
        agency.publishNews("sports", "Another sports update!"); // Alice won't receive this
    }
}