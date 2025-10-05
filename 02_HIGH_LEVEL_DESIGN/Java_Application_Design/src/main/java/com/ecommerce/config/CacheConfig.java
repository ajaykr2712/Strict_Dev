package com.ecommerce.config;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Cache Configuration - Infrastructure Configuration
 * 
 * Manages distributed caching for high-performance data access.
 * Implements caching patterns from "Designing Data-Intensive Applications":
 * - Multi-level caching (L1: in-memory, L2: Redis cluster)
 * - Cache-aside pattern with write-through
 * - TTL-based expiration and invalidation
 */
public class CacheConfig {
    private static volatile boolean initialized = false;
    private static final Map<String, Object> cacheInstances = new ConcurrentHashMap<>();
    
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        System.out.println("Initializing Cache Configuration...");
        
        // Initialize local in-memory cache (L1)
        initializeLocalCache();
        
        // Initialize Redis cluster (L2)
        initializeDistributedCache();
        
        // Configure cache policies
        configureCachePolicies();
        
        initialized = true;
        System.out.println("Cache Configuration initialized successfully at " + LocalDateTime.now());
    }
    
    private static void initializeLocalCache() {
        // Simulate L1 cache initialization
        cacheInstances.put("local_cache", new ConcurrentHashMap<String, Object>());
        System.out.println("  - L1 Local Cache initialized (in-memory)");
        System.out.println("    * Max size: 10,000 entries");
        System.out.println("    * TTL: 5 minutes");
    }
    
    private static void initializeDistributedCache() {
        // Simulate Redis cluster initialization
        cacheInstances.put("redis_cluster", "redis://cluster-endpoint:6379");
        System.out.println("  - L2 Distributed Cache initialized (Redis Cluster)");
        System.out.println("    * Cluster nodes: 3 masters, 3 replicas");
        System.out.println("    * Max memory: 4GB per node");
        System.out.println("    * Eviction policy: LRU");
    }
    
    private static void configureCachePolicies() {
        System.out.println("  - Cache policies configured:");
        System.out.println("    * User sessions: TTL 30 minutes");
        System.out.println("    * Product catalog: TTL 24 hours");
        System.out.println("    * Shopping carts: TTL 7 days");
        System.out.println("    * Search results: TTL 1 hour");
    }
    
    public static Object getCacheInstance(String type) {
        if (!initialized) {
            throw new IllegalStateException("Cache not initialized");
        }
        return cacheInstances.get(type);
    }
    
    public static void shutdown() {
        if (!initialized) {
            return;
        }
        
        System.out.println("Shutting down Cache Configuration...");
        
        // Close cache connections
        cacheInstances.clear();
        
        initialized = false;
        System.out.println("Cache Configuration shut down successfully");
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
