// BulkheadIsolationExample.java
// Demonstrates isolating resource pools per dependency to prevent cascading failures.
// Pattern: Use dedicated thread pools + bounded queues per external service.

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class BulkheadIsolationExample {

    static class Bulkhead {
        final ExecutorService pool;
        final Semaphore capacity;
        Bulkhead(int maxConcurrent, int poolSize) {
            this.pool = Executors.newFixedThreadPool(poolSize);
            this.capacity = new Semaphore(maxConcurrent);
        }
        <T> Future<T> submit(Supplier<T> supplier) {
            if (!capacity.tryAcquire()) {
                throw new RejectedExecutionException("Bulkhead saturated");
            }
            return pool.submit(() -> {
                try { return supplier.get(); } finally { capacity.release(); }
            });
        }
        void shutdown(){ pool.shutdownNow(); }
    }

    static class ServiceClient {
        private final Map<String, Bulkhead> bulkheads;
        ServiceClient(){
            bulkheads = Map.of(
                    "inventory", new Bulkhead(5, 5),
                    "pricing", new Bulkhead(3, 3),
                    "shipping", new Bulkhead(2, 2)
            );
        }
        String call(String service, long latencyMs) {
            var bh = bulkheads.get(service);
            try {
                return bh.submit(() -> {
                    try { Thread.sleep(latencyMs); } catch (InterruptedException ignored) {}
                    return service + ":OK";
                }).get(300, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                return service + ":TIMEOUT"; // degrade path
            } catch (RejectedExecutionException ree) {
                return service + ":REJECTED"; // fail fast due to isolation
            } catch (Exception e) {
                return service + ":ERROR";
            }
        }
        void shutdown(){ bulkheads.values().forEach(Bulkhead::shutdown); }
    }

    public static void main(String[] args) {
        var client = new ServiceClient();
        var exec = Executors.newCachedThreadPool();
        for (int i=0;i<30;i++) {
            int idx = i;
            exec.submit(() -> {
                String svc = switch (idx % 3) { case 0 -> "inventory"; case 1 -> "pricing"; default -> "shipping"; };
                long simulated = (idx % 10 == 0 && svc.equals("shipping")) ? 600 : 50 + (idx%5)*20;
                String result = client.call(svc, simulated);
                System.out.println(idx + " -> " + result);
            });
        }
        exec.shutdown();
        try { exec.awaitTermination(3, TimeUnit.SECONDS);} catch (InterruptedException ignored) {}
        client.shutdown();
    }
}
