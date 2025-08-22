import java.util.*;

public class RequestReplyExample {
    static class Broker {
        private final Map<String, Queue<String>> topics = new HashMap<>();
        void publish(String topic, String msg){ topics.computeIfAbsent(topic,k->new ArrayDeque<>()).add(msg); }
        String poll(String topic){ Queue<String> q = topics.getOrDefault(topic, new ArrayDeque<>()); return q.poll(); }
    }
    static class Client {
        private final Broker broker;
        Client(Broker b){ this.broker = b; }
        String call(String serviceTopic, String payload){
            String replyTo = UUID.randomUUID().toString();
            broker.publish(serviceTopic, payload + "|replyTo=" + replyTo);
            // simulate service handling synchronously for demo
            Service.handle(broker, serviceTopic);
            return broker.poll(replyTo);
        }
    }
    static class Service {
        static void handle(Broker broker, String serviceTopic){
            String req = broker.poll(serviceTopic);
            if (req == null) return;
            String[] parts = req.split("\\|replyTo=");
            String payload = parts[0]; String replyTo = parts[1];
            broker.publish(replyTo, "processed:" + payload);
        }
    }
    public static void main(String[] args){
        Broker broker = new Broker();
        Client client = new Client(broker);
        String response = client.call("svc.orders", "create order 42");
        System.out.println("Response: " + response);
    }
}
