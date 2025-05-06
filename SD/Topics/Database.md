# Database

## Overview
A database is an organized collection of structured information, or data, typically stored electronically in a computer system. Databases are foundational to nearly every application, enabling efficient storage, retrieval, and management of data.

## Key Concepts
- **DBMS (Database Management System):** Software for creating, managing, and interacting with databases (e.g., MySQL, PostgreSQL, MongoDB).
- **Schema:** The structure of the database (tables, fields, relationships).
- **Query Language:** SQL for relational databases, various APIs for NoSQL.

## Advanced Topics
### 1. ACID Properties
- **Atomicity:** Transactions are all-or-nothing.
- **Consistency:** Transactions bring the database from one valid state to another.
- **Isolation:** Concurrent transactions do not interfere with each other.
- **Durability:** Once committed, data is permanent.

### 2. Indexing
- Improves query performance by allowing fast data retrieval.
- Types: B-tree, hash, bitmap, full-text.
- Trade-off: Faster reads, but slower writes and increased storage.

### 3. Transactions & Isolation Levels
- **Read Uncommitted, Read Committed, Repeatable Read, Serializable.**
- Prevents issues like dirty reads, non-repeatable reads, and phantom reads.

### 4. Replication
- Copies data across multiple servers for high availability and fault tolerance.
- Types: Master-slave, master-master, synchronous, asynchronous.

### 5. Sharding
- Splits large databases into smaller, faster, more manageable parts called shards.
- Sharding keys and strategies (range, hash, geo-based).

### 6. CAP Theorem
- **Consistency, Availability, Partition Tolerance:** Only two can be fully achieved at once in distributed systems.

### 7. NoSQL Databases
- Types: Document (MongoDB), Key-Value (Redis), Columnar (Cassandra), Graph (Neo4j).
- Schema-less, horizontal scaling, eventual consistency.

### 8. Performance Tuning
- Query optimization (EXPLAIN plans, avoiding N+1 queries).
- Connection pooling.
- Caching strategies (in-memory, query result caching).

### 9. Security
- Encryption at rest and in transit.
- Access controls and roles.
- Auditing and monitoring.

### 10. Real-World Example
- E-commerce: Orders, users, inventory stored in relational DB; product catalog in NoSQL for fast search.
- Social networks: User data in SQL, activity feeds in NoSQL for scalability.

### 11. Best Practices
- Normalize for consistency, denormalize for performance where needed.
- Regularly back up data and test restores.
- Monitor slow queries and optimize indexes.

### 12. Interview Questions
- Explain the differences between SQL and NoSQL databases.
- What is the CAP theorem and how does it apply to distributed databases?
- How would you design a database for a high-traffic application?

### 13. Diagram
```
[App Server] --(SQL Query)--> [DBMS] --(Data Storage)--> [Disk/Memory]
```

---
Continue to the next topic for deeper mastery!