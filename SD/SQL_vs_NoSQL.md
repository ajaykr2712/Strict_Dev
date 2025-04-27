# SQL vs NoSQL

## Overview
SQL (Structured Query Language) and NoSQL (Not Only SQL) databases represent two major paradigms in data storage and management. Understanding their differences, strengths, and trade-offs is crucial for designing scalable, efficient systems.

## Key Concepts
- **SQL Databases:** Relational, use structured schemas, support ACID transactions (e.g., MySQL, PostgreSQL, Oracle).
- **NoSQL Databases:** Non-relational, flexible schemas, designed for scalability and specific use cases (e.g., MongoDB, Cassandra, Redis, DynamoDB).

## Advanced Topics
### 1. Data Modeling
- **SQL:** Tables, rows, columns, relationships (foreign keys, joins).
- **NoSQL:** Document (JSON/BSON), key-value, column-family, graph models.
- **Schema Evolution:** SQL requires migrations; NoSQL allows dynamic fields.

### 2. Consistency Models
- **SQL:** Strong consistency, ACID guarantees.
- **NoSQL:** Eventual consistency, BASE (Basically Available, Soft state, Eventual consistency), tunable consistency (e.g., Cassandra).

### 3. Scalability
- **SQL:** Vertical scaling (bigger servers), some support for sharding/replication.
- **NoSQL:** Horizontal scaling (more servers), built for distributed architectures.

### 4. Query Capabilities
- **SQL:** Powerful, expressive queries (JOINs, aggregations, transactions).
- **NoSQL:** Simpler queries, optimized for speed and scale; some support secondary indexes and aggregations.

### 5. Transactions
- **SQL:** Multi-row, multi-table ACID transactions.
- **NoSQL:** Limited or no ACID support; some (e.g., MongoDB, Cosmos DB) offer multi-document transactions.

### 6. Use Cases
- **SQL:** Financial systems, ERP, CRM, applications requiring complex queries and strong consistency.
- **NoSQL:** Real-time analytics, IoT, content management, big data, caching, social networks.

### 7. Performance
- **SQL:** Optimized for complex queries, but can struggle with massive scale.
- **NoSQL:** Optimized for high throughput, low latency, and large-scale data.

### 8. Security
- **SQL:** Mature access controls, encryption, auditing.
- **NoSQL:** Varies by implementation; newer systems may lack mature security features.

### 9. Best Practices
- Choose SQL for structured data and transactional integrity.
- Choose NoSQL for flexibility, scalability, and unstructured or semi-structured data.
- Consider hybrid approaches (polyglot persistence) for complex systems.

### 10. Interview Questions
- What are the main differences between SQL and NoSQL databases?
- When would you choose NoSQL over SQL?
- How do NoSQL databases handle consistency and transactions?

### 11. Diagram
```
[App] --(SQL Query)--> [SQL DB: Tables, Rows]
[App] --(NoSQL Query)--> [NoSQL DB: Docs, KV, Columns, Graph]
```

---
Continue to the next topic for deeper mastery!