import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.util.concurrent.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for StructuredConcurrencyExample
 * Tests structured concurrency patterns for managing concurrent tasks
 * with clear lifecycle ownership and result aggregation.
 */
public class StructuredConcurrencyTest {

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
     * Mock classes for testing
     */
    record PriceQuote(String provider, double price, java.time.Instant ts) {}

    static class StructuredPriceScope implements AutoCloseable {
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final ConcurrentLinkedQueue<Future<PriceQuote>> futures = new ConcurrentLinkedQueue<>();
        private volatile boolean closed = false;

        public void fork(Callable<PriceQuote> task) {
            if (closed) throw new IllegalStateException("Scope closed");
            futures.add(executor.submit(task));
        }

        public List<PriceQuote> joinAndCollectFastestSuccess(int needed, long timeout, TimeUnit unit) 
                throws InterruptedException, TimeoutException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            var results = new ArrayList<PriceQuote>();
            while (System.nanoTime() < deadline && results.size() < needed) {
                for (var f : futures) {
                    if (f.isDone()) {
                        futures.remove(f);
                        try { 
                            results.add(f.get()); 
                        } catch (ExecutionException ex) { 
                            /* ignore failed */ 
                        }
                        if (results.size() >= needed) break;
                    }
                }
                if (results.size() < needed) {
                    Thread.sleep(10);
                }
            }
            if (results.size() < needed) {
                throw new TimeoutException("Not enough successful results");
            }
            return results;
        }

