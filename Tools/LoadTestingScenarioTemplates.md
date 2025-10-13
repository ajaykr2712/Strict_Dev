# Load Testing Scenario Templates

Reusable scenario blueprints for performance verification.

## 1. Baseline Smoke
| Goal | Verify environment + instrumentation works |
| Users | 5 constant |
| Duration | 5m |
| Metrics | Error %, basic latency |

## 2. Steady State Soak
| Goal | Detect memory leaks, GC patterns |
| Users | 30 constant |
| Duration | 2h+ |
| Metrics | Heap usage drift, 95/99 latency

## 3. Ramp + Spike
| Goal | Observe autoscaling + backpressure |
| Pattern | 0->200 users over 10m, sudden spike to 400 for 2m, drop to 100 |
| Metrics | Scale lag time, queue depth, shed rate |

## 4. Stress to Failure
| Goal | Identify breaking point & graceful degradation |
| Pattern | Linear ramp until error rate > 5% |
| Metrics | Max sustainable RPS, failure modes |

## 5. Realistic Mix
| Goal | Reflect production operation mix |
| Composition | 60% read, 30% write, 10% search |
| Data | Use anonymized prod distribution |

## 6. Cache Warm vs Cold
| Goal | Quantify cache dependency |
| Runs | Warmed cache vs flushed |
| Metrics | Delta in P95 latency, origin QPS |

## 7. Resilience Under Fault
| Goal | Validate circuit breakers/timeouts |
| Faults | Inject 300ms delay in dependency, 5% error burst |
| Metrics | Breaker open rate, fallback success |

## 8. Multi-Region Latency
| Goal | Assess geo routing impact |
| Pattern | 50% traffic region A, 50% region B |
| Metrics | Cross-region call count, added latency |

## 9. Burst Queue Drain
| Goal | Evaluate backlog recovery speed |
| Pattern | Inject 5k jobs then normal arrival rate |
| Metrics | Drain time, max queue depth |

## 10. Data Growth Scaling
| Goal | Query performance vs dataset size |
| Pattern | Run same query at 10GB, 50GB, 200GB |
| Metrics | Response slope, index efficiency |

## Reporting Checklist
- Test config hash
- Git commit id
- Environment spec (CPU/mem)
- Limiting resource (CPU? IO? lock contention?)
- Bottleneck remediation recommendations

## Tooling
- k6, Gatling, Locust, JMeter
- Chaos: Toxiproxy, Envoy fault filters

## Success Criteria Examples
| Scenario | Target |
|----------|--------|
| Steady State | <1% errors, P95 < 300ms |
| Ramp | Recovery to baseline < 2m post spike |
| Stress | Graceful 503s, no cascading failures |
