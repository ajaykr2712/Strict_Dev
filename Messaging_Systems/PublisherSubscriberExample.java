public class PublisherSubscriberExample {
    static class Broker {
        private final java.util.Map<String, java.util.List<java.util.function.Consumer<String>>> topics = new java.util.HashMap<>();
        void subscribe(String topic, java.util.function.Consumer<String> consumer){ topics.computeIfAbsent(topic, k -> new java.util.ArrayList<>()).add(consumer); }
        void publish(String topic, String message){ for (var c : topics.getOrDefault(topic, java.util.List.of())) c.accept(message); }
    }

    public static void main(String[] args){
        Broker broker = new Broker();
        broker.subscribe("orders", msg -> System.out.println("Analytics received: " + msg));
        broker.subscribe("orders", msg -> System.out.println("Billing received: " + msg));
        broker.publish("orders", "order-123 created");
        broker.publish("orders", "order-123 paid");
    }
}
