import java.util.*;
import java.util.concurrent.*;
import java.time.Instant;

/**
 * Google's Rate Limiting System Implementation
 * 
 * This demonstrates how Google implements rate limiting across their APIs
 * to protect services, ensure fair usage, and enforce quota limits.
 * 
 * Key Features:
 * - Token Bucket algorithm for smooth rate limiting
 * - Multiple rate limit dimensions (per-user, per-IP, per-endpoint)
 * - Distributed rate limiting with Redis simulation
 * - Graceful HTTP 429 responses with retry information
 * - Support for different subscription tiers
 * 
 * @author Google-Inspired Implementation
 */
public class RateLimitingSystemDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Google Rate Limiting System Demo ===\n");
        
        // Initialize rate limiting system
        RateLimitingSystem rateLimiter = new RateLimitingSystem();
        
        // Define rate limit policies for different tiers
        rateLimiter.addPolicy("free-tier", new RateLimitPolicy(100, 60, 20)); // 100 req/min, 20 burst
        rateLimiter.addPolicy("basic-tier", new RateLimitPolicy(1000, 60, 200));
        rateLimiter.addPolicy("pro-tier", new RateLimitPolicy(10000, 60, 2000));
        rateLimiter.addPolicy("enterprise-tier", new RateLimitPolicy(100000, 60, 20000));
        
        // Simulate various scenarios
        demonstrateBasicRateLimiting(rateLimiter);
        demonstrateBurstCapacity(rateLimiter);
        demonstrateDifferentTiers(rateLimiter);
        demonstrateMultiDimensionalLimiting(rateLimiter);
        demonstrateDDoSProtection(rateLimiter);
        demonstrateGracefulDegradation(rateLimiter);
        
        // Show analytics
        rateLimiter.printAnalytics();
    }
    
    private static void demonstrateBasicRateLimiting(RateLimitingSystem rateLimiter) {
        System.out.println("1. Basic Rate Limiting (Token Bucket Algorithm)");
        System.out.println("-----------------------------------------------");
        
        String userId = "user-123";
        String tier = "free-tier";
        
        int allowed = 0;
        int denied = 0;
        
        // Simulate 150 requests (free tier allows 100/min)
        for (int i = 0; i < 150; i++) {
            RateLimitResult result = rateLimiter.checkRateLimit(userId, tier, "GET /api/search");
            if (result.isAllowed()) {
                allowed++;
            } else {
                denied++;
            }
        }
        
        System.out.println("Requests Allowed: " + allowed);
        System.out.println("Requests Denied: " + denied);
        System.out.println("Rate Limit Hit: " + (denied > 0));
        System.out.println();
    }
    
    private static void demonstrateBurstCapacity(RateLimitingSystem rateLimiter) {
        System.out.println("2. Burst Capacity Handling");
        System.out.println("---------------------------");
        
        String userId = "user-456";
        String tier = "basic-tier";
        
        // Simulate burst of 250 requests (basic tier: 1000/min + 200 burst)
        System.out.println("Simulating burst of 250 requests...");
        
        int burstAllowed = 0;
        for (int i = 0; i < 250; i++) {
            RateLimitResult result = rateLimiter.checkRateLimit(userId, tier, "GET /api/videos");
            if (result.isAllowed()) {
                burstAllowed++;
            }
        }
        
        System.out.println("Burst Requests Allowed: " + burstAllowed);
        System.out.println("Burst capacity absorbed: " + (burstAllowed >= 200));
        System.out.println();
    }
    
    private static void demonstrateDifferentTiers(RateLimitingSystem rateLimiter) {
        System.out.println("3. Subscription Tier Enforcement");
        System.out.println("--------------------------------");
        
        Map<String, String> userTiers = Map.of(
            "free-user", "free-tier",
            "basic-user", "basic-tier",
            "pro-user", "pro-tier",
            "enterprise-user", "enterprise-tier"
        );
        
        // Test each tier with 200 requests
        int testRequests = 200;
        
        for (Map.Entry<String, String> entry : userTiers.entrySet()) {
            String user = entry.getKey();
            String tier = entry.getValue();
            
            int allowed = 0;
            for (int i = 0; i < testRequests; i++) {
                RateLimitResult result = rateLimiter.checkRateLimit(user, tier, "GET /api/maps");
                if (result.isAllowed()) allowed++;
            }
            
            System.out.printf("%s (%s): %d/%d requests allowed\n", 
                user, tier, allowed, testRequests);
        }
        System.out.println();
    }
    
    private static void demonstrateMultiDimensionalLimiting(RateLimitingSystem rateLimiter) {
        System.out.println("4. Multi-Dimensional Rate Limiting");
        System.out.println("----------------------------------");
        
        // Per-User, Per-IP, and Per-Endpoint limits
        String userId = "user-789";
        String ipAddress = "192.168.1.100";
        String endpoint = "/api/premium-feature";
        
        // Check rate limits across all dimensions
        RateLimitResult userLimit = rateLimiter.checkRateLimit(userId, "pro-tier", endpoint);
        RateLimitResult ipLimit = rateLimiter.checkRateLimitByIP(ipAddress, endpoint);
        RateLimitResult endpointLimit = rateLimiter.checkRateLimitByEndpoint(endpoint);
        
        System.out.println("User Rate Limit: " + (userLimit.isAllowed() ? "PASS" : "FAIL"));
        System.out.println("IP Rate Limit: " + (ipLimit.isAllowed() ? "PASS" : "FAIL"));
        System.out.println("Endpoint Rate Limit: " + (endpointLimit.isAllowed() ? "PASS" : "FAIL"));
        
        boolean requestAllowed = userLimit.isAllowed() && ipLimit.isAllowed() && endpointLimit.isAllowed();
        System.out.println("Overall: " + (requestAllowed ? "REQUEST ALLOWED" : "REQUEST DENIED"));
        System.out.println();
    }
    
    private static void demonstrateDDoSProtection(RateLimitingSystem rateLimiter) {
        System.out.println("5. DDoS Protection (Aggressive IP Limiting)");
        System.out.println("-------------------------------------------");
        
        String attackerIP = "203.0.113.42";
        String endpoint = "/api/expensive-operation";
        
        // Simulate DDoS attack: 10,000 requests from single IP
        int blocked = 0;
        int allowed = 0;
        
        for (int i = 0; i < 10000; i++) {
            RateLimitResult result = rateLimiter.checkRateLimitByIP(attackerIP, endpoint);
            if (result.isAllowed()) {
                allowed++;
            } else {
                blocked++;
            }
        }
        
        System.out.println("DDoS Attack Simulation:");
        System.out.println("  Total Requests: 10,000");
        System.out.println("  Requests Allowed: " + allowed);
        System.out.println("  Requests Blocked: " + blocked);
        System.out.println("  Block Rate: " + (blocked * 100.0 / 10000) + "%");
        System.out.println("  Service Protected: " + (blocked > 9000));
        System.out.println();
    }
    
    private static void demonstrateGracefulDegradation(RateLimitingSystem rateLimiter) {
        System.out.println("6. Graceful Degradation (Rate Limit Response)");
        System.out.println("---------------------------------------------");
        
        String userId = "user-999";
        String tier = "free-tier";
        
        // Exhaust rate limit
        for (int i = 0; i < 120; i++) {
            rateLimiter.checkRateLimit(userId, tier, "GET /api/data");
        }
        
        // Next request should be rate limited
        RateLimitResult result = rateLimiter.checkRateLimit(userId, tier, "GET /api/data");
        
        if (!result.isAllowed()) {
            System.out.println("HTTP Status: 429 Too Many Requests");
            System.out.println("Response Headers:");
            System.out.println("  X-RateLimit-Limit: " + result.getLimit());
            System.out.println("  X-RateLimit-Remaining: " + result.getRemaining());
            System.out.println("  X-RateLimit-Reset: " + result.getResetTime());
            System.out.println("  Retry-After: " + result.getRetryAfter() + " seconds");
            System.out.println("\nError Message: " + result.getMessage());
        }
        System.out.println();
    }
}

