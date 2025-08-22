import java.util.*;
import java.util.concurrent.*;

public class IdempotencyKeyExample {
    static class PaymentService {
        private final Map<String, String> processed = new ConcurrentHashMap<>();
        String process(String idempotencyKey, String request){
            return processed.computeIfAbsent(idempotencyKey, k -> {
                System.out.println("Processing request: " + request);
                return UUID.randomUUID().toString();
            });
        }
    }
    public static void main(String[] args){
        PaymentService svc = new PaymentService();
        String key = "order-123";
        String r1 = svc.process(key, "charge $10");
        String r2 = svc.process(key, "charge $10");
        System.out.println("Same result? " + r1.equals(r2));
    }
}
