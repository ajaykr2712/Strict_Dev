# Advanced Caching Strategies for Distributed Systems

This document covers sophisticated caching strategies used by major tech companies like Netflix, Uber, and WhatsApp to achieve high performance and scalability.

## 1. Multi-Level Caching Architecture

### L1 Cache - Application Memory
**In-Memory Caching with Caffeine (Netflix Example)**

```java
// Netflix content metadata caching
@Configuration
public class CacheConfiguration {
    
    @Bean
    public Cache<String, MovieMetadata> movieCache() {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats()
            .build();
    }
}

@Service
public class MovieService {
    
    @Autowired
    private Cache<String, MovieMetadata> movieCache;
    
    public MovieMetadata getMovieMetadata(String movieId) {
        return movieCache.get(movieId, id -> 
            databaseService.fetchMovieMetadata(id)
        );
    }
}
```

**Key Features:**
- Nanosecond access times
- Limited by JVM heap size
- Automatic eviction policies (LRU, LFU, Time-based)
- Statistical monitoring

### L2 Cache - Distributed Cache (Redis)
**Redis Cluster for High Availability**

```java
// Uber ride matching cache
@Component
public class RideMatchingCache {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String DRIVER_LOCATION_KEY = "driver:location:";
    private static final String RIDE_REQUEST_KEY = "ride:request:";
    
    // Cache driver locations with TTL
    public void cacheDriverLocation(String driverId, DriverLocation location) {
        String key = DRIVER_LOCATION_KEY + driverId;
        redisTemplate.opsForValue().set(key, location, 
            Duration.ofSeconds(30)); // 30-second TTL
    }
    
    // Geospatial queries for nearby drivers
    public Set<DriverLocation> getNearbyDrivers(double latitude, double longitude, 
                                               double radiusKm) {
        return redisTemplate.opsForGeo()
            .radius("drivers:geo", latitude, longitude, radiusKm);
    }
    
    // Pipeline operations for bulk updates
    public void bulkUpdateDriverLocations(Map<String, DriverLocation> locations) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            locations.forEach((driverId, location) -> {
                String key = DRIVER_LOCATION_KEY + driverId;
                connection.setEx(key.getBytes(), 30, 
                    serialize(location));
            });
            return null;
        });
    }
}
```

### L3 Cache - Database Query Cache
**WhatsApp Message Delivery Cache**

```java
// Message delivery status caching
@Repository
public class MessageRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "messageStatus", key = "#messageId")
    public MessageStatus getMessageStatus(String messageId) {
        return jdbcTemplate.queryForObject(
            "SELECT status, delivered_at FROM messages WHERE id = ?",
            new Object[]{messageId},
            new MessageStatusRowMapper()
        );
    }
    
    @CacheEvict(value = "messageStatus", key = "#messageId")
    public void updateMessageStatus(String messageId, MessageStatus status) {
        jdbcTemplate.update(
            "UPDATE messages SET status = ?, delivered_at = ? WHERE id = ?",
            status.getStatus(), status.getDeliveredAt(), messageId
        );
    }
}
```

## 2. Cache Patterns and Strategies

### Cache-Aside (Lazy Loading)
**Netflix Recommendation Cache**

```java
@Service
public class RecommendationService {
    
    @Autowired
    private RecommendationEngine engine;
    
    @Autowired
    private RedisTemplate<String, Object> cache;
    
    public List<Movie> getRecommendations(String userId) {
        String cacheKey = "recommendations:" + userId;
        
        // Try cache first
        List<Movie> cached = (List<Movie>) cache.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Cache miss - compute recommendations
        List<Movie> recommendations = engine.computeRecommendations(userId);
        
        // Store in cache with expiration
        cache.opsForValue().set(cacheKey, recommendations, 
            Duration.ofHours(2));
        
        return recommendations;
    }
}
```

### Write-Through Cache
**Uber Driver Profile Updates**

```java
@Service
public class DriverProfileService {
    
    @Autowired
    private DriverRepository repository;
    
    @Autowired
    private RedisTemplate<String, Object> cache;
    
    @Transactional
    public void updateDriverProfile(String driverId, DriverProfile profile) {
        // Write to database first
        repository.updateProfile(driverId, profile);
        
        // Then update cache
        String cacheKey = "driver:profile:" + driverId;
        cache.opsForValue().set(cacheKey, profile, 
            Duration.ofHours(24));
        
        // Invalidate related caches
        cache.delete("driver:search:*");
    }
}
```

### Write-Behind (Write-Back) Cache
**WhatsApp Message Batching**

