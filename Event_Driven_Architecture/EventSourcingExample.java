import java.util.*;
import java.util.concurrent.*;

/**
 * Event Sourcing Implementation Example
 * 
 * Real-world Use Case: Netflix User Activity Event Store
 * Stores all user events and can rebuild current state from events
 */

public class EventSourcingExample {
    
    // Base event interface
    interface Event {
        String getEventId();
        String getAggregateId();
        String getEventType();
        long getTimestamp();
        Map<String, Object> getEventData();
    }
    
    // Abstract base event
    abstract static class BaseEvent implements Event {
        protected final String eventId;
        protected final String aggregateId;
        protected final String eventType;
        protected final long timestamp;
        protected final Map<String, Object> eventData;
        
        public BaseEvent(String aggregateId, String eventType, Map<String, Object> eventData) {
            this.eventId = UUID.randomUUID().toString();
            this.aggregateId = aggregateId;
            this.eventType = eventType;
            this.timestamp = System.currentTimeMillis();
            this.eventData = new HashMap<>(eventData);
        }
        
        @Override
        public String getEventId() { return eventId; }
        
        @Override
        public String getAggregateId() { return aggregateId; }
        
        @Override
        public String getEventType() { return eventType; }
        
        @Override
        public long getTimestamp() { return timestamp; }
        
        @Override
        public Map<String, Object> getEventData() { return new HashMap<>(eventData); }
        
        @Override
        public String toString() {
            return String.format("%s{id='%s', aggregate='%s', timestamp=%d}", 
                    eventType, eventId, aggregateId, timestamp);
        }
    }
    
    // Specific event types
    static class UserRegisteredEvent extends BaseEvent {
        public UserRegisteredEvent(String userId, String email, String subscriptionTier) {
            super(userId, "USER_REGISTERED", createEventData(email, subscriptionTier));
        }
        
        private static Map<String, Object> createEventData(String email, String subscriptionTier) {
            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            data.put("subscriptionTier", subscriptionTier);
            return data;
        }
    }
    
    static class ContentWatchedEvent extends BaseEvent {
        public ContentWatchedEvent(String userId, String contentId, int watchDurationMinutes) {
            super(userId, "CONTENT_WATCHED", createEventData(contentId, watchDurationMinutes));
        }
        
        private static Map<String, Object> createEventData(String contentId, int watchDurationMinutes) {
            Map<String, Object> data = new HashMap<>();
            data.put("contentId", contentId);
            data.put("watchDurationMinutes", watchDurationMinutes);
            return data;
        }
    }
    
    static class SubscriptionUpgradedEvent extends BaseEvent {
        public SubscriptionUpgradedEvent(String userId, String oldTier, String newTier) {
            super(userId, "SUBSCRIPTION_UPGRADED", createEventData(oldTier, newTier));
        }
        
        private static Map<String, Object> createEventData(String oldTier, String newTier) {
            Map<String, Object> data = new HashMap<>();
            data.put("oldTier", oldTier);
            data.put("newTier", newTier);
            return data;
        }
    }
    
    static class ContentRatedEvent extends BaseEvent {
        public ContentRatedEvent(String userId, String contentId, double rating) {
            super(userId, "CONTENT_RATED", createEventData(contentId, rating));
        }
        
        private static Map<String, Object> createEventData(String contentId, double rating) {
            Map<String, Object> data = new HashMap<>();
            data.put("contentId", contentId);
            data.put("rating", rating);
            return data;
        }
    }
    
    // Event Store interface
    interface EventStore {
        void saveEvent(Event event);
        List<Event> getEvents(String aggregateId);
        List<Event> getEvents(String aggregateId, long fromTimestamp);
        List<Event> getAllEvents();
    }
    
    // In-memory event store implementation
    static class InMemoryEventStore implements EventStore {
        private final Map<String, List<Event>> eventsByAggregate;
        private final List<Event> allEvents;
        
        public InMemoryEventStore() {
            this.eventsByAggregate = new ConcurrentHashMap<>();
            this.allEvents = new CopyOnWriteArrayList<>();
        }
        
