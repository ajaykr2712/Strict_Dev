# Feature Store Design Patterns

Centralizing, governing, and serving ML features consistently.

## Goals
- Reuse & lineage
- Online/offline parity
- Low-latency serving
- Governance & access control

## Core Components
| Component | Responsibility |
|-----------|----------------|
| Registry | Feature definitions, metadata, ownership |
| Offline Store | Historical computation (batch) |
| Online Store | Low latency lookups |
| Transformation Service | Real-time feature computation |
| Monitoring | Drift, freshness, null ratios |

## Patterns
| Pattern | Use Case | Notes |
|---------|---------|------|
| Push-Based Real-Time | Streaming ingestion updates online store | Low latency events |
| On-Demand Transformation | Compute at request time | Expensive if heavy joins |
| Batch Materialization | Daily aggregates to offline & online | Reduced compute duplication |
| Hybrid TTL Refresh | Keep online features fresh via periodic TTL extension | Counters, decay metrics |

## Example Feature Definition (YAML)
```yaml
name: user_30d_purchase_count
entity: user_id
source: orders_stream
transformation: window_count(30d)
owner: growth-ml
sla: { freshness: "5m", availability: "99.5%" }
pii: false
```

## Online/Offline Parity Tactic
Use identical transformation logic packaged as library; run in Spark (offline) and Flink (online) via adapter layer.

## Governance Checks
- Owner assigned
- Data classification (PII, PCI)
- Freshness SLA monitored
- Backfill documented

## Monitoring Signals
| Signal | Alert Condition |
|--------|-----------------|
| Freshness Lag | now - latest_timestamp > SLA + 2m |
| Drift (KS test) | p-value < 0.01 |
| Null Ratio | > threshold (e.g., 5%) |

## Caching Strategy
Multi-layer: local LRU (JVM) -> Redis -> Persistent store; include versioned feature view hash for invalidation.

## Anti-Patterns
| Issue | Impact |
|-------|--------|
| Ad-hoc SQL per team | Duplication, inconsistency |
| No lineage tracking | Unexplainable model behavior |
| Direct DB queries from model service | Tight coupling, scaling issues |

## Rollout Plan
1. Define 10 canonical high-value features
2. Implement registry + metadata validation
3. Backfill offline store & materialize online
4. Instrument freshness metrics
5. Enforce creation template for new features

## Success Metrics
- % model features sourced from store
- Reduction in duplicate pipelines
- Freshness SLA compliance rate
