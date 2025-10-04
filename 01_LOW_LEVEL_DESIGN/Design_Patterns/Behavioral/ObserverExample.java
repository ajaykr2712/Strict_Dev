import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Observer Pattern Implementation for Netflix-like Streaming Platform
 * 
 * Real-world Use Case: Content recommendation and user notification system
 * - When users interact with content (watch, rate, add to list), multiple components need to be notified
 * - Recommendation engine updates user preferences
 * - Analytics service tracks user behavior
 * - Notification service sends relevant alerts
 * - Marketing service updates campaign targeting
 * 
 * This demonstrates loose coupling between the subject (user actions) and observers (various services)
 */

// Subject interface - represents what can be observed
interface UserActivitySubject {
    void addObserver(UserActivityObserver observer);
    void removeObserver(UserActivityObserver observer);
    void notifyObservers(UserActivity activity);
}

// Observer interface - represents what can observe
interface UserActivityObserver {
    void onUserActivity(UserActivity activity);
    String getObserverName();
}

// Data class representing user activity
class UserActivity {
    private final String userId;
    private final String activityType;
    private final String contentId;
    private final String contentType;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;

    public UserActivity(String userId, String activityType, String contentId, 
                       String contentType, Map<String, Object> metadata) {
        this.userId = userId;
        this.activityType = activityType;
        this.contentId = contentId;
        this.contentType = contentType;
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>(metadata);
    }

    // Getters
    public String getUserId() { return userId; }
    public String getActivityType() { return activityType; }
    public String getContentId() { return contentId; }
    public String getContentType() { return contentType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }

    @Override
    public String toString() {
        return String.format("UserActivity{userId='%s', activityType='%s', contentId='%s', contentType='%s', timestamp=%s}",
                userId, activityType, contentId, contentType, 
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}

// Concrete Subject - Netflix User Activity Tracker
class NetflixUserActivityTracker implements UserActivitySubject {
    private final List<UserActivityObserver> observers;
    private final String platformName;

    public NetflixUserActivityTracker(String platformName) {
        this.observers = new CopyOnWriteArrayList<>(); // Thread-safe for concurrent access
        this.platformName = platformName;
    }

    @Override
    public void addObserver(UserActivityObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            System.out.println("[TRACKER] Added observer: " + observer.getObserverName());
        }
    }

    @Override
    public void removeObserver(UserActivityObserver observer) {
        if (observers.remove(observer)) {
            System.out.println("[TRACKER] Removed observer: " + observer.getObserverName());
        }
    }

    @Override
    public void notifyObservers(UserActivity activity) {
        System.out.println("[TRACKER] Broadcasting activity: " + activity);
        for (UserActivityObserver observer : observers) {
            try {
                observer.onUserActivity(activity);
            } catch (Exception e) {
                System.err.println("[TRACKER] Error notifying observer " + 
                                 observer.getObserverName() + ": " + e.getMessage());
            }
        }
    }

    // Business methods that trigger notifications
    public void userWatchedContent(String userId, String contentId, String contentType, int watchTimeMinutes) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("watchTimeMinutes", watchTimeMinutes);
        metadata.put("platform", platformName);
        
        UserActivity activity = new UserActivity(userId, "WATCHED", contentId, contentType, metadata);
        notifyObservers(activity);
    }

    public void userRatedContent(String userId, String contentId, double rating) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rating", rating);
        metadata.put("platform", platformName);
        
        UserActivity activity = new UserActivity(userId, "RATED", contentId, "UNKNOWN", metadata);
        notifyObservers(activity);
    }

    public void userAddedToWatchlist(String userId, String contentId, String contentType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("platform", platformName);
        
        UserActivity activity = new UserActivity(userId, "ADDED_TO_WATCHLIST", contentId, contentType, metadata);
        notifyObservers(activity);
    }
}

// Concrete Observer - Recommendation Engine
class RecommendationEngine implements UserActivityObserver {
    private final Map<String, Map<String, Double>> userPreferences;
    private final String engineVersion;

    public RecommendationEngine(String engineVersion) {
        this.userPreferences = new ConcurrentHashMap<>();
        this.engineVersion = engineVersion;
    }

    @Override
    public void onUserActivity(UserActivity activity) {
        System.out.println("[RECOMMENDATION-ENGINE] Processing: " + activity.getActivityType() + 
                         " for user " + activity.getUserId());

        updateUserPreferences(activity);
        
        // Simulate recommendation calculation
        if ("WATCHED".equals(activity.getActivityType())) {
            generateRecommendations(activity.getUserId());
        }
    }

    private void updateUserPreferences(UserActivity activity) {
        String userId = activity.getUserId();
        String contentType = activity.getContentType();
        
        userPreferences.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        Map<String, Double> preferences = userPreferences.get(userId);
        
        // Update preference score based on activity type
        double currentScore = preferences.getOrDefault(contentType, 0.0);
        switch (activity.getActivityType()) {
            case "WATCHED":
                Integer watchTime = (Integer) activity.getMetadata().get("watchTimeMinutes");
                if (watchTime != null && watchTime > 30) { // Significant watch time
                    preferences.put(contentType, currentScore + 2.0);
                }
                break;
            case "RATED":
                Double rating = (Double) activity.getMetadata().get("rating");
                if (rating != null && rating >= 4.0) {
                    preferences.put(contentType, currentScore + 1.5);
                }
                break;
            case "ADDED_TO_WATCHLIST":
                preferences.put(contentType, currentScore + 0.5);
                break;
        }
    }

