public class CQRSExample {
    interface Event {}
    static class AccountCreated implements Event { final String id; final String owner; AccountCreated(String id, String owner){ this.id=id; this.owner=owner; } }
    static class MoneyDeposited implements Event { final String id; final int amount; MoneyDeposited(String id, int amount){ this.id=id; this.amount=amount; } }

    interface EventHandler<E extends Event> { void on(E event); }
    static class EventBus {
        private final java.util.List<EventHandler<? super Event>> handlers = new java.util.ArrayList<>();
        void subscribe(EventHandler<? super Event> h){ handlers.add(h); }
        void publish(Event e){ for (EventHandler<? super Event> h : handlers) h.on(e); }
    }

    static class CommandService {
        private final EventBus bus;
        CommandService(EventBus bus){ this.bus = bus; }
        void createAccount(String id, String owner){ bus.publish(new AccountCreated(id, owner)); }
        void deposit(String id, int amount){ if (amount <= 0) throw new IllegalArgumentException("amount>0"); bus.publish(new MoneyDeposited(id, amount)); }
    }

    static class ReadModel implements EventHandler<Event> {
        static class Account { String id; String owner; int balance; }
        private final java.util.Map<String, Account> accounts = new java.util.concurrent.ConcurrentHashMap<>();
        public void on(Event e){
            if (e instanceof AccountCreated ac){
                Account a = new Account(); a.id = ac.id; a.owner = ac.owner; accounts.put(a.id, a);
            } else if (e instanceof MoneyDeposited md){
                Account a = accounts.get(md.id); if (a != null) a.balance += md.amount;
            }
        }
        String getSummary(String id){ Account a = accounts.get(id); return a == null ? "not found" : a.id + " owner=" + a.owner + " balance=" + a.balance; }
    }

    public static void main(String[] args){
        EventBus bus = new EventBus();
        ReadModel read = new ReadModel();
        bus.subscribe(read);
        CommandService cmd = new CommandService(bus);
        cmd.createAccount("A1", "Carol");
        cmd.deposit("A1", 50);
        cmd.deposit("A1", 25);
        System.out.println(read.getSummary("A1"));
    }
}
