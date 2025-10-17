import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for Circuit Breaker Pattern
 * Tests failure detection, circuit opening/closing, and recovery
 */
public class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;
    private AtomicInteger callCount;
    private AtomicInteger failureCount;

    @Before
    public void setUp() {
        circuitBreaker = new CircuitBreaker(3, 1000); // 3 failures, 1 second timeout
        callCount = new AtomicInteger(0);
        failureCount = new AtomicInteger(0);
    }

    @Test
    public void testCircuitBreaker_InitialState_IsClosed() {
        // Assert
        assertEquals("Initial state should be CLOSED", 
                     CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testExecute_SuccessfulCall_ReturnValue() throws Exception {
        // Act
        String result = circuitBreaker.execute(() -> "Success");

        // Assert
        assertEquals("Should return success value", "Success", result);
        assertEquals("Circuit should remain CLOSED", 
                     CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testExecute_MultipleFailures_OpensCircuit() {
        // Act - Cause failures to exceed threshold
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        // Assert
        assertEquals("Circuit should be OPEN after threshold failures", 
                     CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test(expected = CircuitBreakerOpenException.class)
    public void testExecute_CircuitOpen_ThrowsException() throws Exception {
        // Arrange - Open the circuit
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        // Act - Try to execute when circuit is open
        circuitBreaker.execute(() -> "Should not execute");
    }

    @Test
    public void testCircuit_HalfOpen_AfterTimeout() throws Exception {
        // Arrange - Open the circuit
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        // Act - Wait for timeout
        Thread.sleep(1100);

        // Assert
        assertEquals("Circuit should transition to HALF_OPEN", 
                     CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
    }

    @Test
    public void testCircuit_Recovery_ClosesAfterSuccess() throws Exception {
        // Arrange - Open the circuit
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        // Wait for timeout to half-open
        Thread.sleep(1100);

        // Act - Successful call in half-open state
        String result = circuitBreaker.execute(() -> "Success");

        // Assert
        assertEquals("Should return success", "Success", result);
        assertEquals("Circuit should close after success", 
                     CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testCircuit_FailureCount_ResetsOnSuccess() throws Exception {
        // Arrange - Two failures (below threshold)
        for (int i = 0; i < 2; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        // Act - Successful call resets counter
        circuitBreaker.execute(() -> "Success");

        // Another failure should not open circuit
        try {
            circuitBreaker.execute(() -> {
                throw new RuntimeException("Failure");
            });
        } catch (Exception e) {
            // Expected
        }

        // Assert
        assertEquals("Circuit should still be CLOSED", 
                     CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    // Circuit Breaker implementation
    static class CircuitBreaker {
        enum State { CLOSED, OPEN, HALF_OPEN }

        private State state = State.CLOSED;
        private int failureCount = 0;
        private final int failureThreshold;
        private final long timeout;
        private long lastFailureTime = 0;

        public CircuitBreaker(int failureThreshold, long timeout) {
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
        }

        public <T> T execute(Callable<T> operation) throws Exception {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime >= timeout) {
                    state = State.HALF_OPEN;
                } else {
                    throw new CircuitBreakerOpenException("Circuit is OPEN");
                }
            }

            try {
                T result = operation.call();
                onSuccess();
                return result;
            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }

        private void onSuccess() {
            failureCount = 0;
            state = State.CLOSED;
        }

        private void onFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            if (failureCount >= failureThreshold) {
                state = State.OPEN;
            }
        }

        public State getState() {
            if (state == State.OPEN && 
                System.currentTimeMillis() - lastFailureTime >= timeout) {
                return State.HALF_OPEN;
            }
            return state;
        }
    }

    interface Callable<T> {
        T call() throws Exception;
    }

    static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
