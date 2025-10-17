import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Unit tests for OutboxPatternExample
 * Tests the transactional outbox pattern for reliable event publishing
 * ensuring domain changes and events are persisted atomically.
 */
public class OutboxPatternTest {

    /**
     * Mock classes for testing
     */
    static class Order {
        final String id;
        String status = "NEW";

        Order(String id) {
            this.id = id;
        }
    }

    static class OutboxEvent {
        final String id = UUID.randomUUID().toString();
        final String aggregateId;
        final String type;
        final String payload;
        boolean dispatched = false;

        OutboxEvent(String agg, String type, String payload) {
            this.aggregateId = agg;
            this.type = type;
            this.payload = payload;
        }
    }

    static class InMemoryDB {
        final Map<String, Order> orders = new HashMap<>();
        final List<OutboxEvent> outbox = new ArrayList<>();

        synchronized Order createOrder() {
            var o = new Order(UUID.randomUUID().toString());
            orders.put(o.id, o);
            outbox.add(new OutboxEvent(o.id, "OrderCreated", "{id:" + o.id + "}"));
            return o;
        }

        synchronized void markPaid(String id) {
            var o = orders.get(id);
            if (o == null) return;
            o.status = "PAID";
            outbox.add(new OutboxEvent(o.id, "OrderPaid", "{id:" + o.id + "}"));
        }

        synchronized List<OutboxEvent> fetchUndispatched(int limit) {
            return outbox.stream().filter(e -> !e.dispatched).limit(limit).toList();
        }

        synchronized void markDispatched(Collection<OutboxEvent> events) {
            events.forEach(e -> e.dispatched = true);
        }

        synchronized int getTotalEventCount() {
            return outbox.size();
        }

        synchronized int getDispatchedEventCount() {
            return (int) outbox.stream().filter(e -> e.dispatched).count();
        }
    }

    private InMemoryDB db;

    @Before
    public void setUp() {
        db = new InMemoryDB();
    }

    @Test
    public void testCreateOrder() {
        // Arrange & Act
        Order order = db.createOrder();

        // Assert
        assertNotNull("Order should be created", order);
        assertNotNull("Order should have ID", order.id);
        assertEquals("Order status should be NEW", "NEW", order.status);
        assertEquals("Order should be stored in DB", order, db.orders.get(order.id));
    }

    @Test
    public void testCreateOrderCreatesOutboxEvent() {
        // Arrange & Act
        Order order = db.createOrder();

        // Assert
        assertEquals("One outbox event should be created", 1, db.getTotalEventCount());
        OutboxEvent event = db.outbox.get(0);
        assertEquals("Event aggregate ID should match order", order.id, event.aggregateId);
        assertEquals("Event type should be OrderCreated", "OrderCreated", event.type);
        assertTrue("Payload should contain order ID", event.payload.contains(order.id));
        assertFalse("Event should not be dispatched initially", event.dispatched);
    }

    @Test
    public void testMarkPaid() {
        // Arrange
        Order order = db.createOrder();

        // Act
        db.markPaid(order.id);

        // Assert
        assertEquals("Order status should be PAID", "PAID", order.status);
        assertEquals("Two outbox events should exist", 2, db.getTotalEventCount());
    }

    @Test
    public void testMarkPaidCreatesOutboxEvent() {
        // Arrange
        Order order = db.createOrder();

        // Act
        db.markPaid(order.id);

        // Assert
        assertEquals("Two events should be created", 2, db.getTotalEventCount());
        OutboxEvent paidEvent = db.outbox.get(1);
        assertEquals("Second event aggregate ID should match order", order.id, paidEvent.aggregateId);
        assertEquals("Event type should be OrderPaid", "OrderPaid", paidEvent.type);
        assertTrue("Payload should contain order ID", paidEvent.payload.contains(order.id));
        assertFalse("Event should not be dispatched initially", paidEvent.dispatched);
    }

    @Test
    public void testMarkPaidWithNonExistentOrder() {
        // Arrange
        String nonExistentId = "non-existent-id";

        // Act
        db.markPaid(nonExistentId);

        // Assert
        assertEquals("No outbox event should be created for non-existent order", 0, db.getTotalEventCount());
    }

    @Test
    public void testFetchUndispatchedEvents() {
        // Arrange
        db.createOrder();
        db.createOrder();
        db.createOrder();

        // Act
        List<OutboxEvent> undispatched = db.fetchUndispatched(10);

        // Assert
        assertEquals("All 3 events should be undispatched", 3, undispatched.size());
        for (OutboxEvent event : undispatched) {
            assertFalse("Event should not be dispatched", event.dispatched);
        }
    }