/**
 * Rate Limiting System - Main orchestrator
 */
class RateLimitingSystem {
    private final Map<String, RateLimitPolicy> policies = new ConcurrentHashMap<>();
    private final RateLimitStore store = new RateLimitStore();
    private final RateLimiterFactory factory = new RateLimiterFactory();
    private final AnalyticsCollector analytics = new AnalyticsCollector();
    
    // Per-IP and per-endpoint default policies
    private final RateLimitPolicy ipPolicy = new RateLimitPolicy(1000, 60, 100); // 1000/min
    private final RateLimitPolicy endpointPolicy = new RateLimitPolicy(10000, 60, 1000); // 10k/min
    
    public void addPolicy(String tierName, RateLimitPolicy policy) {
        policies.put(tierName, policy);
    }
    
    public RateLimitResult checkRateLimit(String userId, String tier, String endpoint) {
        RateLimitPolicy policy = policies.get(tier);
        if (policy == null) {
            return new RateLimitResult(false, 0, 0, 0, 0, 
                "Unknown subscription tier");
        }
        
        String key = "user:" + userId + ":" + tier;
        RateLimiter limiter = factory.createTokenBucketLimiter(policy);
        
        RateLimitResult result = limiter.tryAcquire(key, store);
        
        // Track analytics
        if (result.isAllowed()) {
            analytics.recordAllowed(userId, tier, endpoint);
        } else {
            analytics.recordDenied(userId, tier, endpoint);
        }
        
        return result;
    }
    