        @Override
        public void saveEvent(Event event) {
            allEvents.add(event);
            eventsByAggregate.computeIfAbsent(event.getAggregateId(), k -> new ArrayList<>())
                           .add(event);
            System.out.println("[EVENT-STORE] Saved: " + event);
        }
        
        @Override
        public List<Event> getEvents(String aggregateId) {
            return new ArrayList<>(eventsByAggregate.getOrDefault(aggregateId, new ArrayList<>()));
        }
        
        @Override
        public List<Event> getEvents(String aggregateId, long fromTimestamp) {
            return eventsByAggregate.getOrDefault(aggregateId, new ArrayList<>())
                    .stream()
                    .filter(event -> event.getTimestamp() >= fromTimestamp)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        @Override
        public List<Event> getAllEvents() {
            return new ArrayList<>(allEvents);
        }
    }
    
    // User aggregate (current state)
    static class UserAggregate {
        private String userId;
        private String email;
        private String subscriptionTier;
        private int totalWatchTimeMinutes;
        private List<String> watchedContent;
        private Map<String, Double> contentRatings;
        private long version; // For optimistic locking
        
        public UserAggregate(String userId) {
            this.userId = userId;
            this.watchedContent = new ArrayList<>();
            this.contentRatings = new HashMap<>();
            this.version = 0;
        }
        
        // Apply events to rebuild state
        public void apply(Event event) {
            switch (event.getEventType()) {
                case "USER_REGISTERED":
                    applyUserRegistered(event);
                    break;
                case "CONTENT_WATCHED":
                    applyContentWatched(event);
                    break;
                case "SUBSCRIPTION_UPGRADED":
                    applySubscriptionUpgraded(event);
                    break;
                case "CONTENT_RATED":
                    applyContentRated(event);
                    break;
                default:
                    System.out.println("Unknown event type: " + event.getEventType());
            }
            version++;
        }
        
        private void applyUserRegistered(Event event) {
            Map<String, Object> data = event.getEventData();
            this.email = (String) data.get("email");
            this.subscriptionTier = (String) data.get("subscriptionTier");
        }
        
        private void applyContentWatched(Event event) {
            Map<String, Object> data = event.getEventData();
            String contentId = (String) data.get("contentId");
            int watchDuration = (Integer) data.get("watchDurationMinutes");
            
            if (!watchedContent.contains(contentId)) {
                watchedContent.add(contentId);
            }
            totalWatchTimeMinutes += watchDuration;
        }
        
        private void applySubscriptionUpgraded(Event event) {
            Map<String, Object> data = event.getEventData();
            this.subscriptionTier = (String) data.get("newTier");
        }
        
        private void applyContentRated(Event event) {
            Map<String, Object> data = event.getEventData();
            String contentId = (String) data.get("contentId");
            double rating = (Double) data.get("rating");
            contentRatings.put(contentId, rating);
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getSubscriptionTier() { return subscriptionTier; }
        public int getTotalWatchTimeMinutes() { return totalWatchTimeMinutes; }
        public List<String> getWatchedContent() { return new ArrayList<>(watchedContent); }
        public Map<String, Double> getContentRatings() { return new HashMap<>(contentRatings); }
        public long getVersion() { return version; }
        
        @Override
        public String toString() {
            return String.format("User{id='%s', email='%s', tier='%s', watchTime=%d, content=%d, ratings=%d, version=%d}",
                    userId, email, subscriptionTier, totalWatchTimeMinutes, 
                    watchedContent.size(), contentRatings.size(), version);
        }
    }
    
    // Repository for rebuilding aggregates from events
    static class UserRepository {
        private final EventStore eventStore;
        
        public UserRepository(EventStore eventStore) {
            this.eventStore = eventStore;
        }
        
        public UserAggregate getUser(String userId) {
            UserAggregate user = new UserAggregate(userId);
            List<Event> events = eventStore.getEvents(userId);
            
            for (Event event : events) {
                user.apply(event);
            }
            
            return user;
        }
        
