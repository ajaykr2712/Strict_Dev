import java.util.*;

public class ChoreographySagaExample {
    interface Event {}
    static class OrderCreated implements Event { final String id; OrderCreated(String id){ this.id=id; } }
    static class PaymentCompleted implements Event { final String id; PaymentCompleted(String id){ this.id=id; } }
    static class InventoryReserved implements Event { final String id; InventoryReserved(String id){ this.id=id; } }
    static class OrderShipped implements Event { final String id; OrderShipped(String id){ this.id=id; } }

    static class EventBus { private final Map<Class<?>, List<java.util.function.Consumer<Event>>> subs = new HashMap<>(); <E extends Event> void on(Class<E> t, java.util.function.Consumer<E> c){ subs.computeIfAbsent(t,k->new ArrayList<>()).add((java.util.function.Consumer<Event>) c); } void emit(Event e){ subs.getOrDefault(e.getClass(), List.of()).forEach(c -> c.accept(e)); } }

    public static void main(String[] args){
        EventBus bus = new EventBus();
        bus.on(OrderCreated.class, e -> { System.out.println("PaymentSvc: charge order " + e.id); bus.emit(new PaymentCompleted(e.id)); });
        bus.on(PaymentCompleted.class, e -> { System.out.println("InventorySvc: reserve items for " + e.id); bus.emit(new InventoryReserved(e.id)); });
        bus.on(InventoryReserved.class, e -> { System.out.println("ShippingSvc: ship order " + e.id); bus.emit(new OrderShipped(e.id)); });
        bus.on(OrderShipped.class, e -> System.out.println("Order workflow completed for " + e.id));
        bus.emit(new OrderCreated("O-1"));
    }
}
