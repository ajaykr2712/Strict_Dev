package com.ecommerce.infrastructure.circuitbreaker;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * Circuit Breaker Registry - Infrastructure Component
 * 
 * Implements the Circuit Breaker pattern for system resilience.
 * Based on principles from "Designing Data-Intensive Applications" for fault tolerance.
 * 
 * Features:
 * - Automatic failure detection and recovery
 * - Configurable thresholds and timeouts
 * - Multiple circuit breakers for different services
 * - Graceful degradation capabilities
 */
public class CircuitBreakerRegistry {
    private final Map<String, CircuitBreaker> circuitBreakers;
    
    public CircuitBreakerRegistry() {
        this.circuitBreakers = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        System.out.println("CircuitBreakerRegistry initialized");
        
        // Register default circuit breakers for critical services
        registerCircuitBreaker("payment-service", 5, Duration.ofSeconds(30));
        registerCircuitBreaker("external-api", 3, Duration.ofSeconds(60));
        registerCircuitBreaker("database", 10, Duration.ofSeconds(15));
    }
    
    /**
     * Register a new circuit breaker
     */
    public void registerCircuitBreaker(String name, int failureThreshold, Duration timeout) {
        circuitBreakers.put(name, new CircuitBreaker(name, failureThreshold, timeout));
        System.out.println("Registered circuit breaker: " + name + 
                          " (threshold=" + failureThreshold + ", timeout=" + timeout + ")");
    }
    
    /**
     * Get circuit breaker by name
     */
    public CircuitBreaker getCircuitBreaker(String name) {
        return circuitBreakers.get(name);
    }
    
    /**
     * Execute operation with circuit breaker protection
     */
    public <T> T execute(String circuitBreakerName, CircuitBreakerOperation<T> operation) throws Exception {
        CircuitBreaker circuitBreaker = getCircuitBreaker(circuitBreakerName);
        if (circuitBreaker == null) {
            throw new IllegalArgumentException("Circuit breaker not found: " + circuitBreakerName);
        }
        
        return circuitBreaker.execute(operation);
    }
    
    /**
     * Get status of all circuit breakers
     */
    public void printStatus() {
        System.out.println("\n=== Circuit Breaker Status ===");
        circuitBreakers.forEach((name, cb) -> 
            System.out.println("  " + name + ": " + cb.getState() + 
                             " (failures: " + cb.getFailureCount() + ")"));
    }
    
    /**
     * Functional interface for circuit breaker operations
     */
    @FunctionalInterface
    public interface CircuitBreakerOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Circuit Breaker Implementation
     */
    public static class CircuitBreaker {
        private final String name;
        private final int failureThreshold;
        private final Duration timeout;
        private final AtomicInteger failureCount;
        private final AtomicLong lastFailureTime;
        private volatile CircuitBreakerState state;
        
        public enum CircuitBreakerState {
            CLOSED,    // Normal operation
            OPEN,      // Failing fast
            HALF_OPEN  // Testing if service recovered
        }
        
        public CircuitBreaker(String name, int failureThreshold, Duration timeout) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
            this.failureCount = new AtomicInteger(0);
            this.lastFailureTime = new AtomicLong(0);
            this.state = CircuitBreakerState.CLOSED;
        }
        
        public <T> T execute(CircuitBreakerOperation<T> operation) throws Exception {
            if (state == CircuitBreakerState.OPEN) {
                if (shouldAttemptReset()) {
                    state = CircuitBreakerState.HALF_OPEN;
                } else {
                    throw new CircuitBreakerOpenException("Circuit breaker " + name + " is OPEN");
                }
            }
            
            try {
                T result = operation.execute();
                onSuccess();
                return result;
            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }
        
        private boolean shouldAttemptReset() {
            return System.currentTimeMillis() - lastFailureTime.get() > timeout.toMillis();
        }
        
        private void onSuccess() {
            failureCount.set(0);
            state = CircuitBreakerState.CLOSED;
        }
        
        private void onFailure() {
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            
            if (failureCount.get() >= failureThreshold) {
                state = CircuitBreakerState.OPEN;
                System.out.println("Circuit breaker " + name + " opened due to " + 
                                 failureCount.get() + " failures");
            }
        }
        
        public CircuitBreakerState getState() {
            return state;
        }
        
        public int getFailureCount() {
            return failureCount.get();
        }
        
        public String getName() {
            return name;
        }
    }
    
    /**
     * Exception thrown when circuit breaker is open
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
