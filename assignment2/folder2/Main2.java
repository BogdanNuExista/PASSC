package folder2;

// ==== News Events ====
class NewsEvent implements Event {
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
    private BasicEventBus eventBus;
    private String name;

    public NewsAgency(String name, BasicEventBus eventBus) {
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
class Person implements Subscriber {
    private String name;
    private BasicEventBus eventBus;

    public Person(String name, BasicEventBus eventBus) {
        this.name = name;
        this.eventBus = eventBus;
    }

    public void subscribeToDomain(String domain) {
        switch (domain) {
            case "sports":
                eventBus.register(SportsNewsEvent.class, this);
                break;
            case "politics":
                eventBus.register(PoliticsNewsEvent.class, this);
                break;
        }
    }

    public void unsubscribeFromDomain(String domain) {
        switch (domain) {
            case "sports":
                eventBus.unregister(SportsNewsEvent.class, this);
                break;
            case "politics":
                eventBus.unregister(PoliticsNewsEvent.class, this);
                break;
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (event instanceof SportsNewsEvent) {
            System.out.println(name + " [Sports] >> " + ((SportsNewsEvent) event).getContent());
        } else if (event instanceof PoliticsNewsEvent) {
            System.out.println(name + " [Politics] >> " + ((PoliticsNewsEvent) event).getContent());
        }
    }
}

// ==== Main Class ====
public class Main2 {
    public static void main(String[] args) {
        BasicEventBus eventBus = new BasicEventBus();
        NewsAgency agency = new NewsAgency("Global News", eventBus);

        Person alice = new Person("Alice", eventBus);
        Person bob = new Person("Bob", eventBus);

        alice.subscribeToDomain("sports");
        bob.subscribeToDomain("politics");

        System.out.println("\n=== News Publishing Simulation ===");
        agency.publishNews("sports", "Team wins championship!");
        agency.publishNews("politics", "New election announced.");
        agency.publishNews("sports", "Player breaks record!");

        // Unsubscribe Alice from sports
        alice.unsubscribeFromDomain("sports");
        agency.publishNews("sports", "Another sports update!"); // Alice won't receive this
    }
}