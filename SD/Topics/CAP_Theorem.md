# CAP Theorem

## Overview
The CAP Theorem, also known as Brewer's Theorem, states that a distributed system can only guarantee two out of the following three properties at any given time: Consistency, Availability, and Partition Tolerance. It is a foundational concept for understanding trade-offs in distributed database and system design.

## Key Concepts
- **Consistency (C):** Every read receives the most recent write or an error.
- **Availability (A):** Every request receives a (non-error) response, without guarantee that it contains the most recent write.
- **Partition Tolerance (P):** The system continues to operate despite arbitrary partitioning due to network failures.

## Advanced Topics
### 1. CAP Trade-offs
- **CA (Consistency + Availability):** Not partition tolerant; suitable for single-node or tightly coupled systems.
- **CP (Consistency + Partition Tolerance):** May sacrifice availability during network partitions (e.g., HBase, MongoDB in some configs).
- **AP (Availability + Partition Tolerance):** May serve stale data to ensure availability (e.g., Couchbase, DynamoDB).

### 2. Real-World Implications
- No distributed system can guarantee all three properties simultaneously.
- System design choices depend on business requirements and failure scenarios.
- Many modern databases offer tunable consistency levels.

### 3. Eventual Consistency
- A common compromise in AP systems where data will become consistent over time.
- Used in systems like Amazon Dynamo, Cassandra, and many NoSQL databases.

### 4. Best Practices
- Analyze your application's tolerance for stale data and downtime.
- Choose database and architecture patterns that align with your CAP priorities.
- Use consensus protocols (e.g., Paxos, Raft) for strong consistency when needed.

### 5. Interview Questions
- Explain the CAP Theorem and its impact on distributed systems.
- Give examples of systems that prioritize each pair of CAP properties.
- How would you design a system for high availability during network partitions?

### 6. Diagram
```
      [Consistency]
         /     \
        /       \
[Availability]---[Partition Tolerance]
```

---
Understanding the CAP Theorem is essential for making informed trade-offs in distributed system design.