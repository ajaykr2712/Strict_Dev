// Mediator Design Pattern Example
import java.util.*;

public class MediatorExample {
    interface ChatMediator { void broadcast(String from, String message); void register(User user); }

    static class ChatRoom implements ChatMediator {
        private final Map<String, User> users = new HashMap<>();
        @Override public void broadcast(String from, String message) {
            users.values().forEach(u -> { if (!u.getName().equals(from)) u.receive(from, message); });
        }
        @Override public void register(User user) { users.put(user.getName(), user); }
    }

    static class User {
        private final String name; private final ChatMediator mediator;
        User(String name, ChatMediator mediator) { this.name = name; this.mediator = mediator; mediator.register(this); }
        String getName() { return name; }
        void send(String message) { System.out.println(name + " sends: " + message); mediator.broadcast(name, message); }
        void receive(String from, String message) { System.out.println(name + " receives from " + from + ": " + message); }
    }

    public static void main(String[] args) {
        ChatMediator room = new ChatRoom();
        User alice = new User("Alice", room);
        User bob = new User("Bob", room);
        User eve = new User("Eve", room);
        alice.send("Hi all!");
        bob.send("Hello Alice!");
        eve.send("Listening...");
    }
}
