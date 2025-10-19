import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for Rate Limiter
 * Tests token bucket algorithm and request throttling
 */
public class RateLimiterTest {

    private RateLimiter rateLimiter;

    @Before
    public void setUp() {
        // 5 requests per second
        rateLimiter = new RateLimiter(5, TimeUnit.SECONDS);
    }

    @Test
    public void testTryAcquire_WithinLimit_ReturnsTrue() {
        // Act & Assert
        assertTrue("First request should be allowed", rateLimiter.tryAcquire());
        assertTrue("Second request should be allowed", rateLimiter.tryAcquire());
        assertTrue("Third request should be allowed", rateLimiter.tryAcquire());
    }

    @Test
    public void testTryAcquire_ExceedsLimit_ReturnsFalse() {
        // Arrange - Consume all tokens
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire();
        }

        // Act
        boolean result = rateLimiter.tryAcquire();

        // Assert
        assertFalse("Request exceeding limit should be denied", result);
    }

    @Test
    public void testTryAcquire_AfterRefill_AllowsRequests() throws InterruptedException {
        // Arrange - Consume all tokens
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire();
        }

        // Act - Wait for refill
        Thread.sleep(1100);
        boolean result = rateLimiter.tryAcquire();

        // Assert
        assertTrue("Request after refill should be allowed", result);
    }

    @Test
    public void testConcurrentRequests_RespectsLimit() throws InterruptedException {
        // Arrange
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                if (rateLimiter.tryAcquire()) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert
        assertEquals("Should allow exactly 5 requests", 5, successCount.get());
        assertEquals("Should deny exactly 5 requests", 5, failureCount.get());
    }

    @Test
    public void testAcquire_BlocksUntilAvailable() throws InterruptedException {
        // Arrange - Consume all tokens
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire();
        }

        // Act - Blocking acquire
        long startTime = System.currentTimeMillis();
        rateLimiter.acquire(); // Should wait for refill
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertTrue("Should have waited for refill", duration >= 900);
    }

    @Test
    public void testTryAcquire_WithPermits_Success() {
        // Act
        boolean result = rateLimiter.tryAcquire(3);

        // Assert
        assertTrue("Should acquire 3 permits", result);
        assertEquals("Should have 2 permits remaining", 2, rateLimiter.availablePermits());
    }

    @Test
    public void testTryAcquire_TooManyPermits_Fails() {
        // Act
        boolean result = rateLimiter.tryAcquire(10);

        // Assert
        assertFalse("Should not acquire more permits than capacity", result);
    }

    // Rate Limiter implementation using Token Bucket algorithm
    static class RateLimiter {
        private final int capacity;
        private final long refillIntervalNanos;
        private int availableTokens;
        private long lastRefillTime;

        public RateLimiter(int permitsPerTimeUnit, TimeUnit timeUnit) {
            this.capacity = permitsPerTimeUnit;
            this.availableTokens = permitsPerTimeUnit;
            this.refillIntervalNanos = timeUnit.toNanos(1);
            this.lastRefillTime = System.nanoTime();
        }

        public synchronized boolean tryAcquire() {
            return tryAcquire(1);
        }

        public synchronized boolean tryAcquire(int permits) {
            refill();
            if (availableTokens >= permits) {
                availableTokens -= permits;
                return true;
            }
            return false;
        }

        public synchronized void acquire() throws InterruptedException {
            while (!tryAcquire()) {
                Thread.sleep(100);
            }
        }

        public synchronized int availablePermits() {
            refill();
            return availableTokens;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillTime;
            
            if (elapsed >= refillIntervalNanos) {
                availableTokens = capacity;
                lastRefillTime = now;
            }
        }
    }
}
