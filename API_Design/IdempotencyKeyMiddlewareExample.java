// IdempotencyKeyMiddlewareExample.java
// NOTE: Under API_Design/ with no package for consistency.
// Demonstrates idempotent POST handling using an Idempotency-Key header with response caching.

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class IdempotencyKeyMiddlewareExample {
    static class CachedResponse { final int status; final String body; final Instant ts = Instant.now(); CachedResponse(int s,String b){status=s;body=b;} }

    static class IdempotencyStore {
        private final ConcurrentHashMap<String, CompletableFuture<CachedResponse>> store = new ConcurrentHashMap<>();
        CompletableFuture<CachedResponse> compute(String key, Callable<CachedResponse> action) {
            return store.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(() -> {
                try { return action.call(); } catch (Exception e) { throw new CompletionException(e); }
            }));
        }
    }

    static class OrderService {
        final IdempotencyStore store = new IdempotencyStore();
        CachedResponse handleCreate(String idemKey, String payload) throws Exception {
            if (idemKey == null || idemKey.isBlank()) throw new IllegalArgumentException("Missing Idempotency-Key");
            var future = store.compute(idemKey, () -> {
                // Simulate expensive creation
                try { Thread.sleep(120); } catch (InterruptedException ignored) {}
                return new CachedResponse(201, "{orderId:'"+UUID.randomUUID()+"'}");
            });
            return future.get();
        }
    }

    public static void main(String[] args) throws Exception {
        var svc = new OrderService();
        String key = "client-req-123";
        var pool = Executors.newFixedThreadPool(3);
        List<Future<CachedResponse>> calls = new ArrayList<>();
        for (int i=0;i<3;i++) {
            calls.add(pool.submit(() -> svc.handleCreate(key, "{}")));
        }
        for (var f : calls) {
            var resp = f.get();
            System.out.println(resp.status + " body=" + resp.body + " ts=" + resp.ts);
        }
        pool.shutdown();
    }
}
