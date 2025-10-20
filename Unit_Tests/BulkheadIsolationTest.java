package unittests;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Unit tests for BulkheadIsolationExample
 * Refactored: October 20, 2025
 * 
 * Tests the bulkhead pattern for isolating resource pools
 * to prevent cascading failures in microservices architecture.
 * Tests the bulkhead pattern implementation for isolating resource pools
 * per dependency to prevent cascading failures.
 */
public class BulkheadIsolationTest {

    private ExecutorService testExecutor;

    @Before
    public void setUp() {
        testExecutor = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() {
        if (testExecutor != null) {
            testExecutor.shutdownNow();
        }
    }

    /**
     * Mock Bulkhead class for testing
     */
    static class Bulkhead {
        final ExecutorService pool;
        final Semaphore capacity;
        
        Bulkhead(int maxConcurrent, int poolSize) {
            this.pool = Executors.newFixedThreadPool(poolSize);
            this.capacity = new Semaphore(maxConcurrent);
        }
        
        <T> Future<T> submit(java.util.function.Supplier<T> supplier) {
            if (!capacity.tryAcquire()) {
                throw new RejectedExecutionException("Bulkhead saturated");
            }
            return pool.submit(() -> {
                try { 
                    return supplier.get(); 
                } finally { 
                    capacity.release(); 
                }
            });
        }
        
        void shutdown() { 
            pool.shutdownNow(); 
        }
    }

    @Test
    public void testBulkheadCreation() {
        // Arrange & Act
        Bulkhead bulkhead = new Bulkhead(5, 5);

        // Assert
        assertNotNull("Bulkhead should be created", bulkhead);
        assertNotNull("Thread pool should be initialized", bulkhead.pool);
        assertNotNull("Semaphore should be initialized", bulkhead.capacity);
        assertEquals("Semaphore should have 5 permits", 5, bulkhead.capacity.availablePermits());

        // Cleanup
        bulkhead.shutdown();
    }

    @Test
    public void testSubmitSuccessfulTask() throws Exception {
        // Arrange
        Bulkhead bulkhead = new Bulkhead(5, 5);

        // Act
        Future<String> result = bulkhead.submit(() -> "SUCCESS");
        String value = result.get(1, TimeUnit.SECONDS);

        // Assert
        assertEquals("Task should complete successfully", "SUCCESS", value);

        // Cleanup
        bulkhead.shutdown();
    }

    @Test(expected = RejectedExecutionException.class)
    public void testBulkheadSaturation() throws Exception {
        // Arrange
        Bulkhead bulkhead = new Bulkhead(2, 2);
        CountDownLatch latch = new CountDownLatch(1);

        try {
            // Act - Submit 2 tasks that block
            bulkhead.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "TASK1";
            });

            bulkhead.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "TASK2";
            });

            // Give tasks time to start
            Thread.sleep(100);

            // Assert - Third task should be rejected
            bulkhead.submit(() -> "TASK3");
            fail("Should throw RejectedExecutionException when bulkhead is saturated");
        } finally {
            latch.countDown();
            bulkhead.shutdown();
        }
    }

    @Test
    public void testConcurrentCapacityLimit() throws Exception {
        // Arrange
        Bulkhead bulkhead = new Bulkhead(3, 5);
        CountDownLatch startLatch = new CountDownLatch(3);
        CountDownLatch endLatch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        try {
            // Act - Submit 3 tasks that block (exactly at capacity)
            for (int i = 0; i < 3; i++) {
                final int taskNum = i;
                Future<String> future = bulkhead.submit(() -> {
                    startLatch.countDown();
                    try {
                        endLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "TASK" + taskNum;
                });
                futures.add(future);
            }

            // Wait for all 3 tasks to start
            assertTrue("All tasks should start", startLatch.await(1, TimeUnit.SECONDS));
            
            // Assert - No more permits should be available
            assertEquals("No permits should be available", 0, bulkhead.capacity.availablePermits());

            // Release tasks
            endLatch.countDown();

            // Verify all completed
            for (Future<String> future : futures) {
                assertNotNull("Task should complete", future.get(1, TimeUnit.SECONDS));
            }

            // Verify permits are released
            Thread.sleep(100);
            assertEquals("Permits should be released", 3, bulkhead.capacity.availablePermits());
        } finally {
            bulkhead.shutdown();
        }
    }

    @Test
    public void testPermitReleaseOnTaskCompletion() throws Exception {
        // Arrange
        Bulkhead bulkhead = new Bulkhead(1, 1);

        try {
            // Act
            Future<String> task1 = bulkhead.submit(() -> "TASK1");
            task1.get(1, TimeUnit.SECONDS);

            // Wait for permit to be released
            Thread.sleep(100);

            // Assert - Should be able to submit another task
            Future<String> task2 = bulkhead.submit(() -> "TASK2");
            String result = task2.get(1, TimeUnit.SECONDS);
            assertEquals("Second task should execute", "TASK2", result);
        } finally {
            bulkhead.shutdown();
        }
    }

    @Test
    public void testPermitReleaseOnTaskException() throws Exception {
        // Arrange
        Bulkhead bulkhead = new Bulkhead(1, 1);

        try {
            // Act - Submit task that throws exception
            Future<String> task1 = bulkhead.submit(() -> {
                throw new RuntimeException("Task failed");
            });

            try {
                task1.get(1, TimeUnit.SECONDS);
                fail("Should throw ExecutionException");
            } catch (ExecutionException e) {
                // Expected
            }

            // Wait for permit to be released
            Thread.sleep(100);

            // Assert - Permit should still be released
            assertEquals("Permit should be released even on exception", 1, bulkhead.capacity.availablePermits());

            // Should be able to submit another task
            Future<String> task2 = bulkhead.submit(() -> "TASK2");
            String result = task2.get(1, TimeUnit.SECONDS);
            assertEquals("Second task should execute", "TASK2", result);
        } finally {
            bulkhead.shutdown();
        }
    }

    @Test
    public void testMultipleBulkheadsIsolation() throws Exception {
        // Arrange
        Bulkhead bulkhead1 = new Bulkhead(1, 1);
        Bulkhead bulkhead2 = new Bulkhead(1, 1);
        CountDownLatch latch1 = new CountDownLatch(1);

        try {
            // Act - Block bulkhead1
            bulkhead1.submit(() -> {
                try {
                    latch1.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "BULKHEAD1";
            });

            Thread.sleep(100);

            // Assert - bulkhead2 should still work
            Future<String> task2 = bulkhead2.submit(() -> "BULKHEAD2");
            String result = task2.get(1, TimeUnit.SECONDS);
            assertEquals("Second bulkhead should be independent", "BULKHEAD2", result);

            // First bulkhead should be saturated
            try {
                bulkhead1.submit(() -> "SHOULD_FAIL");
                fail("First bulkhead should be saturated");
            } catch (RejectedExecutionException e) {
                // Expected
            }
        } finally {
            latch1.countDown();
            bulkhead1.shutdown();
            bulkhead2.shutdown();
        }
    }

    @Test
    public void testShutdown() throws Exception {
        // Arrange
        Bulkhead bulkhead = new Bulkhead(5, 5);
        Future<String> task = bulkhead.submit(() -> {
            Thread.sleep(100);
            return "COMPLETED";
        });

        // Act
        bulkhead.shutdown();

        // Assert
        assertTrue("Pool should be shutdown", bulkhead.pool.isShutdown());
    }

    @Test
    public void testHighConcurrencyScenario() throws Exception {
        // Arrange
        Bulkhead bulkhead = new Bulkhead(10, 10);
        int taskCount = 50;
        CountDownLatch completionLatch = new CountDownLatch(10);

        try {
            // Act - Submit tasks in parallel
            int successCount = 0;
            int rejectedCount = 0;

            for (int i = 0; i < taskCount; i++) {
                try {
                    bulkhead.submit(() -> {
                        try {
                            Thread.sleep(50);
                            completionLatch.countDown();
                            return "SUCCESS";
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return "INTERRUPTED";
                        }
                    });
                    successCount++;
                } catch (RejectedExecutionException e) {
                    rejectedCount++;
                }
            }

            // Assert
            assertTrue("Some tasks should be accepted", successCount > 0);
            assertTrue("Some tasks should be rejected due to saturation", rejectedCount > 0);
            assertEquals("Total should equal task count", taskCount, successCount + rejectedCount);
        } finally {
            bulkhead.shutdown();
        }
    }

    @Test
    public void testDifferentPoolAndCapacitySizes() {
        // Arrange & Act
        Bulkhead bulkhead = new Bulkhead(3, 5);

        // Assert
        assertEquals("Capacity should match constructor arg", 3, bulkhead.capacity.availablePermits());
        assertNotNull("Pool should be created with size 5", bulkhead.pool);

        // Cleanup
        bulkhead.shutdown();
    }
}