    public RateLimitResult checkRateLimitByIP(String ipAddress, String endpoint) {
        String key = "ip:" + ipAddress;
        RateLimiter limiter = factory.createTokenBucketLimiter(ipPolicy);
        
        RateLimitResult result = limiter.tryAcquire(key, store);
        
        if (!result.isAllowed()) {
            analytics.recordIPBlocked(ipAddress, endpoint);
        }
        
        return result;
    }
    
    public RateLimitResult checkRateLimitByEndpoint(String endpoint) {
        String key = "endpoint:" + endpoint;
        RateLimiter limiter = factory.createTokenBucketLimiter(endpointPolicy);
        
        return limiter.tryAcquire(key, store);
    }
    
    public void printAnalytics() {
        analytics.printSummary();
    }
}

/**
 * Rate Limit Policy - Defines limits for a tier
 */
class RateLimitPolicy {
    private final int maxRequests;
    private final int windowSeconds;
    private final int burstCapacity;
    
    public RateLimitPolicy(int maxRequests, int windowSeconds, int burstCapacity) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.burstCapacity = burstCapacity;
    }
    
    public int getMaxRequests() { return maxRequests; }
    public int getWindowSeconds() { return windowSeconds; }
    public int getBurstCapacity() { return burstCapacity; }
    public double getRefillRate() { return (double) maxRequests / windowSeconds; }
}

/**
 * Rate Limiter Interface
 */
interface RateLimiter {
    RateLimitResult tryAcquire(String key, RateLimitStore store);
}

/**
 * Token Bucket Rate Limiter - Most flexible algorithm
 */
class TokenBucketRateLimiter implements RateLimiter {
    private final RateLimitPolicy policy;
    
    public TokenBucketRateLimiter(RateLimitPolicy policy) {
        this.policy = policy;
    }
    
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitStore store) {
        TokenBucket bucket = store.getTokenBucket(key);
        
        if (bucket == null) {
            // Create new bucket
            bucket = new TokenBucket(
                policy.getMaxRequests() + policy.getBurstCapacity(),
                policy.getMaxRequests() + policy.getBurstCapacity(),
                policy.getRefillRate(),
                System.currentTimeMillis()
            );
            store.putTokenBucket(key, bucket);
        }
        
        // Refill tokens based on time elapsed
        long now = System.currentTimeMillis();
        long elapsedMs = now - bucket.getLastRefillTime();
        double tokensToAdd = (elapsedMs / 1000.0) * policy.getRefillRate();
        
        int newTokens = Math.min(
            (int) (bucket.getTokens() + tokensToAdd),
            policy.getMaxRequests() + policy.getBurstCapacity()
        );
        
        if (newTokens >= 1) {
            // Allow request
            bucket.setTokens(newTokens - 1);
            bucket.setLastRefillTime(now);
            store.putTokenBucket(key, bucket);
            
            return new RateLimitResult(
                true,
                policy.getMaxRequests(),
                newTokens - 1,
                calculateResetTime(bucket),
                0,
                "Request allowed"
            );
        } else {
            // Deny request
            int retryAfter = calculateRetryAfter(bucket);
            
            return new RateLimitResult(
                false,
                policy.getMaxRequests(),
                0,
                calculateResetTime(bucket),
                retryAfter,
                "Rate limit exceeded. Please try again in " + retryAfter + " seconds."
            );
        }
    }
    
    private long calculateResetTime(TokenBucket bucket) {
        long timeToFullRefill = (long) ((policy.getMaxRequests() / policy.getRefillRate()) * 1000);
        return bucket.getLastRefillTime() + timeToFullRefill;
    }
    
    private int calculateRetryAfter(TokenBucket bucket) {
        // Time needed to refill 1 token
        return (int) Math.ceil(1.0 / policy.getRefillRate());
    }
}

