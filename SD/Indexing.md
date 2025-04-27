# Indexing

## Overview
Indexing is a database optimization technique that improves the speed of data retrieval operations by creating data structures (indexes) that allow quick lookups. Proper indexing is crucial for high-performance, large-scale systems.

## Key Concepts
- **Index:** A data structure (e.g., B-tree, hash) that enables fast search and retrieval.
- **Primary Index:** Built on the primary key, ensures uniqueness.
- **Secondary Index:** Built on non-primary key columns for faster queries.
- **Composite Index:** Indexes on multiple columns.

## Advanced Topics
### 1. Types of Indexes
- **B-tree Index:** Balanced tree structure, efficient for range queries.
- **Hash Index:** Fast for equality lookups, not suitable for range queries.
- **Bitmap Index:** Efficient for columns with low cardinality (few unique values).
- **Full-Text Index:** Enables fast text search (e.g., LIKE, MATCH).
- **Spatial Index:** Optimized for geospatial queries.

### 2. Indexing Strategies
- **Covering Index:** Contains all columns needed for a query, avoiding table lookups.
- **Partial Index:** Indexes a subset of rows based on a condition.
- **Unique Index:** Enforces uniqueness on indexed columns.

### 3. Trade-offs
- **Read vs. Write Performance:** Indexes speed up reads but slow down writes (INSERT, UPDATE, DELETE) due to index maintenance.
- **Storage Overhead:** Indexes consume additional disk space.
- **Index Maintenance:** Regularly rebuild or reorganize indexes to avoid fragmentation.

### 4. Query Optimization
- **EXPLAIN Plans:** Analyze how queries use indexes.
- **Index Hints:** Direct the query optimizer to use specific indexes.
- **Avoid Over-Indexing:** Too many indexes can degrade performance.

### 5. Real-World Example
- E-commerce: Index product names and categories for fast search.
- Social networks: Index user IDs and timestamps for efficient feed retrieval.

### 6. Best Practices
- Index columns used in WHERE, JOIN, and ORDER BY clauses.
- Use composite indexes for multi-column queries.
- Monitor slow queries and add indexes as needed.
- Regularly review and drop unused indexes.

### 7. Interview Questions
- How does a B-tree index work?
- What are the trade-offs of adding more indexes to a table?
- How do you decide which columns to index?

### 8. Diagram
```
[Table]
  |
[Primary Index (B-tree)]
  |
[Fast Lookup: SELECT * FROM users WHERE id = 123]
```

---
Continue to the next topic for deeper mastery!