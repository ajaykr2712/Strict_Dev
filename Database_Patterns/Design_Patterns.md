# Database Design Patterns

## Schema Design Patterns

### Embedded Documents
- Store related data within a single document
- Reduce joins and improve read performance
- Best for one-to-few relationships
- Example: User profile with embedded addresses

### References
- Store references to other documents
- Normalize data to avoid duplication
- Best for one-to-many and many-to-many relationships
- Example: Orders referencing products

### Denormalization
- Duplicate data to optimize read operations
- Trade storage space for query performance
- Best for read-heavy applications
- Example: Product information copied to order items

## Data Modeling Patterns

### Polymorphic Pattern
- Store different types of data in same collection
- Use discriminator field to identify type
- Flexible schema for varying document structures
- Example: Different content types (movies, shows, documentaries)

### Bucket Pattern
- Group documents into time-based or size-based buckets
- Reduce index size and improve query performance
- Best for time-series and IoT data
- Example: Sensor readings grouped by hour

### Computed Pattern
- Pre-calculate and store frequently accessed values
- Reduce real-time computation overhead
- Update computed values on data changes
- Example: User statistics and aggregations

### Subset Pattern
- Store frequently accessed data separately
- Keep working set in memory
- Link to complete data when needed
- Example: Recent messages vs. message history

## Query Optimization Patterns

### Index Design
- Create indexes on frequently queried fields
- Use compound indexes for multi-field queries
- Consider partial and sparse indexes
- Monitor index usage and performance

### Aggregation Pipeline
- Use aggregation framework for complex queries
- Pipeline stages for filtering, grouping, and transforming
- Optimize pipeline order for performance
- Example: User behavior analytics

### Pagination Strategies
- Offset-based pagination for simple cases
- Cursor-based pagination for large datasets
- Time-based pagination for real-time data
- Avoid large offset values

## Consistency Patterns

### Write Concerns
- Acknowledgment levels for write operations
- Balance between performance and durability
- Choose based on application requirements
- Example: Critical data vs. cache data

### Read Preferences
- Primary reads for strong consistency
- Secondary reads for eventual consistency
- Nearest reads for reduced latency
- Consider network topology

### Transactions
- ACID properties for critical operations
- Multi-document transactions when needed
- Consider performance impact
- Design for minimal transaction scope

## Scalability Patterns

### Horizontal Scaling (Sharding)
- Distribute data across multiple servers
- Choose appropriate shard key
- Avoid hotspots and uneven distribution
- Plan for shard key changes

### Vertical Scaling
- Increase server resources
- Optimize for single-server performance
- Consider resource limits
- Monitor resource utilization

### Read Replicas
- Distribute read load across replicas
- Improve read performance and availability
- Handle eventual consistency
- Geographic distribution for global apps
