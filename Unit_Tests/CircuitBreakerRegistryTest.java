package unittests;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Unit tests for CircuitBreakerRegistry
 * Refactored: October 20, 2025
 * 
 * Tests infrastructure component for managing multiple circuit breakers
 * ensuring fault tolerance and graceful degradation.
 * Tests the circuit breaker registry infrastructure component
 * for managing multiple circuit breakers and fault tolerance.
 */
public class CircuitBreakerRegistryTest {

    /**
     * Mock classes for testing
     */
    static class CircuitBreaker {
        private final String name;
        private final int failureThreshold;
        private final long timeoutMillis;
        private int failureCount = 0;
        private CircuitState state = CircuitState.CLOSED;
        private long lastFailureTime = 0;

        enum CircuitState { CLOSED, OPEN, HALF_OPEN }

        CircuitBreaker(String name, int failureThreshold, java.time.Duration timeout) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.timeoutMillis = timeout.toMillis();
        }

        public <T> T execute(CircuitBreakerOperation<T> operation) throws Exception {
            if (state == CircuitState.OPEN) {
                long now = System.currentTimeMillis();
                if (now - lastFailureTime < timeoutMillis) {
                    throw new RuntimeException("Circuit breaker is OPEN for " + name);
                }
                state = CircuitState.HALF_OPEN;
            }

            try {
                T result = operation.execute();
                if (state == CircuitState.HALF_OPEN) {
                    state = CircuitState.CLOSED;
                    failureCount = 0;
                }
                return result;
            } catch (Exception e) {
                recordFailure();
                throw e;
            }
        }

