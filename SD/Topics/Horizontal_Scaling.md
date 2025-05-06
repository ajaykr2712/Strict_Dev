# Horizontal Scaling

## Overview
Horizontal scaling ("scaling out") involves adding more servers or nodes to a system to distribute load and increase capacity. It is the foundation of modern, resilient, and highly available architectures.

## Key Concepts
- **Distributed Systems:** Multiple servers work together, often behind a load balancer.
- **Statelessness:** Applications are designed so any node can handle any request.
- **Elasticity:** Resources can be added or removed dynamically based on demand.

## Advanced Topics
### 1. Load Balancing
- Distributes requests across nodes to prevent overload.
- Algorithms: Round-robin, least connections, weighted, geo-based.
- Health checks and failover mechanisms.

### 2. Data Partitioning
- **Sharding:** Splitting data across nodes for scalability.
- **Consistent Hashing:** Evenly distributes data and minimizes rebalancing.
- **Replication:** Copies data for redundancy and high availability.

### 3. State Management
- **Session Management:** Store session data in distributed caches (Redis, Memcached) or databases.
- **Sticky Sessions:** Load balancer routes a user to the same node (not ideal for true statelessness).

### 4. Fault Tolerance
- **Redundancy:** Multiple nodes ensure service continuity if one fails.
- **Self-Healing:** Systems detect and replace failed nodes automatically.
- **Graceful Degradation:** System continues to operate at reduced capacity during failures.

### 5. Consistency Challenges
- **CAP Theorem:** Trade-offs between consistency, availability, and partition tolerance.
- **Eventual Consistency:** Common in distributed databases and caches.
- **Distributed Transactions:** Complex and often avoided in favor of eventual consistency.

### 6. Real-World Example
- Web applications using Kubernetes or AWS Auto Scaling Groups to add/remove servers based on traffic.
- Distributed databases like Cassandra or MongoDB scale horizontally for massive datasets.

### 7. Best Practices
- Design stateless services for easy scaling.
- Automate provisioning and scaling (infrastructure as code, auto-scaling policies).
- Monitor node health and system metrics.
- Use distributed logging and tracing for observability.

### 8. Interview Questions
- What are the challenges of horizontal scaling?
- How do you manage state in a horizontally scaled system?
- Explain sharding and its impact on scalability.

### 9. Diagram
```
[Load Balancer]
      |
-----------------------------
|      |      |      |      |
[Node1][Node2][Node3][NodeN]
```

---
Continue to the next topic for deeper mastery!