    @Test
    public void testFetchUndispatchedWithLimit() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            db.createOrder();
        }

        // Act
        List<OutboxEvent> undispatched = db.fetchUndispatched(2);

        // Assert
        assertEquals("Should return only 2 events due to limit", 2, undispatched.size());
    }

    @Test
    public void testFetchUndispatchedExcludesDispatchedEvents() {
        // Arrange
        db.createOrder();
        db.createOrder();
        List<OutboxEvent> firstBatch = db.fetchUndispatched(1);
        db.markDispatched(firstBatch);

        // Act
        List<OutboxEvent> undispatched = db.fetchUndispatched(10);

        // Assert
        assertEquals("Only 1 undispatched event should remain", 1, undispatched.size());
        assertNotEquals("Returned event should not be the dispatched one", 
            firstBatch.get(0).id, undispatched.get(0).id);
    }

    @Test
    public void testMarkDispatched() {
        // Arrange
        db.createOrder();
        db.createOrder();
        List<OutboxEvent> events = db.fetchUndispatched(10);

        // Act
        db.markDispatched(events);

        // Assert
        for (OutboxEvent event : events) {
            assertTrue("Event should be marked as dispatched", event.dispatched);
        }
        assertEquals("All events should be dispatched", 2, db.getDispatchedEventCount());
    }

    @Test
    public void testMarkDispatchedEmptyCollection() {
        // Arrange
        db.createOrder();
        List<OutboxEvent> emptyList = new ArrayList<>();

        // Act
        db.markDispatched(emptyList);

        // Assert
        assertEquals("No events should be dispatched", 0, db.getDispatchedEventCount());
    }

    @Test
    public void testAtomicOrderCreationAndEventPersistence() {
        // Arrange & Act
        Order order = db.createOrder();

        // Assert - Both order and event should exist
        assertTrue("Order should exist in database", db.orders.containsKey(order.id));
        assertEquals("Exactly one event should exist", 1, db.getTotalEventCount());
        assertEquals("Event should reference the created order", 
            order.id, db.outbox.get(0).aggregateId);
    }

    @Test
    public void testMultipleOrderLifecycle() {
        // Arrange & Act
        Order order1 = db.createOrder();
        Order order2 = db.createOrder();
        db.markPaid(order1.id);
        db.markPaid(order2.id);

        // Assert
        assertEquals("Order1 should be PAID", "PAID", order1.status);
        assertEquals("Order2 should be PAID", "PAID", order2.status);
        assertEquals("Four events should exist", 4, db.getTotalEventCount());

        List<OutboxEvent> events = db.outbox;
        assertEquals("First event should be OrderCreated", "OrderCreated", events.get(0).type);
        assertEquals("Second event should be OrderCreated", "OrderCreated", events.get(1).type);
        assertEquals("Third event should be OrderPaid", "OrderPaid", events.get(2).type);
        assertEquals("Fourth event should be OrderPaid", "OrderPaid", events.get(3).type);
    }

    @Test
    public void testConcurrentOrderCreation() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                db.createOrder();
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertEquals("All orders should be created", threadCount, db.orders.size());
        assertEquals("All events should be created", threadCount, db.getTotalEventCount());
    }

    @Test
    public void testEventIdUniqueness() {
        // Arrange & Act
        db.createOrder();
        db.createOrder();
        db.createOrder();

        // Assert
        Set<String> eventIds = new HashSet<>();
        for (OutboxEvent event : db.outbox) {
            assertTrue("Event ID should be unique", eventIds.add(event.id));
        }
        assertEquals("All event IDs should be unique", 3, eventIds.size());
    }

    @Test
    public void testBatchProcessing() {
        // Arrange
        for (int i = 0; i < 10; i++) {
            db.createOrder();
        }

        // Act - Process in batches of 3
        List<OutboxEvent> batch1 = db.fetchUndispatched(3);
        db.markDispatched(batch1);
        List<OutboxEvent> batch2 = db.fetchUndispatched(3);
        db.markDispatched(batch2);
        List<OutboxEvent> batch3 = db.fetchUndispatched(3);
        db.markDispatched(batch3);
        List<OutboxEvent> batch4 = db.fetchUndispatched(3);
        db.markDispatched(batch4);

        // Assert
        assertEquals("First batch should have 3 events", 3, batch1.size());
        assertEquals("Second batch should have 3 events", 3, batch2.size());
        assertEquals("Third batch should have 3 events", 3, batch3.size());
        assertEquals("Fourth batch should have 1 event", 1, batch4.size());
        assertEquals("All events should be dispatched", 10, db.getDispatchedEventCount());
    }

    @Test
    public void testEventPayloadFormat() {
        // Arrange & Act
        Order order = db.createOrder();

        // Assert
        OutboxEvent event = db.outbox.get(0);
        assertTrue("Payload should be in expected format", 
            event.payload.matches("\\{id:[a-f0-9\\-]+\\}"));
    }
}
