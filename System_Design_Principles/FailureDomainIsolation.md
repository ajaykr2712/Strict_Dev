# Failure Domain Isolation

Design to confine faults to the smallest blast radius.

## Dimensions of Isolation
| Dimension | Examples |
|-----------|----------|
| Compute | Separate node pools, AZs |
| Network | Segmented VPCs, service meshes |
| Data | Shards, tenant partitions |
| Time | Rate limiting, bulkhead scheduling |
| Software | Feature flag scopes, canaries |

## Tactics
1. Explicitly map critical dependency graph
2. Remove hidden single points (shared cache cluster)
3. Partition by tenant tier (gold vs standard)
4. Use cell-based architecture (N identical cells + traffic router)
5. Stagger deploy waves; abort on anomaly

## Cell Architecture Sketch
```
Global Router -> Cell1 (svc set) + DB shard A
              -> Cell2 (svc set) + DB shard B
              -> Cell3 (svc set) + DB shard C
```
If Cell2 degrades, remove from router without global outage.

## Metrics
- Blast radius (% traffic impacted per incident)
- Mean time to drain unhealthy cell
- Cross-cell dependency count (target downward trend)

## Anti-Patterns
| Issue | Impact | Remedy |
|-------|--------|--------|
| Global shared synchronous dependency | Total outage risk | Localize; add async buffer |
| Single CI/CD pipeline for all services | Coordinated failure | Staged, per-service pipelines |
| Over-centralized config service | Cascading config push failures | Cache & degrade safely |

## Testing Isolation
- Fault injection per cell (latency, kill node)
- Region evacuation game days
- Simulate partial packet loss

## Rollout Strategy
1. Define boundaries + tagging (service->cell)
2. Migrate low-risk services to cells first
3. Introduce per-cell SLO dashboards
4. Add automated cell health scoring
5. Expand to all critical paths

## Success Indicators
- Incidents limited to single cell/AZ
- Faster remediation via drain + replace
- Reduced correlated failures
