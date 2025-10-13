# Backpressure Strategies

Controlling producer rates to prevent overloading downstream systems.

## Symptoms of Missing Backpressure
- Growing unbounded queues
- Elevated latency -> timeout spiral
- Memory pressure / GC churn

## Core Strategies
| Strategy | Mechanism | When |
|----------|-----------|------|
| Bounded Queues | Reject or block when full | Thread pool tasks |
| Token Bucket | Rate shaping (steady + burst) | API gateway, outbound calls |
| Windowed Feedback | Adjust producer concurrency based on moving avg latency | Adaptive pipelines |
| Credits (Pull) | Consumer grants capacity units | Message streaming |
| Load Shedding | Drop low-priority work early | Overload defense |

## Java Bounded Queue Example
```java
var queue = new ArrayBlockingQueue<Runnable>(1000);
ExecutorService exec = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, queue, new ThreadPoolExecutor.AbortPolicy());
```

## Adaptive Concurrency Sketch
Measure P95 latency; increase concurrency window if below target, decrease sharply if above.

## Token Bucket Pseudocode
```
tokens = capacity
loop each interval:
  tokens = min(capacity, tokens + refill)
  if tokens>0: allow request & tokens-- else reject
```

## Prioritized Load Shedding
Maintain multiple queues (high, normal, low). On overload drain high first; drop low.

## Observability
| Metric | Target |
|--------|--------|
| Queue depth | Stable oscillation < 70% capacity |
| Rejected tasks | Controlled & intentional |
| Latency P95 | Within SLO band |

## Pitfalls
| Pitfall | Mitigation |
|---------|-----------|
| Silent drops | Emit structured events for shed requests |
| Per-service isolated tuning only | Coordinate global admission control |
| Over-buffering | Keep buffers minimal to surface pressure early |

## Integration Notes
Backpressure + circuit breakers: breaker opens faster if backlog growing. Feed backlog metrics into autoscaling decisions.
