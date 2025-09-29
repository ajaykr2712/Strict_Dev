public class EventBusExample {
    interface Event {}
    static class UserRegistered implements Event { final String email; UserRegistered(String email){ this.email = email; } }

    interfacdde Subscriber<E extends Event> { void handle(E event); }

    static class EventBus {
        private final java.util.Map<Class<?>, java.util.List<Subscriber<?>>> subs = new java.util.HashMap<>();
        synchronized <E extends Event> void subscribe(Class<E> type, Subscriber<E> s){
            subs.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(s);
        }
        @SuppressWarnings("unchecked")
        void publish(Event e){
            var list = subs.getOrDefault(e.getClass(), java.util.List.of());
            for (Subscriber<?> s : list){ ((Subscriber<Event>) s).handle(e); }
        }
    }

    public static void main(String[] args){
        EventBus bus = new EventBus();
        bus.subscribe(UserRegistered.class, e -> System.out.println("Send welcome email to " + e.email));
        bus.subscribe(UserRegistered.class, e -> System.out.println("Create CRM contact for " + e.email));
        bus.publish(new UserRegistered("user@example.com"));
    }
}
