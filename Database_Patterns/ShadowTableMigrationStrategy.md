# Shadow Table Migration Strategy

Zero-downtime schema/data migrations using a shadow (dual-write) table.

## Problem
Need to evolve schema (e.g., split column, change type) without blocking writes or risking data loss.

## Core Idea
1. Create shadow table with new schema
2. Dual-write (old -> old+shadow) during transition
3. Backfill historical rows in controlled batches
4. Flip read path to shadow table
5. Retire original table

## Flow
```
Writer -> OldTable + ShadowTable (dual write)
          ^              |
          | (backfill)   v
        Backfill Job  Consistency Validator
```

## Steps
1. Provision shadow table (include version marker column)
2. Add feature flag for dual-write
3. Start backfill (id range or timestamp batches)
4. Validate row counts & checksums periodically
5. Enable read switch (canary % of traffic)
6. Monitor metrics (latency, error rate)
7. Finalize: stop dual-write, archive old table

## Backfill Throttling
Use token bucket (e.g., max 500 rows/sec) to avoid IO saturation. Track progress via watermark (last id processed).

## Consistency Validation
- Count diff < threshold (e.g., 0.01%)
- Sampled row hash match
- Missing row alerting

## Failure Handling
| Failure | Response |
|---------|----------|
| Shadow write errors | Disable dual-write feature flag |
| Backfill lag growing | Reduce batch size / increase resources |
| Validation mismatch | Pause read flip; investigate corruption |

## Observability
Emit structured events:
- `migration.dualwrite.enabled`
- `migration.backfill.progress { processed, remaining }`
- `migration.validation.mismatch { rowId }`

## Rollback Plan
If read flip causes issues: revert flag to read old table immediately; shadow still up-to-date via dual-write.

## When to Use
- Large tables where blocking ALTER is costly
- Complex transformations requiring derived columns

## When Not to Use
- Tiny tables (simple in-place ALTER fine)
- Non-critical internal data

## Tooling Suggestions
- Change data capture replication (Debezium) for near real-time shadow sync
- Checksum utilities (pg_comparator, mysqldump --skip-extended-insert + hash)

## Success Metrics
- Zero failed writes due to migration
- Backfill completed within SLA window
- No elevated read latency during flip
