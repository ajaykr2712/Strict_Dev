package com.ecommerce.service;

import com.ecommerce.domain.User;
import com.ecommerce.infrastructure.monitoring.MetricsCollector;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * User Service - Application Service Layer
 * 
 * Handles user management operations with:
 * - Caching for performance (simulated with in-memory cache)
 * - Metrics collection for monitoring
 * - Business logic orchestration
 */
public class UserService {
    private final MetricsCollector metricsCollector;
    private final Map<String, User> userCache;
    private final Map<String, User> userDatabase;
    
    public UserService(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.userCache = new ConcurrentHashMap<>();
        this.userDatabase = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        System.out.println("UserService initialized");
    }
    
    /**
     * Register a new user
     */
    public User registerUser(String email, String name) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Check if user already exists
            if (findUserByEmail(email) != null) {
                throw new IllegalArgumentException("User with email " + email + " already exists");
            }
            
            // Create new user
            User user = new User(email, name);
            
            // Save to database (simulated)
            userDatabase.put(user.getId(), user);
            
            // Update cache
            userCache.put(user.getId(), user);
            
            // Record metrics
            metricsCollector.recordMetric("user.registration.success", 1);
            metricsCollector.recordLatency("user.registration.latency", 
                                         System.currentTimeMillis() - startTime);
            
            System.out.println("User registered successfully: " + user.getEmail());
            return user;
            
        } catch (Exception e) {
            metricsCollector.recordMetric("user.registration.error", 1);
            throw e;
        }
    }
    
    /**
     * Find user by ID with caching
     */
    public User findUserById(String userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Try cache first
            User user = userCache.get(userId);
            if (user != null) {
                metricsCollector.recordMetric("user.cache.hit", 1);
                return user;
            }
            
            // Cache miss - load from database
            metricsCollector.recordMetric("user.cache.miss", 1);
            user = userDatabase.get(userId);
            
            if (user != null) {
                // Update cache
                userCache.put(userId, user);
            }
            
            metricsCollector.recordLatency("user.lookup.latency", 
                                         System.currentTimeMillis() - startTime);
            return user;
            
        } catch (Exception e) {
            metricsCollector.recordMetric("user.lookup.error", 1);
            throw e;
        }
    }
    
    /**
     * Find user by email
     */
    public User findUserByEmail(String email) {
        return userDatabase.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Update user login timestamp
     */
    public User updateLastLogin(String userId) {
        User user = findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        User updatedUser = user.withLastLogin(LocalDateTime.now());
        
        // Update database and cache
        userDatabase.put(userId, updatedUser);
        userCache.put(userId, updatedUser);
        
        metricsCollector.recordMetric("user.login.update", 1);
        return updatedUser;
    }
    
    /**
     * Deactivate user account
     */
    public User deactivateUser(String userId) {
        User user = findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        User deactivatedUser = user.withStatus(User.UserStatus.INACTIVE);
        
        // Update database and cache
        userDatabase.put(userId, deactivatedUser);
        userCache.put(userId, deactivatedUser);
        
        metricsCollector.recordMetric("user.deactivation", 1);
        return deactivatedUser;
    }
}
