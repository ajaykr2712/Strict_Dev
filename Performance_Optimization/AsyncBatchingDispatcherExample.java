// AsyncBatchingDispatcherExample.java
// Demonstrates batching of high-frequency small operations into larger units to reduce overhead.
// Use cases: metrics emission, database upserts, outbound HTTP calls to rate-limited services.

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncBatchingDispatcherExample {

    static class BatchingDispatcher<T,R> implements AutoCloseable {
        private final int maxBatchSize;
        private final Duration linger;
        private final BlockingQueue<T> queue;
        private final ExecutorService worker = Executors.newSingleThreadExecutor();
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final java.util.function.Function<List<T>, R> batchHandler;
        private final List<R> processed = Collections.synchronizedList(new ArrayList<>());

        BatchingDispatcher(int capacity, int maxBatchSize, Duration linger, java.util.function.Function<List<T>, R> batchHandler) {
            this.queue = new ArrayBlockingQueue<>(capacity);
            this.maxBatchSize = maxBatchSize;
            this.linger = linger;
            this.batchHandler = batchHandler;
            worker.submit(this::loop);
        }

        public boolean submit(T item) {
            if (!running.get()) return false;
            return queue.offer(item); // Drop when full -> could add metrics/backpressure
        }

        public List<R> results() { return processed; }

        private void loop() {
            try {
                while (running.get() || !queue.isEmpty()) {
                    List<T> batch = new ArrayList<>(maxBatchSize);
                    T first = queue.poll(linger.toMillis(), TimeUnit.MILLISECONDS);
                    if (first != null) batch.add(first); else continue; // nothing arrived
                    queue.drainTo(batch, maxBatchSize - 1);
                    var result = batchHandler.apply(batch);
                    processed.add(result);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        @Override public void close() {
            running.set(false);
            worker.shutdown();
            try { worker.awaitTermination(1, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        }
    }

    public static void main(String[] args) throws Exception {
        try (var dispatcher = new BatchingDispatcher<String, String>(1000, 10, Duration.ofMillis(50),
                batch -> {
                    // Simulate processing cost amortization
                    try { Thread.sleep(5); } catch (InterruptedException ignored) {}
                    return "BATCH{" + batch.size() + "}";
                })) {

            // Simulate high-frequency producers
            var pool = Executors.newFixedThreadPool(4);
            for (int i = 0; i < 4; i++) {
                int id = i;
                pool.submit(() -> {
                    for (int n = 0; n < 35; n++) {
                        dispatcher.submit("P" + id + "-" + n);
                        try { Thread.sleep(ThreadLocalRandom.current().nextInt(1, 8)); } catch (InterruptedException ignored) {}
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(2, TimeUnit.SECONDS);
            Thread.sleep(300); // allow lingering flushes

            System.out.println("Batch results: " + dispatcher.results());
            System.out.println("Total batches: " + dispatcher.results().size());
        }
    }
}
