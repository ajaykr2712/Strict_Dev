# Low Latency GC Strategies

Reducing GC pause impact for latency sensitive systems (trading, ad bidding, gaming backends).

## Goals
- Predictable tail latency (P99/P999)
- Avoid stop-the-world spikes
- Maintain throughput under sustained allocation

## JVM Collector Options
| Collector | When to Use | Notes |
|-----------|-------------|-------|
| G1 | General low pause < 10GB heap | Balance throughput + latency |
| ZGC | Very low pauses (sub ms) large heaps | Requires newer JDK; colored pointers |
| Shenandoah | Low pause & region evacuation | Good for mixed workloads |
| Serial | Only for tiny heaps/dev | High pauses |

## Tuning Levers
| Lever | Impact |
|-------|--------|
| Object Lifetime Profiling | Promote only survivors | Reduce tenuring churn |
| Allocation Rate Reduction | Less GC pressure | Pooling, reuse buffers |
| Escape Analysis Benefit | Scalar replacement | Fewer heap allocations |
| Region Size (G1/ZGC) | Footprint vs metadata overhead | Auto usually fine |

## Practical Techniques
1. Prefer off-heap ring buffers for transient messages (e.g., Agrona)
2. Use primitive collections to avoid boxing churn
3. Pre-size data structures to avoid rehash growth
4. Flatten object graphs; reduce pointer chasing
5. Monitor allocation flame graphs (async-profiler)
6. Avoid frequent string concatenations -> use `StringBuilder`

## Allocation Budgeting
Track bytes allocated per request. Example target: < 50KB per typical request.

## Telemetry
- `gc.pause` histogram
- Allocation rate (MB/s)
- Live set size after GC
- Promotion failure counts

## Sample JVM Flags (Exploratory)
```
-XX:+UseZGC \
-XX:ZCollectionInterval=0.5 \
-XX:+ZProactive \
-XX:+AlwaysPreTouch \
-XX:ConcGCThreads=8
```
Tune with benchmarks; avoid cargo cult.

## Verifying Improvements
1. Baseline latency distribution before changes
2. Apply one change
3. Run identical load (fixed seed)
4. Compare P99/P999 delta; keep improvements > noise threshold

## Anti-Patterns
| Issue | Alternative |
|-------|------------|
| Excessive object pooling of small immutable objects | Let GC reclaim fast |
| Custom GC tinkering without metrics | Instrument first |
| Overusing finalizers/phantom refs | Use Cleaner/API sparingly |

## Decision Flow
```
Need sub-ms pauses? -> Try ZGC/Shenandoah
Pauses OK (<50ms) and mid-size heap? -> G1 tuned
Huge heap > 16GB? -> ZGC candidate
```

## Success Metrics
- P99 pause < target (e.g., 5ms)
- Stable allocation rate under spike
- Reduced young gen promotion amplification
