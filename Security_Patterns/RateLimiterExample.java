import java.util.*;
import java.util.concurrent.*;

/**
 * Rate Limiter Implementation
 * 
 * Real-world Use Case: API Rate Limiting for WhatsApp Business API
 * Prevents abuse and ensures fair usage of messaging services
 */

public class RateLimiterExample {
    
    // Rate limiter interface
    interface RateLimiter {
        boolean allowRequest(String clientId);
        boolean allowRequest(String clientId, int tokens);
        void reset(String clientId);
        RateLimitInfo getInfo(String clientId);
        String getAlgorithm();
    }
    
    // Rate limit information
    static class RateLimitInfo {
        private final int limit;
        private final int remaining;
        private final long resetTime;
        private final long windowSize;
        
        public RateLimitInfo(int limit, int remaining, long resetTime, long windowSize) {
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
            this.windowSize = windowSize;
        }
        
        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
        public long getResetTime() { return resetTime; }
        public long getWindowSize() { return windowSize; }
        
        @Override
        public String toString() {
            return String.format("Limit: %d, Remaining: %d, Reset: %d, Window: %ds", 
                    limit, remaining, resetTime, windowSize / 1000);
        }
    }
    
    // Token Bucket Rate Limiter
    static class TokenBucketRateLimiter implements RateLimiter {
        private final int capacity;
        private final int refillRate; // tokens per second
        private final Map<String, TokenBucket> buckets;
        
        public TokenBucketRateLimiter(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.buckets = new ConcurrentHashMap<>();
        }
        
        @Override
        public boolean allowRequest(String clientId) {
            return allowRequest(clientId, 1);
        }
        
        @Override
        public boolean allowRequest(String clientId, int tokens) {
            TokenBucket bucket = buckets.computeIfAbsent(clientId, 
                    k -> new TokenBucket(capacity, refillRate));
            return bucket.consume(tokens);
        }
        
        @Override
        public void reset(String clientId) {
            buckets.remove(clientId);
        }
        
        @Override
        public RateLimitInfo getInfo(String clientId) {
            TokenBucket bucket = buckets.get(clientId);
            if (bucket == null) {
                return new RateLimitInfo(capacity, capacity, 0, 0);
            }
            return bucket.getInfo();
        }
        
        @Override
        public String getAlgorithm() {
            return "Token Bucket";
        }
        
        private static class TokenBucket {
            private final int capacity;
            private final int refillRate;
            private int tokens;
            private long lastRefillTime;
            
            public TokenBucket(int capacity, int refillRate) {
                this.capacity = capacity;
                this.refillRate = refillRate;
                this.tokens = capacity;
                this.lastRefillTime = System.currentTimeMillis();
            }
            
            public synchronized boolean consume(int requestedTokens) {
                refill();
                if (tokens >= requestedTokens) {
                    tokens -= requestedTokens;
                    return true;
                }
                return false;
            }
            
            private void refill() {
                long now = System.currentTimeMillis();
                long timePassed = now - lastRefillTime;
                int tokensToAdd = (int) (timePassed * refillRate / 1000);
                
                if (tokensToAdd > 0) {
                    tokens = Math.min(capacity, tokens + tokensToAdd);
                    lastRefillTime = now;
                }
            }
            
            public RateLimitInfo getInfo() {
                refill();
                long nextRefillTime = lastRefillTime + (1000 / refillRate);
                return new RateLimitInfo(capacity, tokens, nextRefillTime, 1000);
            }
        }
    }
    
    // Fixed Window Rate Limiter
    static class FixedWindowRateLimiter implements RateLimiter {
        private final int limit;
        private final long windowSizeMs;
        private final Map<String, WindowCounter> windows;
        
        public FixedWindowRateLimiter(int limit, long windowSizeMs) {
            this.limit = limit;
            this.windowSizeMs = windowSizeMs;
            this.windows = new ConcurrentHashMap<>();
        }
        
        @Override
        public boolean allowRequest(String clientId) {
            return allowRequest(clientId, 1);
        }
        
        @Override
        public boolean allowRequest(String clientId, int tokens) {
            WindowCounter window = windows.computeIfAbsent(clientId, 
                    k -> new WindowCounter(limit, windowSizeMs));
            return window.allowRequest(tokens);
        }
        
        @Override
        public void reset(String clientId) {
            windows.remove(clientId);
        }
        
        @Override
        public RateLimitInfo getInfo(String clientId) {
            WindowCounter window = windows.get(clientId);
            if (window == null) {
                return new RateLimitInfo(limit, limit, 0, windowSizeMs);
            }
            return window.getInfo();
        }
        
        @Override
        public String getAlgorithm() {
            return "Fixed Window";
        }
        
