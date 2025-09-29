# Cache Stampede Mitigation Strategies

Cache stampede (dogpile effect) occurs when many workers regenerate an expired item simultaneously, overloading origin systems.

## Key Strategies
1. Probabilistic Early Expiration (PEE)
2. Request Coalescing / Single-Flight
3. Stale-While-Revalidate (SWR)
4. Jittered TTLs
5. Async Refresh Daemons
6. Soft/Hard TTL Split
7. Token Bucket Regeneration Throttling

## 1. Probabilistic Early Expiration
```
if (now - value.fetchTime) * beta * rand() > ttl {
  recompute()
}
```
Prevents thundering expiry edge by spreading refreshes.

## 2. Single-Flight (Java Sketch)
```java
class SingleFlightCache<K,V> {
  private final ConcurrentHashMap<K, CompletableFuture<V>> inflight = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<K, Entry<V>> data = new ConcurrentHashMap<>();
  record Entry<V>(V v,long exp){}
  CompletableFuture<V> get(K k, Supplier<V> loader, long ttlMs){
    var e = data.get(k);
    long now=System.currentTimeMillis();
    if(e!=null && e.exp>now) return CompletableFuture.completedFuture(e.v());
    return inflight.computeIfAbsent(k, key ->
      CompletableFuture.supplyAsync(() -> loader.get())
        .whenComplete((v,ex)->{inflight.remove(key); if(ex==null) data.put(key,new Entry<>(v,now+ttlMs)); })
    );
  }
}
```

## 3. Stale-While-Revalidate
Serve stale for short soft window while background refresh occurs; cap with hard TTL to avoid infinite staleness.

## 4. Jittered TTLs
Avoid synchronized expirations: `effectiveTTL = baseTTL - rand(0, spread)`

## 5. Async Refresh Daemons
Preemptively refresh hot keys based on access frequency histogram.

## 6. Soft vs Hard TTL
Soft -> allowed stale serve; Hard -> mandatory recompute.

## 7. Regeneration Throttling
Use token bucket per key to bound refresh QPS.

## Strategy Selection Matrix
| Workload | Recommended Combo |
|----------|-------------------|
| Read-heavy, few hot keys | Single-flight + SWR + jitter |
| High churn keys | Probabilistic + jitter |
| Latency sensitive | SWR + async refresh |
| Expensive recompute | Single-flight + token bucket |

## Metrics
- Origin QPS before/after
- % requests served stale (target controlled band)
- Mean regeneration latency
- Suppressed duplicate recomputations

## Pitfalls
| Pitfall | Avoidance |
|---------|-----------|
| Serving stale forever | Enforce hard TTL |
| Loader explosion under burst | Coalesce + throttle |
| Coordinating across nodes | Use distributed locks or version fencing |

## Integration Notes
Combine with circuit breaker to degrade gracefully if regeneration also failing. Emit structured events: `cache.refresh.start`, `cache.refresh.success`, `cache.refresh.suppressed` for observability.
