# Vertical Partitioning

## Overview
Vertical partitioning is a database design technique that splits a table into smaller tables, each containing a subset of the columns. This approach optimizes performance, security, and manageability for large, complex databases.

## Key Concepts
- **Partition:** A subset of columns from the original table, grouped into a new table.
- **Primary Key:** Shared across partitions to maintain relationships.
- **Normalization:** Often used to reduce redundancy and improve data integrity.

## Advanced Topics
### 1. Use Cases
- **Performance Optimization:** Frequently accessed columns are separated for faster queries.
- **Security:** Sensitive columns (e.g., PII) are isolated for stricter access control.
- **Legacy Systems:** Gradual migration or refactoring of monolithic schemas.

### 2. Implementation Strategies
- **Manual Partitioning:** Explicitly create new tables and update application logic.
- **Database Support:** Some DBMSs offer built-in support for partitioned tables.
- **ORM Support:** Modern ORMs can map entities to vertically partitioned tables.

### 3. Trade-offs
- **Join Overhead:** Queries spanning partitions require joins, impacting performance.
- **Complexity:** Application logic and migrations become more complex.
- **Consistency:** Ensuring atomic updates across partitions may require distributed transactions.

### 4. Real-World Example
- **User Table:** Split into `UserProfile` (basic info) and `UserCredentials` (auth data) for security and performance.
- **E-commerce:** Product details and inventory data stored separately for faster catalog queries.

### 5. Best Practices
- Partition columns based on access patterns and security requirements.
- Use the same primary key in all partitions for easy joins.
- Document partitioning logic for maintainability.
- Monitor query performance and adjust partitions as needed.

### 6. Interview Questions
- What is vertical partitioning and when would you use it?
- How does vertical partitioning differ from horizontal partitioning (sharding)?
- What are the trade-offs of vertical partitioning in large-scale systems?

### 7. Diagram
```
[User Table]
|--[UserProfile: id, name, email]
|--[UserCredentials: id, password_hash, last_login]
```

---
Continue to the next topic for deeper mastery!