```java
@Component
public class MessageBatchProcessor {
    
    private final Map<String, List<Message>> batchBuffer = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(2);
    
    @PostConstruct
    public void startBatchProcessing() {
        scheduler.scheduleAtFixedRate(this::flushBatches, 
            0, 5, TimeUnit.SECONDS);
    }
    
    public void bufferMessage(Message message) {
        String chatId = message.getChatId();
        batchBuffer.computeIfAbsent(chatId, k -> new ArrayList<>())
                   .add(message);
        
        // Immediate cache update
        updateCacheWithMessage(message);
    }
    
    private void flushBatches() {
        batchBuffer.forEach((chatId, messages) -> {
            if (!messages.isEmpty()) {
                // Bulk insert to database
                messageRepository.batchInsert(messages);
                messages.clear();
            }
        });
    }
}
```

## 3. Cache Invalidation Strategies

### Time-Based Expiration (TTL)
```java
// Netflix content popularity cache
@Service
public class ContentPopularityService {
    
    @Autowired
    private RedisTemplate<String, Object> cache;
    
    public void updatePopularityScore(String contentId, double score) {
        String key = "popularity:" + contentId;
        
        // Short TTL for trending content
        Duration ttl = score > 8.0 ? 
            Duration.ofMinutes(5) :   // Hot content
            Duration.ofHours(1);      // Regular content
            
        cache.opsForValue().set(key, score, ttl);
    }
}
```

### Event-Based Invalidation
```java
// Uber surge pricing cache invalidation
@EventListener
public class SurgePricingCacheHandler {
    
    @Autowired
    private RedisTemplate<String, Object> cache;
    
    @EventListener
    public void handleDemandChange(DemandChangeEvent event) {
        String region = event.getRegionId();
        
        // Invalidate affected cache keys
        Set<String> keys = cache.keys("surge:pricing:" + region + ":*");
        if (!keys.isEmpty()) {
            cache.delete(keys);
        }
        
        // Pre-warm with new pricing
        prewarmSurgePricing(region);
    }
}
```

### Version-Based Cache Tags
```java
// WhatsApp group chat cache with versioning
@Service
public class GroupChatService {
    
    @Autowired
    private RedisTemplate<String, Object> cache;
    
    public GroupChat getGroupChat(String groupId) {
        String versionKey = "group:version:" + groupId;
        String dataKey = "group:data:" + groupId;
        
        Long currentVersion = cache.opsForValue().get(versionKey);
        CachedGroupChat cached = cache.opsForValue().get(dataKey);
        
        if (cached != null && cached.getVersion().equals(currentVersion)) {
            return cached.getGroupChat();
        }
        
        // Cache miss or version mismatch
        GroupChat groupChat = loadGroupChatFromDatabase(groupId);
        Long newVersion = System.currentTimeMillis();
        
        cache.opsForValue().set(versionKey, newVersion);
        cache.opsForValue().set(dataKey, 
            new CachedGroupChat(groupChat, newVersion));
        
        return groupChat;
    }
}
```

## 4. Advanced Cache Optimization Techniques

### Cache Warming Strategies
```java
// Netflix content pre-loading
@Component
public class ContentCacheWarmer {
    
    @Autowired
    private ContentService contentService;
    
    @Autowired
    private AnalyticsService analytics;
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void warmPopularContent() {
        // Get trending content IDs
        List<String> trendingIds = analytics.getTrendingContentIds(100);
        
        // Pre-load in parallel
        trendingIds.parallelStream().forEach(contentId -> {
            try {
                contentService.getContentMetadata(contentId);
                contentService.getContentRecommendations(contentId);
            } catch (Exception e) {
                log.warn("Failed to warm cache for content: " + contentId, e);
            }
        });
    }
}
```

### Probabilistic Cache Refresh
```java
// Uber driver availability with probabilistic refresh
@Service
public class DriverAvailabilityService {
    
    private static final double REFRESH_PROBABILITY = 0.1; // 10%
    private final Random random = new Random();
    
    public boolean isDriverAvailable(String driverId) {
        String cacheKey = "driver:available:" + driverId;
        Boolean cached = cache.opsForValue().get(cacheKey);
        
        if (cached != null) {
            // Probabilistic refresh for popular items
            if (random.nextDouble() < REFRESH_PROBABILITY) {
                CompletableFuture.runAsync(() -> refreshDriverStatus(driverId));
            }
            return cached;
        }
        
        // Cache miss - load and cache
        boolean available = checkDriverAvailabilityFromSource(driverId);
        cache.opsForValue().set(cacheKey, available, Duration.ofMinutes(2));
        return available;
    }
}
```

