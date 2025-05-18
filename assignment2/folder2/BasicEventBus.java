package folder2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ==== Interfata eveniment ====
interface Event {}

// ==== Interfata Subscriber ====
interface Subscriber {
    void handleEvent(Event event);
}

class BasicEventBus {
    private Map<Class<? extends Event>, List<Subscriber>> subscribersMap = new HashMap<>();

    public void register(Class<? extends Event> eventType, Subscriber subscriber) {
        subscribersMap.computeIfAbsent(eventType, k -> new ArrayList<>()).add(subscriber);
    }

    public void unregister(Class<? extends Event> eventType, Subscriber subscriber) {
        List<Subscriber> subscribers = subscribersMap.get(eventType);
        if (subscribers != null) {
            subscribers.remove(subscriber);
        }
    }

    public void post(Event event) {
        Class<? extends Event> eventType = event.getClass();
        List<Subscriber> subscribers = subscribersMap.get(eventType);
        if (subscribers != null) {
            for (Subscriber subscriber : subscribers) {
                subscriber.handleEvent(event);
            }
        }
    }
}