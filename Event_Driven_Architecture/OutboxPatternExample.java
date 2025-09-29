// OutboxPatternExample.java
// Demonstrates simplified Outbox pattern: domain changes + event persisted atomically, dispatcher relays to broker.

import java.util.*;
import java.util.concurrent.*;

public class OutboxPatternExample {
    static class Order { final String id; String status = "NEW"; Order(String id){this.id=id;} }
    static class OutboxEvent { final String id = UUID.randomUUID().toString(); final String aggregateId; final String type; final String payload; boolean dispatched=false; OutboxEvent(String agg,String type,String payload){this.aggregateId=agg;this.type=type;this.payload=payload;} }

    static class InMemoryDB {
        final Map<String, Order> orders = new HashMap<>();
        final List<OutboxEvent> outbox = new ArrayList<>();
        synchronized Order createOrder() {
            var o = new Order(UUID.randomUUID().toString());
            orders.put(o.id, o);
            outbox.add(new OutboxEvent(o.id, "OrderCreated", "{id:"+o.id+"}"));
            return o;
        }
        synchronized void markPaid(String id) {
            var o = orders.get(id); if (o==null) return; o.status = "PAID";
            outbox.add(new OutboxEvent(o.id, "OrderPaid", "{id:"+o.id+"}"));
        }
        synchronized List<OutboxEvent> fetchUndispatched(int limit) {
            return outbox.stream().filter(e->!e.dispatched).limit(limit).toList();
        }
        synchronized void markDispatched(Collection<OutboxEvent> events) { events.forEach(e->e.dispatched=true); }
    }

    static class EventDispatcher implements AutoCloseable {
        private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        private final InMemoryDB db;
        EventDispatcher(InMemoryDB db){this.db=db;}
        void start(){ exec.scheduleAtFixedRate(this::pollAndDispatch, 0, 200, TimeUnit.MILLISECONDS);}        
        private void pollAndDispatch(){
            var batch = db.fetchUndispatched(10);
            if (batch.isEmpty()) return;
            // Simulate publish to broker
            batch.forEach(e -> System.out.println("Publish -> topic=orders type="+e.type+" payload="+e.payload));
            db.markDispatched(batch);
        }
        @Override public void close(){ exec.shutdown(); }
    }

    public static void main(String[] args) throws Exception {
        var db = new InMemoryDB();
        try (var dispatcher = new EventDispatcher(db)) {
            dispatcher.start();
            var o1 = db.createOrder();
            var o2 = db.createOrder();
            db.markPaid(o1.id);
            Thread.sleep(800);
            db.markPaid(o2.id);
            Thread.sleep(800);
        }
    }
}
