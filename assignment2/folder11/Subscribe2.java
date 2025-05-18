package folder11;

import java.lang.reflect.Method;
import java.util.*;

class MethodNameEventBus {
    private Map<Class<?>, List<MethodInvoker>> subscribersMap = new HashMap<>();

    public void register(Class<?> eventType, Object subscriber, String methodName) {
        try {
            Method method = subscriber.getClass().getDeclaredMethod(methodName, eventType);
            method.setAccessible(true);
            subscribersMap.computeIfAbsent(eventType, k -> new ArrayList<>())
                         .add(new MethodInvoker(subscriber, method));
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Method " + methodName + " not found in " + subscriber.getClass().getName());
        }
    }

    public void unregister(Class<?> eventType, Object subscriber) {
        List<MethodInvoker> invokers = subscribersMap.get(eventType);
        if(invokers != null) {
            invokers.removeIf(invoker -> invoker.subscriber == subscriber);
        }
    }

    public void post(Object event) {
        Class<?> eventType = event.getClass();
        List<MethodInvoker> invokers = subscribersMap.get(eventType);
        if(invokers != null) {
            for(MethodInvoker invoker : new ArrayList<>(invokers)) {
                try {
                    invoker.method.invoke(invoker.subscriber, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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