        @Override 
        public void close() {
            closed = true;
            futures.forEach(f -> f.cancel(true));
            executor.shutdownNow();
        }
    }

    @Test
    public void testScopeCreation() {
        // Arrange & Act
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            // Assert
            assertNotNull("Scope should be created", scope);
            assertFalse("Scope should not be closed initially", scope.closed);
        }
    }

    @Test
    public void testForkSingleTask() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            // Act
            scope.fork(() -> new PriceQuote("Provider1", 100.0, java.time.Instant.now()));
            List<PriceQuote> results = scope.joinAndCollectFastestSuccess(1, 1, TimeUnit.SECONDS);

            // Assert
            assertEquals("Should return 1 result", 1, results.size());
            assertEquals("Provider should be Provider1", "Provider1", results.get(0).provider());
            assertEquals("Price should be 100.0", 100.0, results.get(0).price(), 0.01);
        }
    }

    @Test
    public void testForkMultipleTasks() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            // Act
            scope.fork(() -> new PriceQuote("A", 101.0, java.time.Instant.now()));
            scope.fork(() -> new PriceQuote("B", 99.0, java.time.Instant.now()));
            scope.fork(() -> new PriceQuote("C", 100.0, java.time.Instant.now()));

            List<PriceQuote> results = scope.joinAndCollectFastestSuccess(3, 2, TimeUnit.SECONDS);

            // Assert
            assertEquals("Should return 3 results", 3, results.size());
            Set<String> providers = new HashSet<>();
            for (PriceQuote quote : results) {
                providers.add(quote.provider());
            }
            assertEquals("Should have 3 different providers", 3, providers.size());
        }
    }

    @Test
    public void testCollectFastestSuccess() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            // Act - Fork tasks with different latencies
            scope.fork(() -> {
                Thread.sleep(200);
                return new PriceQuote("Slow", 101.0, java.time.Instant.now());
            });
            scope.fork(() -> {
                Thread.sleep(50);
                return new PriceQuote("Fast", 99.0, java.time.Instant.now());
            });
            scope.fork(() -> {
                Thread.sleep(100);
                return new PriceQuote("Medium", 100.0, java.time.Instant.now());
            });

            long startTime = System.currentTimeMillis();
            List<PriceQuote> results = scope.joinAndCollectFastestSuccess(2, 1, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            // Assert
            assertEquals("Should return 2 fastest results", 2, results.size());
            assertTrue("Should complete in less than 300ms", duration < 300);
        }
    }

    @Test(expected = TimeoutException.class)
    public void testTimeoutWhenNotEnoughResults() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            // Act - Fork tasks that are too slow
            scope.fork(() -> {
                Thread.sleep(500);
                return new PriceQuote("Slow1", 100.0, java.time.Instant.now());
            });
            scope.fork(() -> {
                Thread.sleep(500);
                return new PriceQuote("Slow2", 100.0, java.time.Instant.now());
            });

            // Assert - Should timeout
            scope.joinAndCollectFastestSuccess(2, 200, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void testFailedTasksAreIgnored() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            // Act
            scope.fork(() -> {
                throw new RuntimeException("Task failed");
            });
            scope.fork(() -> new PriceQuote("Success1", 100.0, java.time.Instant.now()));
            scope.fork(() -> new PriceQuote("Success2", 101.0, java.time.Instant.now()));

            List<PriceQuote> results = scope.joinAndCollectFastestSuccess(2, 1, TimeUnit.SECONDS);

            // Assert
            assertEquals("Should return only successful results", 2, results.size());
            for (PriceQuote quote : results) {
                assertTrue("Provider should be Success1 or Success2", 
                    quote.provider().startsWith("Success"));
            }
        }
    }

    @Test
    public void testPartialFailures() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            // Act
            scope.fork(() -> new PriceQuote("Success", 100.0, java.time.Instant.now()));
            scope.fork(() -> {
                throw new RuntimeException("Failed");
            });
            scope.fork(() -> {
                throw new RuntimeException("Failed");
            });

            List<PriceQuote> results = scope.joinAndCollectFastestSuccess(1, 1, TimeUnit.SECONDS);

            // Assert
            assertEquals("Should return 1 successful result", 1, results.size());
            assertEquals("Provider should be Success", "Success", results.get(0).provider());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testForkAfterClose() {
        // Arrange
        StructuredPriceScope scope = new StructuredPriceScope();
        scope.close();

        // Act & Assert
        scope.fork(() -> new PriceQuote("Test", 100.0, java.time.Instant.now()));
    }

    @Test
    public void testCloseScope() {
        // Arrange
        StructuredPriceScope scope = new StructuredPriceScope();
        scope.fork(() -> {
            Thread.sleep(1000);
            return new PriceQuote("Test", 100.0, java.time.Instant.now());
        });

        // Act
        scope.close();

        // Assert
        assertTrue("Scope should be closed", scope.closed);
        assertTrue("Executor should be shutdown", scope.executor.isShutdown());
    }

    @Test
    public void testAutomaticResourceManagement() throws Exception {
        // Arrange
        StructuredPriceScope scope;

        // Act
        try (StructuredPriceScope s = new StructuredPriceScope()) {
            scope = s;
            s.fork(() -> new PriceQuote("Test", 100.0, java.time.Instant.now()));
        }

        // Assert
        assertTrue("Scope should be closed after try-with-resources", scope.closed);
        assertTrue("Executor should be shutdown", scope.executor.isShutdown());
    }

    @Test
    public void testConcurrentForks() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            CountDownLatch latch = new CountDownLatch(10);

            // Act
            for (int i = 0; i < 10; i++) {
                final int index = i;
                testExecutor.submit(() -> {
                    scope.fork(() -> new PriceQuote("Provider" + index, 100.0 + index, 
                        java.time.Instant.now()));
                    latch.countDown();
                });
            }

            latch.await(2, TimeUnit.SECONDS);
            List<PriceQuote> results = scope.joinAndCollectFastestSuccess(10, 2, TimeUnit.SECONDS);

            // Assert
            assertEquals("Should return all 10 results", 10, results.size());
        }
    }

    @Test
    public void testPriceQuoteCreation() {
        // Arrange & Act
        java.time.Instant now = java.time.Instant.now();
        PriceQuote quote = new PriceQuote("TestProvider", 123.45, now);

        // Assert
        assertEquals("Provider should match", "TestProvider", quote.provider());
        assertEquals("Price should match", 123.45, quote.price(), 0.001);
        assertEquals("Timestamp should match", now, quote.ts());
    }

    @Test
    public void testCollectMinimumResults() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            // Act
            for (int i = 0; i < 5; i++) {
                final int index = i;
                scope.fork(() -> new PriceQuote("Provider" + index, 100.0 + index, 
                    java.time.Instant.now()));
            }

            List<PriceQuote> results = scope.joinAndCollectFastestSuccess(3, 1, TimeUnit.SECONDS);

            // Assert
            assertEquals("Should return exactly 3 results", 3, results.size());
        }
    }

    @Test
    public void testEmptyScope() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            // Act & Assert - Should timeout with no tasks
            try {
                scope.joinAndCollectFastestSuccess(1, 100, TimeUnit.MILLISECONDS);
                fail("Should throw TimeoutException when no tasks are forked");
            } catch (TimeoutException e) {
                // Expected
            }
        }
    }

    @Test
    public void testResultTimestamp() throws Exception {
        // Arrange
        try (StructuredPriceScope scope = new StructuredPriceScope()) {
            java.time.Instant before = java.time.Instant.now();
            
            // Act
            scope.fork(() -> new PriceQuote("Test", 100.0, java.time.Instant.now()));
            Thread.sleep(100);
            List<PriceQuote> results = scope.joinAndCollectFastestSuccess(1, 1, TimeUnit.SECONDS);
            java.time.Instant after = java.time.Instant.now();

            // Assert
            PriceQuote quote = results.get(0);
            assertTrue("Timestamp should be after start", 
                quote.ts().isAfter(before) || quote.ts().equals(before));
            assertTrue("Timestamp should be before end", 
                quote.ts().isBefore(after) || quote.ts().equals(after));
        }
    }
}
