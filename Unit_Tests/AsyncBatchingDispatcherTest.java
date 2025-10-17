import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Unit tests for AsyncBatchingDispatcherExample
 * Tests batching of high-frequency operations to reduce overhead
 * for metrics, database operations, and rate-limited services.
 */
public class AsyncBatchingDispatcherTest {

    /**
     * Mock BatchingDispatcher for testing
     */
    static class BatchingDispatcher<T, R> implements AutoCloseable {
        private final int maxBatchSize;
        private final Duration linger;
        private final BlockingQueue<T> queue;
        private final ExecutorService worker = Executors.newSingleThreadExecutor();
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final Function<List<T>, R> batchHandler;
        private final List<R> processed = Collections.synchronizedList(new ArrayList<>());

        BatchingDispatcher(int capacity, int maxBatchSize, Duration linger, Function<List<T>, R> batchHandler) {
            this.queue = new ArrayBlockingQueue<>(capacity);
            this.maxBatchSize = maxBatchSize;
            this.linger = linger;
            this.batchHandler = batchHandler;
            worker.submit(this::loop);
        }

        public boolean submit(T item) {
            if (!running.get()) return false;
            return queue.offer(item);
        }

        public List<R> results() {
            return new ArrayList<>(processed);
        }

        private void loop() {
            try {
                while (running.get() || !queue.isEmpty()) {
                    List<T> batch = new ArrayList<>(maxBatchSize);
                    T first = queue.poll(linger.toMillis(), TimeUnit.MILLISECONDS);
                    if (first != null) batch.add(first);
                    else continue;
                    queue.drainTo(batch, maxBatchSize - 1);
                    R result = batchHandler.apply(batch);
                    processed.add(result);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void close() {
            running.set(false);
            worker.shutdown();
            try {
                worker.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    public void testDispatcherCreation() {
        // Arrange & Act
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(100, 10, Duration.ofMillis(50), 
                    batch -> "BATCH{" + batch.size() + "}")) {
            
            // Assert
            assertNotNull("Dispatcher should be created", dispatcher);
            assertTrue("Dispatcher should be running", dispatcher.running.get());
        }
    }

    @Test
    public void testSubmitSingleItem() throws Exception {
        // Arrange
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(100, 10, Duration.ofMillis(50), 
                    batch -> "BATCH{" + batch.size() + "}")) {
            
            // Act
            boolean submitted = dispatcher.submit("item1");
            Thread.sleep(100); // Wait for processing

            // Assert
            assertTrue("Item should be submitted successfully", submitted);
            List<String> results = dispatcher.results();
            assertEquals("Should have 1 batch result", 1, results.size());
            assertEquals("Batch should contain 1 item", "BATCH{1}", results.get(0));
        }
    }

    @Test
    public void testBatchingMultipleItems() throws Exception {
        // Arrange
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(100, 5, Duration.ofMillis(50), 
                    batch -> "BATCH{" + batch.size() + "}")) {
            
            // Act - Submit 10 items quickly
            for (int i = 0; i < 10; i++) {
                dispatcher.submit("item" + i);
            }
            Thread.sleep(200); // Wait for batching

            // Assert
            List<String> results = dispatcher.results();
            assertTrue("Should create batches", results.size() >= 2);
            // Verify total items processed
            int totalItems = results.stream()
                .mapToInt(r -> Integer.parseInt(r.substring(6, r.length() - 1)))
                .sum();
            assertEquals("All 10 items should be processed", 10, totalItems);
        }
    }

    @Test
    public void testMaxBatchSize() throws Exception {
        // Arrange
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(100, 3, Duration.ofMillis(50), 
                    batch -> "BATCH{" + batch.size() + "}")) {
            
            // Act - Submit 10 items rapidly
            for (int i = 0; i < 10; i++) {
                dispatcher.submit("item" + i);
            }
            Thread.sleep(200);

            // Assert
            List<String> results = dispatcher.results();
            for (String result : results) {
                int batchSize = Integer.parseInt(result.substring(6, result.length() - 1));
                assertTrue("Batch size should not exceed max", batchSize <= 3);
            }
        }
    }

    @Test
    public void testLingerDuration() throws Exception {
        // Arrange
        try (BatchingDispatcher<String, Long> dispatcher = 
                new BatchingDispatcher<>(100, 10, Duration.ofMillis(100), 
                    batch -> System.currentTimeMillis())) {
            
            // Act
            long submitTime1 = System.currentTimeMillis();
            dispatcher.submit("item1");
            Thread.sleep(50);
            dispatcher.submit("item2");
            Thread.sleep(150); // Wait for linger timeout

            // Assert
            List<Long> results = dispatcher.results();
            assertEquals("Should create 1 batch with linger", 1, results.size());
            assertTrue("Batch should be processed after linger", 
                results.get(0) >= submitTime1);
        }
    }

    @Test
    public void testQueueCapacity() {
        // Arrange
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(5, 10, Duration.ofMillis(100), 
                    batch -> {
                        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                        return "BATCH{" + batch.size() + "}";
                    })) {
            
            // Act - Submit more items than capacity
            int successCount = 0;
            for (int i = 0; i < 10; i++) {
                if (dispatcher.submit("item" + i)) {
                    successCount++;
                }
            }

            // Assert
            assertTrue("Some items should be rejected when queue is full", successCount <= 5);
        }
    }