/**
 * Token Bucket - Holds current state
 */
class TokenBucket {
    private int tokens;
    private final int capacity;
    private final double refillRate;
    private long lastRefillTime;
    
    public TokenBucket(int tokens, int capacity, double refillRate, long lastRefillTime) {
        this.tokens = tokens;
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.lastRefillTime = lastRefillTime;
    }
    
    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    public int getCapacity() { return capacity; }
    public double getRefillRate() { return refillRate; }
    public long getLastRefillTime() { return lastRefillTime; }
    public void setLastRefillTime(long time) { this.lastRefillTime = time; }
}

/**
 * Rate Limit Store - Simulates Redis storage
 */
class RateLimitStore {
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    public TokenBucket getTokenBucket(String key) {
        return buckets.get(key);
    }
    
    public void putTokenBucket(String key, TokenBucket bucket) {
        buckets.put(key, bucket);
    }
    
    public void remove(String key) {
        buckets.remove(key);
    }
    
    public int size() {
        return buckets.size();
    }
}

/**
 * Rate Limiter Factory
 */
class RateLimiterFactory {
    public RateLimiter createTokenBucketLimiter(RateLimitPolicy policy) {
        return new TokenBucketRateLimiter(policy);
    }
    
    // Could add other algorithms here:
    // - createLeakyBucketLimiter()
    // - createFixedWindowLimiter()
    // - createSlidingWindowLimiter()
}

/**
 * Rate Limit Result - Response from rate limiter
 */
class RateLimitResult {
    private final boolean allowed;
    private final int limit;
    private final int remaining;
    private final long resetTime;
    private final int retryAfter;
    private final String message;
    
    public RateLimitResult(boolean allowed, int limit, int remaining, 
                          long resetTime, int retryAfter, String message) {
        this.allowed = allowed;
        this.limit = limit;
        this.remaining = remaining;
        this.resetTime = resetTime;
        this.retryAfter = retryAfter;
        this.message = message;
    }
    
    public boolean isAllowed() { return allowed; }
    public int getLimit() { return limit; }
    public int getRemaining() { return remaining; }
    public long getResetTime() { return resetTime; }
    public int getRetryAfter() { return retryAfter; }
    public String getMessage() { return message; }
}

/**
 * Analytics Collector - Tracks rate limiting metrics
 */
class AnalyticsCollector {
    private int totalAllowed = 0;
    private int totalDenied = 0;
    private final Map<String, Integer> deniedByTier = new ConcurrentHashMap<>();
    private final Map<String, Integer> deniedByEndpoint = new ConcurrentHashMap<>();
    private final Set<String> blockedIPs = ConcurrentHashMap.newKeySet();
    
    public void recordAllowed(String userId, String tier, String endpoint) {
        totalAllowed++;
    }
    
    public void recordDenied(String userId, String tier, String endpoint) {
        totalDenied++;
        deniedByTier.merge(tier, 1, Integer::sum);
        deniedByEndpoint.merge(endpoint, 1, Integer::sum);
    }
    
    public void recordIPBlocked(String ipAddress, String endpoint) {
        blockedIPs.add(ipAddress);
    }
    
    public void printSummary() {
        System.out.println("=== Rate Limiting Analytics ===");
        System.out.println("Total Requests Allowed: " + totalAllowed);
        System.out.println("Total Requests Denied: " + totalDenied);
        
        int total = totalAllowed + totalDenied;
        if (total > 0) {
            double blockRate = (totalDenied * 100.0) / total;
            System.out.printf("Block Rate: %.2f%%\n", blockRate);
        }
        
        System.out.println("\nDenials by Tier:");
        deniedByTier.forEach((tier, count) -> 
            System.out.println("  " + tier + ": " + count));
        
        System.out.println("\nDenials by Endpoint:");
        deniedByEndpoint.forEach((endpoint, count) -> 
            System.out.println("  " + endpoint + ": " + count));
        
        System.out.println("\nBlocked IPs: " + blockedIPs.size());
        blockedIPs.forEach(ip -> System.out.println("  " + ip));
    }
}
