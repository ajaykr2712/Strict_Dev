# Caching

## Overview
Caching is a technique used to store frequently accessed data in a temporary storage layer for faster retrieval. It is essential for improving performance, reducing latency, and scaling systems efficiently.

## Key Concepts
- **Cache:** Temporary storage for data that is expensive to compute or fetch.
- **Cache Hit:** Requested data is found in the cache.
- **Cache Miss:** Requested data is not in the cache and must be fetched from the source.
- **Eviction Policy:** Determines which data to remove when the cache is full (e.g., LRU, LFU, FIFO).

## Advanced Topics
### 1. Types of Caches
- **In-Memory Cache:** Fastest, stores data in RAM (e.g., Redis, Memcached).
- **Distributed Cache:** Shared across multiple servers for scalability and consistency.
- **Browser Cache:** Stores static assets on the client side.
- **CDN Cache:** Edge servers cache content close to users.

### 2. Cache Invalidation
- **Time-Based (TTL):** Data expires after a set period.
- **Write-Through:** Updates cache and underlying storage simultaneously.
- **Write-Back:** Updates cache first, then writes to storage asynchronously.
- **Explicit Invalidation:** Application logic removes or updates cache entries.

### 3. Consistency and Staleness
- **Strong Consistency:** Cache always reflects the latest data (hard to achieve at scale).
- **Eventual Consistency:** Cache may serve stale data for a short period.
- **Cache Stampede:** Multiple requests trigger expensive recomputation on cache miss; mitigated by locking or request coalescing.

### 4. Performance Optimization
- **Cache Aside (Lazy Loading):** Application loads data into cache on demand.
- **Read-Through:** Cache fetches data from source on miss automatically.
- **Write-Through/Write-Behind:** Controls how writes propagate to the source.

### 5. Security Considerations
- **Sensitive Data:** Avoid caching PII or confidential information unless encrypted.
- **Cache Poisoning:** Attackers inject malicious data into cache; validate all inputs.

### 6. Real-World Examples
- **Web Apps:** Cache API responses, HTML fragments, or database query results.
- **CDNs:** Cache images, videos, and static assets at edge locations.
- **Microservices:** Use distributed caches for session data and configuration.

### 7. Best Practices
- Set appropriate TTLs to balance freshness and performance.
- Monitor cache hit/miss rates and adjust strategies accordingly.
- Use consistent hashing for distributed caches.
- Document cache invalidation logic clearly.

### 8. Interview Questions
- What are the trade-offs between cache consistency and performance?
- How do you prevent cache stampede?
- Describe a scenario where caching could introduce bugs.

### 9. Diagram
```
[Client] --(Request)--> [Cache] --(Miss)--> [Database/Source]
         <--(Hit)------         <--(Response)--
```

---
Continue to the next topic for deeper mastery!