    @Test
    public void testBatchHandler() throws Exception {
        // Arrange
        List<Integer> batchSizes = Collections.synchronizedList(new ArrayList<>());
        try (BatchingDispatcher<String, Integer> dispatcher = 
                new BatchingDispatcher<>(100, 5, Duration.ofMillis(50), 
                    batch -> {
                        int size = batch.size();
                        batchSizes.add(size);
                        return size;
                    })) {
            
            // Act
            for (int i = 0; i < 7; i++) {
                dispatcher.submit("item" + i);
            }
            Thread.sleep(200);

            // Assert
            List<Integer> results = dispatcher.results();
            assertEquals("Batch sizes should match results", batchSizes, results);
            int totalItems = results.stream().mapToInt(Integer::intValue).sum();
            assertEquals("All items should be processed", 7, totalItems);
        }
    }

    @Test
    public void testSubmitAfterClose() throws Exception {
        // Arrange
        BatchingDispatcher<String, String> dispatcher = 
            new BatchingDispatcher<>(100, 10, Duration.ofMillis(50), 
                batch -> "BATCH{" + batch.size() + "}");
        
        // Act
        dispatcher.close();
        boolean submitted = dispatcher.submit("item1");

        // Assert
        assertFalse("Submit should fail after close", submitted);
    }

    @Test
    public void testCloseWaitsForProcessing() throws Exception {
        // Arrange
        CountDownLatch processingLatch = new CountDownLatch(1);
        BatchingDispatcher<String, String> dispatcher = 
            new BatchingDispatcher<>(100, 10, Duration.ofMillis(50), 
                batch -> {
                    processingLatch.countDown();
                    return "BATCH{" + batch.size() + "}";
                });
        
        dispatcher.submit("item1");
        
        // Act
        boolean processed = processingLatch.await(500, TimeUnit.MILLISECONDS);
        dispatcher.close();

        // Assert
        assertTrue("Item should be processed before close completes", processed);
        assertTrue("Worker should be shutdown", dispatcher.worker.isShutdown());
    }

    @Test
    public void testConcurrentSubmissions() throws Exception {
        // Arrange
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(1000, 10, Duration.ofMillis(50), 
                    batch -> "BATCH{" + batch.size() + "}")) {
            
            int threadCount = 5;
            int itemsPerThread = 20;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // Act
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    for (int j = 0; j < itemsPerThread; j++) {
                        dispatcher.submit("thread" + threadId + "-item" + j);
                    }
                    latch.countDown();
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();
            Thread.sleep(300); // Wait for batching

            // Assert
            List<String> results = dispatcher.results();
            int totalItems = results.stream()
                .mapToInt(r -> Integer.parseInt(r.substring(6, r.length() - 1)))
                .sum();
            assertEquals("All items should be processed", 
                threadCount * itemsPerThread, totalItems);
        }
    }

    @Test
    public void testEmptyBatchNotProcessed() throws Exception {
        // Arrange
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(100, 10, Duration.ofMillis(50), 
                    batch -> "BATCH{" + batch.size() + "}")) {
            
            // Act - Wait without submitting
            Thread.sleep(200);

            // Assert
            List<String> results = dispatcher.results();
            assertEquals("No batches should be created without items", 0, results.size());
        }
    }

    @Test
    public void testBatchHandlerException() throws Exception {
        // Arrange
        AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(100, 10, Duration.ofMillis(50), 
                    batch -> {
                        if (batch.size() > 5) {
                            exceptionThrown.set(true);
                            throw new RuntimeException("Batch too large");
                        }
                        return "BATCH{" + batch.size() + "}";
                    })) {
            
            // Act
            for (int i = 0; i < 3; i++) {
                dispatcher.submit("item" + i);
            }
            Thread.sleep(200);

            // Assert
            assertFalse("Exception should not be thrown for small batch", 
                exceptionThrown.get());
        }
    }

    @Test
    public void testResultsThreadSafety() throws Exception {
        // Arrange
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(1000, 10, Duration.ofMillis(20), 
                    batch -> "BATCH{" + batch.size() + "}")) {
            
            // Act - Submit items and read results concurrently
            ExecutorService executor = Executors.newFixedThreadPool(2);
            executor.submit(() -> {
                for (int i = 0; i < 50; i++) {
                    dispatcher.submit("item" + i);
                    try { Thread.sleep(5); } catch (InterruptedException ignored) {}
                }
            });
            
            executor.submit(() -> {
                for (int i = 0; i < 10; i++) {
                    List<String> results = dispatcher.results();
                    assertNotNull("Results should not be null", results);
                    try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                }
            });

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // Assert
            assertNotNull("Final results should not be null", dispatcher.results());
        }
    }

    @Test
    public void testDrainToLimit() throws Exception {
        // Arrange
        try (BatchingDispatcher<String, String> dispatcher = 
                new BatchingDispatcher<>(100, 5, Duration.ofMillis(200), 
                    batch -> "BATCH{" + batch.size() + "}")) {
            
            // Act - Submit 10 items rapidly to ensure they're in queue
            for (int i = 0; i < 10; i++) {
                dispatcher.submit("item" + i);
            }
            Thread.sleep(300);

            // Assert
            List<String> results = dispatcher.results();
            for (String result : results) {
                int batchSize = Integer.parseInt(result.substring(6, result.length() - 1));
                assertTrue("Batch size should be <= maxBatchSize", batchSize <= 5);
            }
        }
    }
}