        private static class WindowCounter {
            private final int limit;
            private final long windowSizeMs;
            private int count;
            private long windowStart;
            
            public WindowCounter(int limit, long windowSizeMs) {
                this.limit = limit;
                this.windowSizeMs = windowSizeMs;
                this.count = 0;
                this.windowStart = System.currentTimeMillis();
            }
            
            public synchronized boolean allowRequest(int tokens) {
                long now = System.currentTimeMillis();
                
                // Check if we need to reset the window
                if (now - windowStart >= windowSizeMs) {
                    count = 0;
                    windowStart = now;
                }
                
                if (count + tokens <= limit) {
                    count += tokens;
                    return true;
                }
                return false;
            }
            
            public RateLimitInfo getInfo() {
                long now = System.currentTimeMillis();
                if (now - windowStart >= windowSizeMs) {
                    return new RateLimitInfo(limit, limit, windowStart + windowSizeMs, windowSizeMs);
                }
                return new RateLimitInfo(limit, limit - count, windowStart + windowSizeMs, windowSizeMs);
            }
        }
    }
    
    // Sliding Window Log Rate Limiter
    static class SlidingWindowLogRateLimiter implements RateLimiter {
        private final int limit;
        private final long windowSizeMs;
        private final Map<String, Queue<Long>> requestLogs;
        
        public SlidingWindowLogRateLimiter(int limit, long windowSizeMs) {
            this.limit = limit;
            this.windowSizeMs = windowSizeMs;
            this.requestLogs = new ConcurrentHashMap<>();
        }
        
        @Override
        public boolean allowRequest(String clientId) {
            return allowRequest(clientId, 1);
        }
        
        @Override
        public boolean allowRequest(String clientId, int tokens) {
            Queue<Long> log = requestLogs.computeIfAbsent(clientId, k -> new ConcurrentLinkedQueue<>());
            
            synchronized (log) {
                long now = System.currentTimeMillis();
                
                // Remove old entries outside the window
                while (!log.isEmpty() && now - log.peek() > windowSizeMs) {
                    log.poll();
                }
                
                // Check if we can allow the request
                if (log.size() + tokens <= limit) {
                    for (int i = 0; i < tokens; i++) {
                        log.offer(now);
                    }
                    return true;
                }
                return false;
            }
        }
        
        @Override
        public void reset(String clientId) {
            requestLogs.remove(clientId);
        }
        
        @Override
        public RateLimitInfo getInfo(String clientId) {
            Queue<Long> log = requestLogs.get(clientId);
            if (log == null) {
                return new RateLimitInfo(limit, limit, 0, windowSizeMs);
            }
            
            synchronized (log) {
                long now = System.currentTimeMillis();
                
                // Clean old entries
                while (!log.isEmpty() && now - log.peek() > windowSizeMs) {
                    log.poll();
                }
                
                long resetTime = log.isEmpty() ? 0 : log.peek() + windowSizeMs;
                return new RateLimitInfo(limit, limit - log.size(), resetTime, windowSizeMs);
            }
        }
        
        @Override
        public String getAlgorithm() {
            return "Sliding Window Log";
        }
    }
    
    // Rate Limiter Manager
    static class WhatsAppRateLimiterManager {
        private final Map<String, RateLimiter> rateLimiters;
        private final Map<String, String> clientTiers;
        
        public WhatsAppRateLimiterManager() {
            this.rateLimiters = new HashMap<>();
            this.clientTiers = new ConcurrentHashMap<>();
            setupRateLimiters();
        }
        
        private void setupRateLimiters() {
            // Different rate limiters for different tiers
            rateLimiters.put("FREE", new FixedWindowRateLimiter(100, 60000)); // 100 requests per minute
            rateLimiters.put("BASIC", new TokenBucketRateLimiter(1000, 10)); // 1000 capacity, 10 tokens/sec
            rateLimiters.put("PREMIUM", new SlidingWindowLogRateLimiter(5000, 60000)); // 5000 requests per minute
            rateLimiters.put("ENTERPRISE", new TokenBucketRateLimiter(10000, 100)); // 10000 capacity, 100 tokens/sec
        }
        
        public void assignTier(String clientId, String tier) {
            clientTiers.put(clientId, tier);
            System.out.println("[RATE-LIMITER] Assigned " + clientId + " to " + tier + " tier");
        }
        
        public boolean allowRequest(String clientId) {
            return allowRequest(clientId, 1);
        }
        
