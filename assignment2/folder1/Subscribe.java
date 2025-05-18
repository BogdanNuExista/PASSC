package folder1;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {}

class ImprovedEventBus {
    private Map<Class<?>, List<MethodInvoker>> subscribersMap = new HashMap<>();

    public void register(Object subscriber, Class<?> eventType) {
        Method method = findSubscribeMethod(subscriber, eventType);
        if (method == null) {
            throw new IllegalArgumentException("No @Subscribe method for " + eventType);
        }
        method.setAccessible(true); // pentru a face metodele private accesibile
        subscribersMap.computeIfAbsent(eventType, k -> new ArrayList<>())
                      .add(new MethodInvoker(subscriber, method));
    }

    public void unregister(Object subscriber, Class<?> eventType) {
        List<MethodInvoker> invokers = subscribersMap.get(eventType);
        if (invokers != null) {
            invokers.removeIf(invoker -> invoker.subscriber == subscriber);
        }
    }

    public void post(Object event) {
        Class<?> eventType = event.getClass();
        List<MethodInvoker> invokers = subscribersMap.get(eventType);
        if (invokers != null) {
            for (MethodInvoker invoker : new ArrayList<>(invokers)) { 
                try {
                    invoker.method.invoke(invoker.subscriber, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Method findSubscribeMethod(Object subscriber, Class<?> eventType) {
        for (Method method : subscriber.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Class<?>[] params = method.getParameterTypes(); 
                if (params.length == 1 && params[0] == eventType) {
                    return method;
                }
            }
        }
        return null;
    }

    private static class MethodInvoker {
        Object subscriber;
        Method method;
        MethodInvoker(Object subscriber, Method method) {
            this.subscriber = subscriber;
            this.method = method;
        }
    }
}