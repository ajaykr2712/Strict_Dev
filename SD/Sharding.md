# Sharding

## Overview
Sharding is a database architecture pattern that splits large datasets into smaller, more manageable pieces called shards. Each shard is a subset of the data, stored on a separate database server or cluster, enabling horizontal scaling and improved performance for massive applications.

## Key Concepts
- **Shard:** A partition of the overall dataset, typically based on a sharding key (e.g., user ID, geographic region).
- **Sharding Key:** The attribute used to determine which shard a data item belongs to.
- **Shard Map/Directory:** Metadata that maps data ranges or keys to specific shards.
- **Routing Layer:** Middleware or logic that directs queries to the correct shard.

## Advanced Topics
### 1. Sharding Strategies
- **Range-Based Sharding:** Data is partitioned by value ranges (e.g., user IDs 1-1000 on Shard 1).
- **Hash-Based Sharding:** Data is distributed using a hash function on the sharding key, ensuring even distribution.
- **Directory-Based Sharding:** A lookup table maps each key to a shard, allowing flexible placement.
- **Geo-Sharding:** Data is partitioned by geographic region for locality and compliance.

### 2. Rebalancing and Resharding
- **Hotspots:** Uneven data distribution can overload certain shards; monitor and rebalance as needed.
- **Resharding:** Splitting or merging shards to accommodate growth or reduce load.
- **Zero-Downtime Resharding:** Techniques like dual writes and phased cutovers to avoid downtime.

### 3. Cross-Shard Operations
- **Joins and Transactions:** Complex queries spanning multiple shards are challenging; often require denormalization or application-level joins.
- **Global Secondary Indexes:** Indexes that span all shards, often with performance trade-offs.

### 4. Consistency and Availability
- **Distributed Transactions:** Two-phase commit (2PC) or eventual consistency models.
- **CAP Theorem:** Sharded systems must balance consistency, availability, and partition tolerance.

### 5. Real-World Examples
- **MongoDB:** Built-in sharding for massive document stores.
- **Cassandra:** Hash-based sharding for distributed, scalable storage.
- **Twitter:** User timelines sharded by user ID to handle billions of tweets.

### 6. Best Practices
- Choose a sharding key that evenly distributes load and minimizes cross-shard queries.
- Monitor shard health and balance regularly.
- Automate resharding and failover processes.
- Document sharding logic and routing for maintainability.

### 7. Interview Questions
- What are the trade-offs between range-based and hash-based sharding?
- How do you handle cross-shard transactions?
- Describe a scenario where sharding is necessary.

### 8. Diagram
```
[App] --(Shard Key)--> [Shard 1: User IDs 1-1M]
                  |--> [Shard 2: User IDs 1M-2M]
                  |--> [Shard N: User IDs ...]
```

---
Continue to the next topic for deeper mastery!