    private void generateRecommendations(String userId) {
        Map<String, Double> preferences = userPreferences.get(userId);
        if (preferences != null && !preferences.isEmpty()) {
            String topPreference = preferences.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("GENERAL");
            
            System.out.println("[RECOMMENDATION-ENGINE] Generated recommendations for user " + 
                             userId + " based on top preference: " + topPreference);
        }
    }

    @Override
    public String getObserverName() {
        return "RecommendationEngine-" + engineVersion;
    }
}

// Concrete Observer - Analytics Service
class AnalyticsService implements UserActivityObserver {
    private final Map<String, Integer> activityCounts;
    private final Map<String, Set<String>> uniqueUsers;

    public AnalyticsService() {
        this.activityCounts = new ConcurrentHashMap<>();
        this.uniqueUsers = new ConcurrentHashMap<>();
    }

    @Override
    public void onUserActivity(UserActivity activity) {
        System.out.println("[ANALYTICS] Recording activity: " + activity.getActivityType());
        
        // Track activity counts
        activityCounts.merge(activity.getActivityType(), 1, Integer::sum);
        
        // Track unique users per activity type
        uniqueUsers.computeIfAbsent(activity.getActivityType(), k -> ConcurrentHashMap.newKeySet())
                  .add(activity.getUserId());
        
        // Generate real-time insights
        generateInsights(activity);
    }

    private void generateInsights(UserActivity activity) {
        String activityType = activity.getActivityType();
        int totalCount = activityCounts.get(activityType);
        int uniqueUserCount = uniqueUsers.get(activityType).size();
        
        if (totalCount % 10 == 0) { // Report every 10 activities
            System.out.println("[ANALYTICS] Insight - " + activityType + 
                             ": Total=" + totalCount + ", Unique Users=" + uniqueUserCount);
        }
    }

    public void printAnalyticsSummary() {
        System.out.println("\n[ANALYTICS] Summary Report:");
        activityCounts.forEach((activity, count) -> {
            int uniqueCount = uniqueUsers.get(activity).size();
            System.out.println("  " + activity + ": " + count + " activities from " + uniqueCount + " unique users");
        });
    }

    @Override
    public String getObserverName() {
        return "AnalyticsService";
    }
}

// Concrete Observer - Notification Service
class NotificationService implements UserActivityObserver {
    private final Map<String, List<String>> userNotifications;

    public NotificationService() {
        this.userNotifications = new ConcurrentHashMap<>();
    }

    @Override
    public void onUserActivity(UserActivity activity) {
        String userId = activity.getUserId();
        String notification = generateNotification(activity);
        
        if (notification != null) {
            userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);
            System.out.println("[NOTIFICATION] Sent to user " + userId + ": " + notification);
        }
    }

    private String generateNotification(UserActivity activity) {
        switch (activity.getActivityType()) {
            case "WATCHED":
                Integer watchTime = (Integer) activity.getMetadata().get("watchTimeMinutes");
                if (watchTime != null && watchTime > 120) { // Binge watching
                    return "Great binge session! Check out similar " + activity.getContentType() + " content.";
                }
                break;
            case "RATED":
                Double rating = (Double) activity.getMetadata().get("rating");
                if (rating != null && rating >= 5.0) {
                    return "Thanks for the 5-star rating! Here are more titles you might love.";
                }
                break;
            case "ADDED_TO_WATCHLIST":
                return "Added to watchlist! Don't forget to watch " + activity.getContentId() + " this weekend.";
        }
        return null;
    }

    @Override
    public String getObserverName() {
        return "NotificationService";
    }
}

// Demonstration class
class ObserverPatternDemo {
    public static void main(String[] args) {
        System.out.println("=== Observer Pattern Demo: Netflix User Activity Tracking ===\n");

        // Create the subject (what will be observed)
        NetflixUserActivityTracker activityTracker = new NetflixUserActivityTracker("Netflix");

        // Create observers (what will react to changes)
        RecommendationEngine recommendationEngine = new RecommendationEngine("v2.1");
        AnalyticsService analyticsService = new AnalyticsService();
        NotificationService notificationService = new NotificationService();

        // Register observers
        System.out.println("1. Registering observers:");
        activityTracker.addObserver(recommendationEngine);
        activityTracker.addObserver(analyticsService);
        activityTracker.addObserver(notificationService);

        System.out.println("\n2. Simulating user activities:");
        
        // Simulate various user activities
        activityTracker.userWatchedContent("user123", "stranger-things-s4", "TV_SERIES", 75);
        System.out.println();
        
        activityTracker.userRatedContent("user123", "stranger-things-s4", 5.0);
        System.out.println();
        
        activityTracker.userAddedToWatchlist("user456", "the-crown", "TV_SERIES");
        System.out.println();
        
        activityTracker.userWatchedContent("user789", "top-gun-maverick", "MOVIE", 130);
        System.out.println();
        
        activityTracker.userWatchedContent("user456", "squid-game", "TV_SERIES", 45);
        System.out.println();

        // Demonstrate observer removal
        System.out.println("3. Removing notification service:");
        activityTracker.removeObserver(notificationService);
        
        activityTracker.userRatedContent("user789", "top-gun-maverick", 4.5);
        System.out.println();

        // Show analytics summary
        analyticsService.printAnalyticsSummary();

        System.out.println("\n=== Observer Pattern Benefits Demonstrated ===");
        System.out.println("✓ Loose coupling: Activity tracker doesn't know about specific observers");
        System.out.println("✓ Dynamic relationships: Observers can be added/removed at runtime");
        System.out.println("✓ Broadcast communication: One activity notifies multiple observers");
        System.out.println("✓ Open/Closed principle: New observers can be added without modifying existing code");
    }
}
