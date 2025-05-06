# Replication

## Overview
Replication is the process of copying and maintaining database objects, such as data or files, across multiple servers or locations. It is a cornerstone of high availability, fault tolerance, and scalability in distributed systems.

## Key Concepts
- **Primary/Leader:** The main node where writes occur.
- **Replica/Secondary/Follower:** Nodes that receive copies of data from the primary.
- **Synchronous Replication:** Data is written to all replicas before acknowledging the client, ensuring strong consistency but higher latency.
- **Asynchronous Replication:** Data is written to the primary and then propagated to replicas, allowing for lower latency but potential data lag.

## Advanced Topics
### 1. Replication Strategies
- **Single-leader (Master-Slave):** One primary, multiple replicas. Simple but single point of failure.
- **Multi-leader (Master-Master):** Multiple nodes accept writes; conflict resolution required.
- **Peer-to-Peer:** All nodes are equal; used in some NoSQL systems.

### 2. Consistency Models
- **Strong Consistency:** All replicas reflect the latest write before acknowledging.
- **Eventual Consistency:** Replicas may lag but will eventually converge.
- **Read-after-Write Consistency:** Guarantees that a read after a write will return the latest value.

### 3. Failover and Recovery
- **Automatic Failover:** If the primary fails, a replica is promoted to primary.
- **Manual Failover:** Requires operator intervention.
- **Split-Brain:** Multiple primaries due to network partition; must be resolved to avoid data loss.

### 4. Replication Lag
- **Monitoring:** Track delay between primary and replicas.
- **Mitigation:** Optimize network, tune replication settings, use faster disks.

### 5. Use Cases
- **High Availability:** Minimize downtime by failing over to replicas.
- **Geographic Distribution:** Place replicas closer to users for lower latency.
- **Read Scaling:** Offload read queries to replicas.

### 6. Best Practices
- Regularly test failover procedures.
- Monitor replication lag and health.
- Use appropriate consistency models for your application.
- Secure replication channels (TLS, VPN).

### 7. Real-World Example
- MySQL/MariaDB master-slave replication for read scaling.
- MongoDB replica sets for automatic failover.
- PostgreSQL streaming replication for high availability.

### 8. Interview Questions
- What are the trade-offs between synchronous and asynchronous replication?
- How do you handle replication lag in a distributed system?
- Explain how automatic failover works in a replicated database.

### 9. Diagram
```
[Primary]
   |
-------------------
|        |        |
[Replica1][Replica2][ReplicaN]
```

---
Continue to the next topic for deeper mastery!