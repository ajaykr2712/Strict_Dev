# Database Design & Data Modeling

## Overview
This document outlines the database design for the e-commerce platform, following principles from "Designing Data-Intensive Applications" for scalability, reliability, and maintainability.

## Database Schema

### Core Tables

#### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    
    -- Indexing for performance
    INDEX idx_users_email (email),
    INDEX idx_users_status (status),
    INDEX idx_users_created_at (created_at)
);
```

#### Products Table
```sql
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexing for search and filtering
    INDEX idx_products_category (category),
    INDEX idx_products_status (status),
    INDEX idx_products_price (price),
    FULLTEXT INDEX idx_products_search (name, description)
);
```

#### Orders Table
```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    shipping_address TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexing for queries
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_created_at (created_at)
);
```

#### Order Items Table
```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL, -- Denormalized for performance
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INTEGER NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    
    -- Composite index for order queries
    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_product_id (product_id)
);
```

#### Payments Table
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    user_id UUID NOT NULL REFERENCES users(id),
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexing for financial queries
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_user_id (user_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_transaction_id (transaction_id)
);
```

## Sharding Strategy

### Horizontal Partitioning

#### 1. User Data Sharding
```sql
-- Shard by user_id hash
-- Ensures even distribution of users across shards
CREATE TABLE users_shard_1 AS SELECT * FROM users WHERE hash(id) % 4 = 0;
CREATE TABLE users_shard_2 AS SELECT * FROM users WHERE hash(id) % 4 = 1;
CREATE TABLE users_shard_3 AS SELECT * FROM users WHERE hash(id) % 4 = 2;
CREATE TABLE users_shard_4 AS SELECT * FROM users WHERE hash(id) % 4 = 3;
```

#### 2. Product Data Sharding
```sql
-- Shard by category for better cache locality
CREATE TABLE products_electronics AS SELECT * FROM products WHERE category = 'ELECTRONICS';
CREATE TABLE products_books AS SELECT * FROM products WHERE category = 'BOOKS';
CREATE TABLE products_clothing AS SELECT * FROM products WHERE category = 'CLOTHING';
-- ... other categories
```

#### 3. Order Data Sharding
```sql
-- Shard by date range for historical data management
CREATE TABLE orders_2024_q1 AS SELECT * FROM orders WHERE created_at >= '2024-01-01' AND created_at < '2024-04-01';
CREATE TABLE orders_2024_q2 AS SELECT * FROM orders WHERE created_at >= '2024-04-01' AND created_at < '2024-07-01';
-- ... other quarters
```

## Read/Write Splitting

### Master-Slave Configuration

#### Master Database (Writes)
```java
@Transactional
@WriteDatabase
public User createUser(User user) {
    // All write operations go to master
    return userRepository.save(user);
}
```

#### Slave Databases (Reads)
```java
@ReadDatabase
public User findUserById(String userId) {
    // Read operations go to read replicas
    return userRepository.findById(userId);
}
```

## Caching Strategy

### Multi-Level Caching

#### L1 Cache (Application Level)
```java
@Cacheable(value = "users", key = "#userId")
public User findUserById(String userId) {
    return userRepository.findById(userId);
}
```

#### L2 Cache (Redis)
```java
// Product catalog cache with 24-hour TTL
@Cacheable(value = "products", key = "#productId", expiration = "24h")
public Product findProductById(String productId) {
    return productRepository.findById(productId);
}
```

### Cache Invalidation
```java
@CacheEvict(value = "products", key = "#product.id")
public Product updateProduct(Product product) {
    return productRepository.save(product);
}
```

## Event Sourcing Schema

### Events Table
```sql
CREATE TABLE domain_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    event_version INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexing for event replay
    INDEX idx_events_aggregate_id (aggregate_id),
    INDEX idx_events_aggregate_type (aggregate_type),
    INDEX idx_events_created_at (created_at)
);
```

### Event Store Operations
```java
public void saveEvent(DomainEvent event) {
    EventRecord record = new EventRecord(
        event.getAggregateId(),
        event.getAggregateType(),
        event.getEventType(),
        event.getEventData(),
        event.getVersion()
    );
    eventRepository.save(record);
}

public List<DomainEvent> getEventsForAggregate(String aggregateId) {
    return eventRepository.findByAggregateIdOrderByCreatedAt(aggregateId);
}
```

## Data Consistency Patterns

### ACID Transactions
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void processOrder(Order order) {
    // Strong consistency for financial operations
    orderRepository.save(order);
    paymentRepository.save(payment);
    inventoryService.reserveStock(order.getItems());
}
```

### Eventual Consistency
```java
@EventListener
@Async
public void handleOrderCreated(OrderCreatedEvent event) {
    // Eventually consistent operations
    inventoryService.updateStockAsync(event.getOrderItems());
    emailService.sendOrderConfirmationAsync(event.getOrder());
}
```

## Performance Optimization

### Database Indexing Strategy
```sql
-- Composite indexes for common query patterns
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
CREATE INDEX idx_products_category_price ON products(category, price);
CREATE INDEX idx_payments_user_status_date ON payments(user_id, status, created_at);
```

### Query Optimization
```java
// Use projection queries to reduce data transfer
@Query("SELECT new UserSummary(u.id, u.name, u.email) FROM User u WHERE u.status = :status")
List<UserSummary> findActiveUserSummaries(@Param("status") UserStatus status);
```

### Connection Pooling
```properties
# HikariCP configuration for optimal performance
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

## Backup and Recovery

### Backup Strategy
```bash
# Automated daily backups
pg_dump --format=custom --compress=9 --file=backup_$(date +%Y%m%d).dump ecommerce_db

# Point-in-time recovery setup
archive_mode = on
archive_command = 'cp %p /backup/archive/%f'
wal_level = replica
```

### Disaster Recovery
```bash
# Master-slave replication for high availability
# Automatic failover using tools like Patroni or repmgr
# Cross-region backup replication
```

## Monitoring and Observability

### Database Metrics
```sql
-- Query performance monitoring
SELECT query, calls, total_time, mean_time 
FROM pg_stat_statements 
ORDER BY total_time DESC 
LIMIT 10;

-- Index usage analysis
SELECT schemaname, tablename, attname, n_distinct, correlation 
FROM pg_stats 
WHERE tablename IN ('users', 'products', 'orders');
```

### Health Checks
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check database connectivity and performance
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Health.up().withDetail("database", "Available").build();
        } catch (Exception e) {
            return Health.down().withDetail("database", "Unavailable").build();
        }
    }
}
```

## Migration Strategy

### Schema Versioning
```sql
-- Migration scripts with version control
-- V1__Initial_schema.sql
-- V2__Add_user_preferences.sql
-- V3__Add_product_categories.sql
```

### Zero-Downtime Deployments
```java
// Backward-compatible schema changes
// Feature flags for gradual rollouts
// Blue-green deployment strategies
```
