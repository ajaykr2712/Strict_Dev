/**
 * Singleton Pattern Implementation
 * 
 * Use Case: Netflix Event Store Manager
 * 
 * Netflix needs a centralized event store to manage all streaming events,
 * user interactions, and system events. This ensures consistency and
 * prevents multiple instances from causing data inconsistencies.
 * 
 * Real-world scenario: Managing video streaming events, user preferences,
 * and recommendation data in a single, globally accessible instance.
 * 
 * @author System Design Expert
 * @version 1.0
 */

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alternative Singleton implementation using double-checked locking
 * 
 * Use this approach when you need constructor parameters or more complex initialization
 * Note: This is more complex and generally the enum approach is preferred
 */
class NetflixConfigurationManager {
    private static volatile NetflixConfigurationManager instance;
    private final Map<String, String> configurations;
    private final String environment;
    
    /**
     * Private constructor to prevent direct instantiation
     */
    private NetflixConfigurationManager(String environment) {
        this.environment = environment;
        this.configurations = new ConcurrentHashMap<>();
        loadConfigurations();
    }
    
    /**
     * Thread-safe getInstance method using double-checked locking
     * 
     * @param environment The environment (e.g., "PROD", "DEV", "TEST")
     * @return Singleton instance
     */
    public static NetflixConfigurationManager getInstance(String environment) {
        if (instance == null) {
            synchronized (NetflixConfigurationManager.class) {
                if (instance == null) {
                    instance = new NetflixConfigurationManager(environment);
                }
            }
        }
        return instance;
    }
    
    /**
     * Get singleton instance with default environment
     */
    public static NetflixConfigurationManager getInstance() {
        return getInstance("PROD");
    }
    
    /**
     * Load configurations based on environment
     */
    private void loadConfigurations() {
        // Load different configurations based on environment
        switch (environment) {
            case "PROD":
                configurations.put("max_streaming_quality", "4K");
                configurations.put("cdn_servers", "1000");
                configurations.put("cache_ttl", "3600");
                break;
            case "DEV":
                configurations.put("max_streaming_quality", "1080p");
                configurations.put("cdn_servers", "10");
                configurations.put("cache_ttl", "300");
                break;
            case "TEST":
                configurations.put("max_streaming_quality", "720p");
                configurations.put("cdn_servers", "1");
                configurations.put("cache_ttl", "60");
                break;
            default:
                throw new IllegalArgumentException("Unknown environment: " + environment);
        }
    }
    
    /**
     * Get configuration value
     */
    public String getConfiguration(String key) {
        return configurations.get(key);
    }
    
    /**
     * Set configuration value
     */
    public void setConfiguration(String key, String value) {
        configurations.put(key, value);
    }
    
    public String getEnvironment() {
        return environment;
    }
}

/**
 * Demonstration class showing how to use the Singleton patterns
 */
class SingletonExample {
    public static void main(String[] args) {
        System.out.println("=== Netflix Singleton Pattern Demo ===\n");
        
        // Demo 1: Event Store Manager (Enum Singleton)
        demonstrateEventStore();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Demo 2: Configuration Manager (Double-checked locking)
        demonstrateConfigurationManager();
    }
    
    private static void demonstrateEventStore() {
        System.out.println("1. Netflix Event Store Manager Demo");
        System.out.println("-----------------------------------");
        
        // Get singleton instance
        NetflixEventStoreManager eventStore = NetflixEventStoreManager.INSTANCE;
        
        // Create sample streaming events
        Map<String, Object> videoStartData = new HashMap<>();
        videoStartData.put("videoId", "movie_12345");
        videoStartData.put("quality", "1080p");
        videoStartData.put("device", "smart_tv");
        
        Map<String, Object> videoPauseData = new HashMap<>();
        videoPauseData.put("videoId", "movie_12345");
        videoPauseData.put("watchTime", "00:45:30");
        
        Map<String, Object> recommendationData = new HashMap<>();
        recommendationData.put("recommendedVideoId", "series_67890");
        recommendationData.put("algorithm", "collaborative_filtering");
        
        // Store events
        eventStore.storeEvent(new StreamingEvent("evt_001", "user_123", "VIDEO_STARTED", videoStartData));
        eventStore.storeEvent(new StreamingEvent("evt_002", "user_123", "VIDEO_PAUSED", videoPauseData));
        eventStore.storeEvent(new StreamingEvent("evt_003", "user_456", "VIDEO_STARTED", videoStartData));
        eventStore.storeEvent(new StreamingEvent("evt_004", "user_123", "RECOMMENDATION_CLICKED", recommendationData));
        
        // Demonstrate singleton behavior - same instance
        NetflixEventStoreManager anotherReference = NetflixEventStoreManager.INSTANCE;
        System.out.println("Same instance check: " + (eventStore == anotherReference));
        
        // Query events
        System.out.println("\nTotal events: " + eventStore.getTotalEventCount());
        System.out.println("Events for user_123: " + eventStore.getUserEvents("user_123").size());
        System.out.println("VIDEO_STARTED events: " + eventStore.getEventsByType("VIDEO_STARTED").size());
        
        // Show event type counts
        System.out.println("\nEvent type counts:");
        eventStore.getEventTypeCounts().forEach((type, count) -> 
            System.out.println("  " + type + ": " + count));
    }
    
    private static void demonstrateConfigurationManager() {
        System.out.println("2. Netflix Configuration Manager Demo");
        System.out.println("------------------------------------");
        
        // Get singleton instances for different environments
        NetflixConfigurationManager prodConfig = NetflixConfigurationManager.getInstance("PROD");
        NetflixConfigurationManager devConfig = NetflixConfigurationManager.getInstance("DEV");
        
        // Note: Both will return the same instance (first one created)
        System.out.println("Same instance check: " + (prodConfig == devConfig));
        System.out.println("Environment: " + prodConfig.getEnvironment());
        
        // Show configurations
        System.out.println("\nProduction Configurations:");
        System.out.println("  Max Streaming Quality: " + prodConfig.getConfiguration("max_streaming_quality"));
        System.out.println("  CDN Servers: " + prodConfig.getConfiguration("cdn_servers"));
        System.out.println("  Cache TTL: " + prodConfig.getConfiguration("cache_ttl"));
        
        // Modify configuration
        prodConfig.setConfiguration("feature_flags", "new_ui_enabled");
        System.out.println("  Feature Flags: " + prodConfig.getConfiguration("feature_flags"));
        
        // Verify same instance has the updated configuration
        NetflixConfigurationManager anotherRef = NetflixConfigurationManager.getInstance();
        System.out.println("  Feature Flags from another reference: " + 
                         anotherRef.getConfiguration("feature_flags"));
    }
}