        public UserAggregate getUserAtPointInTime(String userId, long timestamp) {
            UserAggregate user = new UserAggregate(userId);
            List<Event> events = eventStore.getEvents(userId);
            
            for (Event event : events) {
                if (event.getTimestamp() <= timestamp) {
                    user.apply(event);
                } else {
                    break;
                }
            }
            
            return user;
        }
        
        public void saveEvent(Event event) {
            eventStore.saveEvent(event);
        }
    }
    
    // Command handlers
    static class UserCommandHandler {
        private final UserRepository userRepository;
        
        public UserCommandHandler(UserRepository userRepository) {
            this.userRepository = userRepository;
        }
        
        public void registerUser(String userId, String email, String subscriptionTier) {
            UserRegisteredEvent event = new UserRegisteredEvent(userId, email, subscriptionTier);
            userRepository.saveEvent(event);
        }
        
        public void recordContentWatched(String userId, String contentId, int watchDurationMinutes) {
            ContentWatchedEvent event = new ContentWatchedEvent(userId, contentId, watchDurationMinutes);
            userRepository.saveEvent(event);
        }
        
        public void upgradeSubscription(String userId, String oldTier, String newTier) {
            SubscriptionUpgradedEvent event = new SubscriptionUpgradedEvent(userId, oldTier, newTier);
            userRepository.saveEvent(event);
        }
        
        public void rateContent(String userId, String contentId, double rating) {
            ContentRatedEvent event = new ContentRatedEvent(userId, contentId, rating);
            userRepository.saveEvent(event);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Event Sourcing Demo: Netflix User Activity ===\n");
        
        // Set up event sourcing infrastructure
        EventStore eventStore = new InMemoryEventStore();
        UserRepository userRepository = new UserRepository(eventStore);
        UserCommandHandler commandHandler = new UserCommandHandler(userRepository);
        
        String userId = "user123";
        
        // Simulate user activity over time
        System.out.println("1. User Registration:");
        commandHandler.registerUser(userId, "john@example.com", "BASIC");
        
        Thread.sleep(100); // Simulate time passing
        
        System.out.println("\n2. User watches content:");
        commandHandler.recordContentWatched(userId, "stranger-things-s4", 45);
        commandHandler.recordContentWatched(userId, "the-crown-s5", 60);
        
        Thread.sleep(100);
        
        System.out.println("\n3. User rates content:");
        commandHandler.rateContent(userId, "stranger-things-s4", 4.5);
        commandHandler.rateContent(userId, "the-crown-s5", 4.0);
        
        Thread.sleep(100);
        
        System.out.println("\n4. User upgrades subscription:");
        commandHandler.upgradeSubscription(userId, "BASIC", "PREMIUM");
        
        Thread.sleep(100);
        
        System.out.println("\n5. More content watching:");
        commandHandler.recordContentWatched(userId, "squid-game", 120);
        commandHandler.recordContentWatched(userId, "stranger-things-s4", 30); // Re-watching
        
        // Rebuild current state from events
        System.out.println("\n=== Current User State (Rebuilt from Events) ===");
        UserAggregate currentUser = userRepository.getUser(userId);
        System.out.println(currentUser);
        System.out.println("Watched Content: " + currentUser.getWatchedContent());
        System.out.println("Content Ratings: " + currentUser.getContentRatings());
        
        // Time travel - get user state at a specific point in time
        System.out.println("\n=== Time Travel: User State Before Subscription Upgrade ===");
        long beforeUpgrade = System.currentTimeMillis() - 200; // Approximate timestamp before upgrade
        UserAggregate userBeforeUpgrade = userRepository.getUserAtPointInTime(userId, beforeUpgrade);
        System.out.println(userBeforeUpgrade);
        
        // Show all events
        System.out.println("\n=== All Events in Store ===");
        List<Event> allEvents = eventStore.getAllEvents();
        allEvents.forEach(System.out::println);
        
        System.out.println("\n=== Event Sourcing Benefits ===");
        System.out.println("✓ Complete audit trail of all changes");
        System.out.println("✓ Time travel - reconstruct state at any point");
        System.out.println("✓ Event replay for debugging and testing");
        System.out.println("✓ Flexible projections and read models");
        System.out.println("✓ Natural fit for event-driven architectures");
    }
}
