# Rate Limiting

## Overview
Rate limiting is a technique used to control the number of requests a user or system can make to a resource within a specified time window. It protects services from abuse, ensures fair usage, and helps maintain system stability under high load.

## Key Concepts
- **Request Quota:** Maximum number of requests allowed per user/IP in a given period.
- **Time Window:** The interval (e.g., per second, minute, hour) over which requests are counted.
- **Throttling:** Temporarily slowing or blocking requests that exceed the limit.
- **Burst Capacity:** Allows short spikes above the steady rate, smoothing traffic.

## Advanced Topics
### 1. Algorithms
- **Token Bucket:** Tokens are added at a fixed rate; each request consumes a token. Allows bursts up to bucket size.
- **Leaky Bucket:** Requests are processed at a constant rate; excess requests are queued or dropped.
- **Fixed Window:** Counts requests in discrete intervals (e.g., per minute).
- **Sliding Window:** Uses overlapping intervals for smoother enforcement.

### 2. Distributed Rate Limiting
- **Centralized Store:** Use Redis or Memcached to track counters across multiple servers.
- **Consistent Hashing:** Distribute counters to reduce contention and improve scalability.
- **Synchronization:** Ensure atomic updates to prevent race conditions.

### 3. Real-World Example
- APIs limit requests per API key to prevent abuse.
- Login endpoints restrict attempts to mitigate brute-force attacks.
- E-commerce sites throttle checkout requests during flash sales.

### 4. Best Practices
- Set limits based on user type (anonymous, authenticated, premium).
- Return clear error messages (e.g., HTTP 429 Too Many Requests) with retry-after headers.
- Monitor and log rate limit violations for security and analytics.
- Allow for whitelisting or dynamic adjustment of limits.

### 5. Interview Questions
- Explain the difference between token bucket and leaky bucket algorithms.
- How would you implement distributed rate limiting?
- What are the trade-offs between fixed and sliding window algorithms?

### 6. Diagram
```
[Client] -> [Rate Limiter] -> [Service]
```

---
Rate limiting is essential for protecting APIs and services from abuse, ensuring reliability and fairness at scale.