        private void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            if (failureCount >= failureThreshold) {
                state = CircuitState.OPEN;
            }
        }

        public CircuitState getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public String getName() { return name; }
    }

    interface CircuitBreakerOperation<T> {
        T execute() throws Exception;
    }

    static class CircuitBreakerRegistry {
        private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

        public void registerCircuitBreaker(String name, int failureThreshold, java.time.Duration timeout) {
            circuitBreakers.put(name, new CircuitBreaker(name, failureThreshold, timeout));
        }

        public CircuitBreaker getCircuitBreaker(String name) {
            return circuitBreakers.get(name);
        }

        public <T> T execute(String circuitBreakerName, CircuitBreakerOperation<T> operation) throws Exception {
            CircuitBreaker circuitBreaker = getCircuitBreaker(circuitBreakerName);
            if (circuitBreaker == null) {
                throw new IllegalArgumentException("Circuit breaker not found: " + circuitBreakerName);
            }
            return circuitBreaker.execute(operation);
        }

        public int getCircuitBreakerCount() {
            return circuitBreakers.size();
        }
    }

    private CircuitBreakerRegistry registry;

    @Before
    public void setUp() {
        registry = new CircuitBreakerRegistry();
    }

    @Test
    public void testRegistryCreation() {
        // Assert
        assertNotNull("Registry should be created", registry);
        assertEquals("Initial registry should be empty", 0, registry.getCircuitBreakerCount());
    }

    @Test
    public void testRegisterCircuitBreaker() {
        // Act
        registry.registerCircuitBreaker("test-service", 5, java.time.Duration.ofSeconds(30));

        // Assert
        assertEquals("Registry should have 1 circuit breaker", 1, registry.getCircuitBreakerCount());
        assertNotNull("Circuit breaker should be retrievable", registry.getCircuitBreaker("test-service"));
    }

    @Test
    public void testRegisterMultipleCircuitBreakers() {
        // Act
        registry.registerCircuitBreaker("payment-service", 5, java.time.Duration.ofSeconds(30));
        registry.registerCircuitBreaker("api-service", 3, java.time.Duration.ofSeconds(60));
        registry.registerCircuitBreaker("database", 10, java.time.Duration.ofSeconds(15));

        // Assert
        assertEquals("Registry should have 3 circuit breakers", 3, registry.getCircuitBreakerCount());
    }

    @Test
    public void testGetCircuitBreaker() {
        // Arrange
        registry.registerCircuitBreaker("test-service", 5, java.time.Duration.ofSeconds(30));

        // Act
        CircuitBreaker cb = registry.getCircuitBreaker("test-service");

        // Assert
        assertNotNull("Circuit breaker should exist", cb);
        assertEquals("Circuit breaker name should match", "test-service", cb.getName());
    }

    @Test
    public void testGetNonExistentCircuitBreaker() {
        // Act
        CircuitBreaker cb = registry.getCircuitBreaker("non-existent");

        // Assert
        assertNull("Non-existent circuit breaker should return null", cb);
    }

    @Test
    public void testExecuteSuccessfulOperation() throws Exception {
        // Arrange
        registry.registerCircuitBreaker("test-service", 5, java.time.Duration.ofSeconds(30));

        // Act
        String result = registry.execute("test-service", () -> "SUCCESS");

        // Assert
        assertEquals("Operation should succeed", "SUCCESS", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecuteWithNonExistentCircuitBreaker() throws Exception {
        // Act
        registry.execute("non-existent", () -> "SUCCESS");
    }

    @Test
    public void testCircuitBreakerStateTransition() throws Exception {
        // Arrange
        registry.registerCircuitBreaker("test-service", 2, java.time.Duration.ofSeconds(1));
        CircuitBreaker cb = registry.getCircuitBreaker("test-service");

        // Act & Assert - Initial state
        assertEquals("Initial state should be CLOSED", CircuitBreaker.CircuitState.CLOSED, cb.getState());

        // Trigger failures
        for (int i = 0; i < 2; i++) {
            try {
                registry.execute("test-service", () -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception ignored) {}
        }

        // Assert - Should be OPEN after threshold
        assertEquals("State should be OPEN after failures", CircuitBreaker.CircuitState.OPEN, cb.getState());
    }

    @Test
    public void testCircuitBreakerOpensAfterThreshold() throws Exception {
        // Arrange
        registry.registerCircuitBreaker("test-service", 3, java.time.Duration.ofSeconds(1));

        // Act - Trigger failures
        for (int i = 0; i < 3; i++) {
            try {
                registry.execute("test-service", () -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception ignored) {}
        }

        // Assert - Next call should fail immediately
        try {
            registry.execute("test-service", () -> "SUCCESS");
            fail("Should throw exception when circuit is OPEN");
        } catch (Exception e) {
            assertTrue("Should indicate circuit is OPEN", 
                e.getMessage().contains("Circuit breaker is OPEN"));
        }
    }

    @Test
    public void testCircuitBreakerRecovery() throws Exception {
        // Arrange
        registry.registerCircuitBreaker("test-service", 2, java.time.Duration.ofMillis(100));

        // Act - Trigger failures to open circuit
        for (int i = 0; i < 2; i++) {
            try {
                registry.execute("test-service", () -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception ignored) {}
        }

        // Wait for timeout
        Thread.sleep(150);

        // Successful operation should close circuit
        String result = registry.execute("test-service", () -> "RECOVERED");
        CircuitBreaker cb = registry.getCircuitBreaker("test-service");

        // Assert
        assertEquals("Operation should succeed after recovery", "RECOVERED", result);
        assertEquals("Circuit should be CLOSED after successful recovery", 
            CircuitBreaker.CircuitState.CLOSED, cb.getState());
        assertEquals("Failure count should be reset", 0, cb.getFailureCount());
    }

    @Test
    public void testIndependentCircuitBreakers() throws Exception {
        // Arrange
        registry.registerCircuitBreaker("service1", 2, java.time.Duration.ofSeconds(1));
        registry.registerCircuitBreaker("service2", 2, java.time.Duration.ofSeconds(1));

        // Act - Fail service1
        for (int i = 0; i < 2; i++) {
            try {
                registry.execute("service1", () -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception ignored) {}
        }

        // Assert - service1 should be OPEN, service2 should be CLOSED
        assertEquals("Service1 should be OPEN", 
            CircuitBreaker.CircuitState.OPEN, 
            registry.getCircuitBreaker("service1").getState());
        assertEquals("Service2 should be CLOSED", 
            CircuitBreaker.CircuitState.CLOSED, 
            registry.getCircuitBreaker("service2").getState());

        // service2 should still work
        String result = registry.execute("service2", () -> "SUCCESS");
        assertEquals("Service2 should work normally", "SUCCESS", result);
    }

    @Test
    public void testConcurrentRegistration() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                registry.registerCircuitBreaker("service" + index, 5, java.time.Duration.ofSeconds(30));
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertEquals("All circuit breakers should be registered", 
            threadCount, registry.getCircuitBreakerCount());
    }

    @Test
    public void testFailureCountIncrement() throws Exception {
        // Arrange
        registry.registerCircuitBreaker("test-service", 5, java.time.Duration.ofSeconds(1));
        CircuitBreaker cb = registry.getCircuitBreaker("test-service");

        // Act
        for (int i = 0; i < 3; i++) {
            try {
                registry.execute("test-service", () -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception ignored) {}
        }

        // Assert
        assertEquals("Failure count should be 3", 3, cb.getFailureCount());
    }

    @Test
    public void testMixedOperations() throws Exception {
        // Arrange
        registry.registerCircuitBreaker("test-service", 3, java.time.Duration.ofSeconds(1));

        // Act - Success
        String result1 = registry.execute("test-service", () -> "SUCCESS1");
        
        // Failure
        try {
            registry.execute("test-service", () -> {
                throw new RuntimeException("Failure");
            });
        } catch (Exception ignored) {}

        // Success again
        String result2 = registry.execute("test-service", () -> "SUCCESS2");

        // Assert
        assertEquals("First operation should succeed", "SUCCESS1", result1);
        assertEquals("Third operation should succeed", "SUCCESS2", result2);
        assertEquals("Failure count should be 1", 1, 
            registry.getCircuitBreaker("test-service").getFailureCount());
    }

    @Test
    public void testDifferentThresholds() throws Exception {
        // Arrange
        registry.registerCircuitBreaker("low-threshold", 2, java.time.Duration.ofSeconds(1));
        registry.registerCircuitBreaker("high-threshold", 10, java.time.Duration.ofSeconds(1));

        // Act - Fail both services same number of times
        for (int i = 0; i < 5; i++) {
            try {
                registry.execute("low-threshold", () -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception ignored) {}
            
            try {
                registry.execute("high-threshold", () -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception ignored) {}
        }

        // Assert
        assertEquals("Low threshold should be OPEN", 
            CircuitBreaker.CircuitState.OPEN, 
            registry.getCircuitBreaker("low-threshold").getState());
        assertEquals("High threshold should still be CLOSED", 
            CircuitBreaker.CircuitState.CLOSED, 
            registry.getCircuitBreaker("high-threshold").getState());
    }
}
