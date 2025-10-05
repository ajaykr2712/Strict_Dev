package com.ecommerce.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * Database Configuration - Infrastructure Configuration
 * 
 * Manages database connections and configuration for data-intensive operations.
 * Implements patterns from "Designing Data-Intensive Applications":
 * - Connection pooling for scalability
 * - Read/write splitting for performance
 * - Sharding support for horizontal scaling
 */
public class DatabaseConfig {
    private static volatile boolean initialized = false;
    private static final Map<String, String> connectionPool = new ConcurrentHashMap<>();
    
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        System.out.println("Initializing Database Configuration...");
        
        // Simulate database connection pool initialization
        initializeConnectionPool();
        
        // Initialize read/write database configurations
        initializeReadWriteSplit();
        
        // Initialize sharding configuration
        initializeSharding();
        
        initialized = true;
        System.out.println("Database Configuration initialized successfully at " + LocalDateTime.now());
    }
    
    private static void initializeConnectionPool() {
        // Simulate connection pool setup
        connectionPool.put("primary", "jdbc:postgresql://localhost:5432/ecommerce_primary");
        connectionPool.put("read_replica", "jdbc:postgresql://localhost:5433/ecommerce_replica");
        connectionPool.put("cache_db", "redis://localhost:6379");
        
        System.out.println("  - Connection pool initialized with " + connectionPool.size() + " connections");
    }
    
    private static void initializeReadWriteSplit() {
        // Configure read/write splitting for better performance
        System.out.println("  - Read/write splitting configured");
        System.out.println("    * Write operations -> Primary DB");
        System.out.println("    * Read operations -> Read Replicas");
    }
    
    private static void initializeSharding() {
        // Configure database sharding for horizontal scaling
        System.out.println("  - Database sharding configured");
        System.out.println("    * User data -> Shard by user_id hash");
        System.out.println("    * Product data -> Shard by category");
        System.out.println("    * Order data -> Shard by date range");
    }
    
    public static String getConnectionString(String type) {
        if (!initialized) {
            throw new IllegalStateException("Database not initialized");
        }
        return connectionPool.get(type);
    }
    
    public static void shutdown() {
        if (!initialized) {
            return;
        }
        
        System.out.println("Shutting down Database Configuration...");
        
        // Close all database connections
        connectionPool.clear();
        
        initialized = false;
        System.out.println("Database Configuration shut down successfully");
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