### Cache Compression
```java
// WhatsApp message history compression
@Configuration
public class CacheCompressionConfig {
    
    @Bean
    public RedisTemplate<String, Object> compressedRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        
        // Custom serializer with compression
        template.setValueSerializer(new CompressedJsonRedisSerializer());
        return template;
    }
}

public class CompressedJsonRedisSerializer implements RedisSerializer<Object> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        try {
            byte[] json = objectMapper.writeValueAsBytes(obj);
            return compress(json);
        } catch (Exception e) {
            throw new SerializationException("Compression failed", e);
        }
    }
    
    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        try {
            byte[] decompressed = decompress(bytes);
            return objectMapper.readValue(decompressed, Object.class);
        } catch (Exception e) {
            throw new SerializationException("Decompression failed", e);
        }
    }
    
    private byte[] compress(byte[] data) {
        // Implement GZip compression
        // Returns compressed data
    }
    
    private byte[] decompress(byte[] data) {
        // Implement GZip decompression
        // Returns decompressed data
    }
}
```

## 5. Cache Monitoring and Metrics

### Performance Metrics
```java
@Component
public class CacheMetricsCollector {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void handleCacheHit(CacheHitEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("cache.access")
            .tag("result", "hit")
            .tag("cache", event.getCacheName())
            .register(meterRegistry));
    }
    
    @EventListener
    public void handleCacheMiss(CacheMissEvent event) {
        meterRegistry.counter("cache.miss", 
            "cache", event.getCacheName()).increment();
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void collectCacheStats() {
        // Collect cache hit ratios, sizes, eviction counts
        cacheManager.getCaches().forEach(cache -> {
            CacheStats stats = cache.getStats();
            
            Gauge.builder("cache.hit.ratio")
                .tag("cache", cache.getName())
                .register(meterRegistry, stats, CacheStats::hitRate);
                
            Gauge.builder("cache.size")
                .tag("cache", cache.getName())
                .register(meterRegistry, stats, CacheStats::estimatedSize);
        });
    }
}
```

## 6. Cache Anti-Patterns to Avoid

### Cache Stampede Prevention
```java
// Prevent cache stampede with locking
@Service
public class AntiStampedeService {
    
    private final Map<String, CompletableFuture<Object>> ongoingLoads = 
        new ConcurrentHashMap<>();
    
    public CompletableFuture<Object> getWithLock(String key) {
        return ongoingLoads.computeIfAbsent(key, k -> 
            CompletableFuture.supplyAsync(() -> {
                try {
                    return loadDataFromSource(k);
                } finally {
                    ongoingLoads.remove(k);
                }
            })
        );
    }
}
```

### Hot Key Management
```java
// Distribute hot keys across multiple cache nodes
@Service
public class HotKeyDistributionService {
    
    public void setHotKey(String key, Object value) {
        // Distribute hot keys across multiple slots
        for (int i = 0; i < 3; i++) {
            String distributedKey = key + ":shard:" + i;
            cache.opsForValue().set(distributedKey, value);
        }
    }
    
    public Object getHotKey(String key) {
        // Randomly select a shard to balance load
        int shard = ThreadLocalRandom.current().nextInt(3);
        String distributedKey = key + ":shard:" + shard;
        return cache.opsForValue().get(distributedKey);
    }
}
```

## 7. Cache Strategy Selection Guide

| Use Case | Pattern | TTL | Technology |
|----------|---------|-----|------------|
| User Sessions | Cache-Aside | 30min - 2h | Redis |
| Product Catalog | Write-Through | 24h | Redis + DB |
| Real-time Analytics | Write-Behind | 5min | Redis |
| Static Content | CDN | 7-30 days | CloudFront/CloudFlare |
| Database Query Results | Query Cache | 1-5min | Application Cache |
| API Rate Limiting | Sliding Window | 1min | Redis |

## Best Practices Summary

1. **Layer Your Caches**: Use multiple cache levels for optimal performance
2. **Monitor Cache Health**: Track hit ratios, latency, and eviction rates
3. **Plan for Cache Failures**: Implement graceful degradation
4. **Size Appropriately**: Balance memory usage with hit ratios
5. **Choose Eviction Policies Wisely**: LRU for general use, LFU for stable workloads
6. **Implement Circuit Breakers**: Protect against cache service failures
7. **Use Consistent Hashing**: For distributed cache scaling
8. **Compress Large Objects**: Reduce memory usage and network overhead