        public boolean allowRequest(String clientId, int tokens) {
            String tier = clientTiers.getOrDefault(clientId, "FREE");
            RateLimiter rateLimiter = rateLimiters.get(tier);
            
            if (rateLimiter == null) {
                System.out.println("[RATE-LIMITER] No rate limiter for tier: " + tier);
                return false;
            }
            
            boolean allowed = rateLimiter.allowRequest(clientId, tokens);
            
            if (!allowed) {
                System.out.println("[RATE-LIMITER] Request denied for " + clientId + 
                                 " (" + tier + " tier, " + rateLimiter.getAlgorithm() + ")");
            }
            
            return allowed;
        }
        
        public void showClientStatus(String clientId) {
            String tier = clientTiers.getOrDefault(clientId, "FREE");
            RateLimiter rateLimiter = rateLimiters.get(tier);
            
            if (rateLimiter != null) {
                RateLimitInfo info = rateLimiter.getInfo(clientId);
                System.out.println("[STATUS] " + clientId + " (" + tier + " - " + 
                                 rateLimiter.getAlgorithm() + "): " + info);
            }
        }
        
        public void resetClient(String clientId) {
            String tier = clientTiers.getOrDefault(clientId, "FREE");
            RateLimiter rateLimiter = rateLimiters.get(tier);
            
            if (rateLimiter != null) {
                rateLimiter.reset(clientId);
                System.out.println("[RATE-LIMITER] Reset rate limit for " + clientId);
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Rate Limiter Demo: WhatsApp Business API ===\n");
        
        WhatsAppRateLimiterManager manager = new WhatsAppRateLimiterManager();
        
        // Assign different tiers to clients
        manager.assignTier("client_free", "FREE");
        manager.assignTier("client_basic", "BASIC");
        manager.assignTier("client_premium", "PREMIUM");
        manager.assignTier("client_enterprise", "ENTERPRISE");
        
        System.out.println("\n1. Testing different tier limits:");
        
        // Test FREE tier (Fixed Window - 100 per minute)
        System.out.println("\nTesting FREE tier client:");
        for (int i = 1; i <= 5; i++) {
            boolean allowed = manager.allowRequest("client_free");
            System.out.println("Request " + i + ": " + (allowed ? "ALLOWED" : "DENIED"));
        }
        manager.showClientStatus("client_free");
        
        // Test BASIC tier (Token Bucket - 1000 capacity, 10/sec)
        System.out.println("\nTesting BASIC tier client:");
        for (int i = 1; i <= 5; i++) {
            boolean allowed = manager.allowRequest("client_basic", 5); // 5 tokens per request
            System.out.println("Bulk request " + i + " (5 tokens): " + (allowed ? "ALLOWED" : "DENIED"));
        }
        manager.showClientStatus("client_basic");
        
        // Test PREMIUM tier (Sliding Window Log - 5000 per minute)
        System.out.println("\nTesting PREMIUM tier client:");
        for (int i = 1; i <= 10; i++) {
            boolean allowed = manager.allowRequest("client_premium");
            System.out.println("Request " + i + ": " + (allowed ? "ALLOWED" : "DENIED"));
        }
        manager.showClientStatus("client_premium");
        
        // Simulate rate limit exhaustion
        System.out.println("\n2. Simulating rate limit exhaustion (FREE tier):");
        for (int i = 1; i <= 105; i++) {
            boolean allowed = manager.allowRequest("client_free");
            if (i % 20 == 0 || !allowed) {
                System.out.println("Request " + i + ": " + (allowed ? "ALLOWED" : "DENIED"));
            }
        }
        manager.showClientStatus("client_free");
        
        // Test burst handling with token bucket
        System.out.println("\n3. Testing burst handling (BASIC tier):");
        System.out.println("Making 20 rapid requests...");
        int allowed = 0;
        for (int i = 1; i <= 20; i++) {
            if (manager.allowRequest("client_basic")) {
                allowed++;
            }
        }
        System.out.println("Allowed " + allowed + " out of 20 requests");
        manager.showClientStatus("client_basic");
        
        // Wait and test token refill
        System.out.println("\nWaiting 2 seconds for token refill...");
        Thread.sleep(2000);
        
        System.out.println("Testing after refill:");
        for (int i = 1; i <= 5; i++) {
            boolean success = manager.allowRequest("client_basic");
            System.out.println("Request " + i + ": " + (success ? "ALLOWED" : "DENIED"));
        }
        manager.showClientStatus("client_basic");
        
        // Test reset functionality
        System.out.println("\n4. Testing reset functionality:");
        manager.resetClient("client_free");
        manager.showClientStatus("client_free");
        
        System.out.println("\n=== Rate Limiter Benefits ===");
        System.out.println("✓ Prevents API abuse and ensures fair usage");
        System.out.println("✓ Different algorithms for different use cases");
        System.out.println("✓ Tiered limits based on client subscription");
        System.out.println("✓ Protects backend services from overload");
        System.out.println("✓ Provides clear feedback to clients about limits");
    }
}
