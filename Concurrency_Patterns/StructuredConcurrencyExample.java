// StructuredConcurrencyExample.java
// Demonstrates a simplified structured concurrency style in Java using virtual threads (Java 21+)
// Key ideas:
// 1. Parent scope owns child tasks.
// 2. Cancellation propagates.
// 3. Results aggregated safely.
// When to use: aggregating multiple IO-bound or mixed tasks with clear lifecycle ownership.
// Pitfalls: avoid long CPU-bound work per virtual thread; remember to handle partial failures.

import java.time.Instant;
import java.util.concurrent.*;

public class StructuredConcurrencyExample {
    record PriceQuote(String provider, double price, Instant ts) {}

    static PriceQuote fetchFromProvider(String name, double base) throws InterruptedException {
        // Simulate variable latency and occasional failure
        Thread.sleep(ThreadLocalRandom.current().nextInt(80, 300));
        if (ThreadLocalRandom.current().nextInt(10) == 0) throw new RuntimeException("Provider " + name + " failed");
        double jitter = ThreadLocalRandom.current().nextDouble(-2, 2);
        return new PriceQuote(name, base + jitter, Instant.now());
    }

    public static void main(String[] args) {
        try (var scope = new StructuredPriceScope()) {
            scope.fork(() -> fetchFromProvider("A", 101));
            scope.fork(() -> fetchFromProvider("B", 99));
            scope.fork(() -> fetchFromProvider("C", 100));
            var quotes = scope.joinAndCollectFastestSuccess(2, 500, TimeUnit.MILLISECONDS);
            quotes.forEach(q -> System.out.printf("%s => %.2f @ %s%n", q.provider(), q.price(), q.ts()));
        } catch (Exception e) {
            System.err.println("Failed to aggregate quotes: " + e.getMessage());
        }
    }

    // Minimal structured scope managing virtual threads
    static class StructuredPriceScope implements AutoCloseable {
        private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        private final ConcurrentLinkedQueue<Future<PriceQuote>> futures = new ConcurrentLinkedQueue<>();
        private volatile boolean closed = false;

        public void fork(Callable<PriceQuote> task) {
            if (closed) throw new IllegalStateException("Scope closed");
            futures.add(executor.submit(task));
        }

        public java.util.List<PriceQuote> joinAndCollectFastestSuccess(int needed, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            var results = new java.util.ArrayList<PriceQuote>();
            while (System.nanoTime() < deadline && results.size() < needed) {
                for (var f : futures) {
                    if (f.isDone()) {
                        futures.remove(f);
                        try { results.add(f.get()); } catch (ExecutionException ex) { /* ignore failed */ }
                        if (results.size() >= needed) break;
                    }
                }
                Thread.sleep(10);
            }
            if (results.size() < needed) throw new TimeoutException("Not enough successful results");
            return results;
        }

        @Override public void close() {
            closed = true;
            futures.forEach(f -> f.cancel(true));
            executor.shutdownNow();
        }
    }
}
