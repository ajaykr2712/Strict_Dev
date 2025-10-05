# Database Design Patterns - Complete Guide

## Overview

Database design patterns provide proven solutions for common data storage, retrieval, and management challenges. They help create scalable, maintainable, and performant data layers for applications.

## 1. SQL Database Design Patterns

### 1.1 Normalization Patterns

#### First Normal Form (1NF)
- Each table cell contains a single value
- Each row is unique
- No repeating groups

```sql
-- ❌ Violates 1NF (multiple values in one cell)
CREATE TABLE customers_bad (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    phone_numbers VARCHAR(500) -- "123-456-7890, 987-654-3210"
);

-- ✅ Follows 1NF
CREATE TABLE customers (
    id INT PRIMARY KEY,
    name VARCHAR(100)
);

CREATE TABLE customer_phones (
    id INT PRIMARY KEY,
    customer_id INT,
    phone_number VARCHAR(20),
    phone_type VARCHAR(20),
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);
```

#### Second Normal Form (2NF)
- Must be in 1NF
- No partial dependencies (non-key attributes depend on entire primary key)

```sql
-- ❌ Violates 2NF (supplier_name depends only on supplier_id, not full key)
CREATE TABLE order_items_bad (
    order_id INT,
    product_id INT,
    supplier_id INT,
    quantity INT,
    supplier_name VARCHAR(100), -- Partial dependency
    PRIMARY KEY (order_id, product_id)
);

-- ✅ Follows 2NF
CREATE TABLE suppliers (
    id INT PRIMARY KEY,
    name VARCHAR(100)
);

CREATE TABLE order_items (
    order_id INT,
    product_id INT,
    supplier_id INT,
    quantity INT,
    PRIMARY KEY (order_id, product_id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);
```

#### Third Normal Form (3NF)
- Must be in 2NF
- No transitive dependencies

```sql
-- ❌ Violates 3NF (city depends on zip_code, not customer_id)
CREATE TABLE customers_bad (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    zip_code VARCHAR(10),
    city VARCHAR(50) -- Transitive dependency
);

-- ✅ Follows 3NF
CREATE TABLE zip_codes (
    zip_code VARCHAR(10) PRIMARY KEY,
    city VARCHAR(50),
    state VARCHAR(50)
);

CREATE TABLE customers (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    zip_code VARCHAR(10),
    FOREIGN KEY (zip_code) REFERENCES zip_codes(zip_code)
);
```

### 1.2 Indexing Patterns

#### B-Tree Indexes
Best for range queries and exact matches.

```sql
-- Primary key index (automatically created)
CREATE TABLE users (
    id INT PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMP
);

-- Index for frequent queries
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Composite index for multi-column queries
CREATE INDEX idx_users_status_created ON users(status, created_at);
```

#### Partial Indexes
Index only rows that meet certain conditions.

```sql
-- Index only active users
CREATE INDEX idx_active_users ON users(email) WHERE status = 'active';

-- Index only recent orders
CREATE INDEX idx_recent_orders ON orders(user_id, created_at) 
WHERE created_at > NOW() - INTERVAL '1 year';
```

### 1.3 Partitioning Patterns

#### Range Partitioning
```sql
-- Partition by date range
CREATE TABLE sales (
    id BIGINT,
    sale_date DATE,
    amount DECIMAL(10,2)
) PARTITION BY RANGE (sale_date);

CREATE TABLE sales_2023 PARTITION OF sales 
FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');

CREATE TABLE sales_2024 PARTITION OF sales 
FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
```

#### Hash Partitioning
```sql
-- Partition by hash for even distribution
CREATE TABLE user_activities (
    id BIGINT,
    user_id BIGINT,
    activity_type VARCHAR(50),
    created_at TIMESTAMP
) PARTITION BY HASH (user_id);

CREATE TABLE user_activities_0 PARTITION OF user_activities 
FOR VALUES WITH (MODULUS 4, REMAINDER 0);

CREATE TABLE user_activities_1 PARTITION OF user_activities 
FOR VALUES WITH (MODULUS 4, REMAINDER 1);
```

## 2. NoSQL Database Design Patterns

### 2.1 Document Database Patterns (MongoDB)

#### Embedding Pattern
Embed related data within a single document.

```javascript
// ✅ Good for one-to-few relationships
{
  "_id": ObjectId("..."),
  "name": "John Doe",
  "email": "john@example.com",
  "addresses": [
    {
      "type": "home",
      "street": "123 Main St",
      "city": "Anytown",
      "zipCode": "12345"
    },
    {
      "type": "work",
      "street": "456 Business Ave",
      "city": "Corporate City",
      "zipCode": "67890"
    }
  ],
  "preferences": {
    "newsletter": true,
    "notifications": false
  }
}
```

#### Reference Pattern
Store references to related documents.

```javascript
// User document
{
  "_id": ObjectId("user_id"),
  "name": "John Doe",
  "email": "john@example.com"
}

// Order documents (many orders per user)
{
  "_id": ObjectId("order_id_1"),
  "user_id": ObjectId("user_id"),
  "total": 99.99,
  "items": [...]
}
```

#### Bucket Pattern
Group related documents together.

```javascript
// Time series data bucketed by hour
{
  "_id": ObjectId("..."),
  "sensor_id": "sensor_001",
  "timestamp": ISODate("2024-01-01T10:00:00Z"),
  "measurements": [
    {
      "time": ISODate("2024-01-01T10:00:00Z"),
      "temperature": 22.5,
      "humidity": 45.2
    },
    {
      "time": ISODate("2024-01-01T10:01:00Z"),
      "temperature": 22.7,
      "humidity": 45.0
    }
    // ... more measurements for this hour
  ]
}
```

### 2.2 Key-Value Store Patterns (Redis)

#### Caching Pattern
```redis
# Simple key-value caching
SET user:1001:profile '{"name":"John","email":"john@example.com"}' EX 3600

# Hash-based user session
HSET session:abc123 user_id 1001 login_time 1640995200 last_activity 1640998800
EXPIRE session:abc123 1800

# List for recent activities
LPUSH user:1001:activities "logged_in" "viewed_profile" "updated_settings"
LTRIM user:1001:activities 0 99  # Keep only last 100 activities
```

#### Pub/Sub Pattern
```redis
# Publisher
PUBLISH notifications '{"type":"new_message","user_id":1001,"message":"Hello!"}'

# Subscriber (in application code)
# SUBSCRIBE notifications
```

#### Rate Limiting Pattern
```redis
# Sliding window rate limiting
local key = "rate_limit:" .. user_id
local window = 3600  -- 1 hour
local limit = 1000   -- 1000 requests per hour

local current = redis.call('INCR', key)
if current == 1 then
    redis.call('EXPIRE', key, window)
end

if current > limit then
    return 0  -- Rate limit exceeded
else
    return 1  -- Request allowed
end
```

### 2.3 Column Family Patterns (Cassandra)

#### Wide Row Pattern
```cql
-- Time series data with wide rows
CREATE TABLE sensor_data (
    sensor_id TEXT,
    date DATE,
    timestamp TIMESTAMP,
    temperature DOUBLE,
    humidity DOUBLE,
    PRIMARY KEY (sensor_id, date, timestamp)
) WITH CLUSTERING ORDER BY (date DESC, timestamp DESC);

-- Query recent data for a sensor
SELECT * FROM sensor_data 
WHERE sensor_id = 'sensor_001' 
AND date >= '2024-01-01' 
ORDER BY date DESC, timestamp DESC 
LIMIT 100;
```

#### Denormalization Pattern
```cql
-- User profile table
CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    username TEXT,
    email TEXT,
    first_name TEXT,
    last_name TEXT,
    created_at TIMESTAMP
);

-- Denormalized user posts (includes user info)
CREATE TABLE user_posts (
    user_id UUID,
    post_id UUID,
    username TEXT,        -- Denormalized from user_profiles
    user_first_name TEXT, -- Denormalized from user_profiles
    title TEXT,
    content TEXT,
    created_at TIMESTAMP,
    PRIMARY KEY (user_id, post_id)
) WITH CLUSTERING ORDER BY (post_id DESC);
```

## 3. Caching Strategies

### 3.1 Cache-Aside (Lazy Loading)
```java
public class UserService {
    private UserRepository userRepository;
    private RedisTemplate<String, User> redisTemplate;
    
    public User getUser(Long userId) {
        String cacheKey = "user:" + userId;
        
        // Try to get from cache first
        User user = redisTemplate.opsForValue().get(cacheKey);
        
        if (user == null) {
            // Cache miss - load from database
            user = userRepository.findById(userId);
            
            if (user != null) {
                // Store in cache for future requests
                redisTemplate.opsForValue().set(cacheKey, user, Duration.ofHours(1));
            }
        }
        
        return user;
    }
    
    public void updateUser(User user) {
        // Update database
        userRepository.save(user);
        
        // Invalidate cache
        String cacheKey = "user:" + user.getId();
        redisTemplate.delete(cacheKey);
    }
}
```

### 3.2 Write-Through
```java
public class WriteThoughUserService {
    public void saveUser(User user) {
        // Write to database first
        userRepository.save(user);
        
        // Then write to cache
        String cacheKey = "user:" + user.getId();
        redisTemplate.opsForValue().set(cacheKey, user, Duration.ofHours(1));
    }
}
```

### 3.3 Write-Behind (Write-Back)
```java
public class WriteBehindUserService {
    private Queue<User> writeQueue = new ConcurrentLinkedQueue<>();
    
    public void saveUser(User user) {
        // Write to cache immediately
        String cacheKey = "user:" + user.getId();
        redisTemplate.opsForValue().set(cacheKey, user, Duration.ofHours(1));
        
        // Queue for asynchronous database write
        writeQueue.offer(user);
    }
    
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    public void flushToDatabase() {
        List<User> usersToWrite = new ArrayList<>();
        
        // Drain queue
        User user;
        while ((user = writeQueue.poll()) != null) {
            usersToWrite.add(user);
        }
        
        // Batch write to database
        if (!usersToWrite.isEmpty()) {
            userRepository.saveAll(usersToWrite);
        }
    }
}
```

## 4. Data Modeling Patterns

### 4.1 Polyglot Persistence
Use different databases for different use cases.

```java
@Service
public class ECommerceService {
    
    // MySQL for transactional data
    @Autowired
    private OrderRepository orderRepository; // JPA/MySQL
    
    // MongoDB for product catalog
    @Autowired
    private ProductRepository productRepository; // MongoDB
    
    // Redis for session management
    @Autowired
    private RedisTemplate<String, Object> redisTemplate; // Redis
    
    // Elasticsearch for search
    @Autowired
    private ProductSearchRepository searchRepository; // Elasticsearch
    
    public void createOrder(Order order) {
        // Save order in MySQL
        Order savedOrder = orderRepository.save(order);
        
        // Update inventory in MongoDB
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId());
            product.decreaseStock(item.getQuantity());
            productRepository.save(product);
            
            // Update search index
            searchRepository.save(product);
        }
        
        // Cache order summary in Redis
        String cacheKey = "order_summary:" + savedOrder.getId();
        redisTemplate.opsForValue().set(cacheKey, 
            createOrderSummary(savedOrder), Duration.ofHours(24));
    }
}
```

### 4.2 Event Sourcing Pattern
```java
// Event Store
@Entity
public class EventStore {
    @Id
    private String id;
    private String aggregateId;
    private String eventType;
    private String eventData;
    private LocalDateTime timestamp;
    private Integer version;
    
    // getters and setters
}

// Aggregate Root
public class Account {
    private String id;
    private BigDecimal balance;
    private List<Event> uncommittedEvents = new ArrayList<>();
    
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        MoneyDepositedEvent event = new MoneyDepositedEvent(id, amount);
        apply(event);
    }
    
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        
        MoneyWithdrawnEvent event = new MoneyWithdrawnEvent(id, amount);
        apply(event);
    }
    
    private void apply(Event event) {
        // Apply event to current state
        when(event);
        
        // Add to uncommitted events
        uncommittedEvents.add(event);
    }
    
    private void when(Event event) {
        if (event instanceof MoneyDepositedEvent) {
            MoneyDepositedEvent depositEvent = (MoneyDepositedEvent) event;
            this.balance = this.balance.add(depositEvent.getAmount());
        } else if (event instanceof MoneyWithdrawnEvent) {
            MoneyWithdrawnEvent withdrawEvent = (MoneyWithdrawnEvent) event;
            this.balance = this.balance.subtract(withdrawEvent.getAmount());
        }
    }
    
    public List<Event> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
    
    // Rebuild from events
    public static Account fromEvents(String accountId, List<Event> events) {
        Account account = new Account();
        account.id = accountId;
        account.balance = BigDecimal.ZERO;
        
        for (Event event : events) {
            account.when(event);
        }
        
        return account;
    }
}
```

### 4.3 CQRS (Command Query Responsibility Segregation)
```java
// Command Side
@Service
public class AccountCommandService {
    private EventStore eventStore;
    private EventBus eventBus;
    
    public void deposit(String accountId, BigDecimal amount) {
        // Load aggregate from events
        List<Event> events = eventStore.getEvents(accountId);
        Account account = Account.fromEvents(accountId, events);
        
        // Execute command
        account.deposit(amount);
        
        // Save new events
        for (Event event : account.getUncommittedEvents()) {
            eventStore.saveEvent(event);
            eventBus.publish(event);
        }
        
        account.markEventsAsCommitted();
    }
}

// Query Side
@Entity
public class AccountView {
    @Id
    private String accountId;
    private BigDecimal balance;
    private LocalDateTime lastUpdated;
    private Integer transactionCount;
    
    // getters and setters
}

@Service
public class AccountQueryService {
    private AccountViewRepository viewRepository;
    
    public AccountView getAccount(String accountId) {
        return viewRepository.findById(accountId);
    }
    
    public List<AccountView> getAccountsWithBalanceGreaterThan(BigDecimal amount) {
        return viewRepository.findByBalanceGreaterThan(amount);
    }
}

// Event Handler for Query Side
@EventHandler
public class AccountViewEventHandler {
    private AccountViewRepository viewRepository;
    
    @EventHandler
    public void handle(MoneyDepositedEvent event) {
        AccountView view = viewRepository.findById(event.getAccountId());
        if (view == null) {
            view = new AccountView();
            view.setAccountId(event.getAccountId());
            view.setBalance(BigDecimal.ZERO);
        }
        
        view.setBalance(view.getBalance().add(event.getAmount()));
        view.setLastUpdated(LocalDateTime.now());
        view.setTransactionCount(view.getTransactionCount() + 1);
        
        viewRepository.save(view);
    }
}
```

## 5. Performance Optimization Patterns

### 5.1 Connection Pooling
```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("user");
        config.setPassword("password");
        
        // Connection pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }
    
    @Bean
    public DataSource readOnlyDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://read-replica:3306/mydb");
        config.setUsername("readonly_user");
        config.setPassword("password");
        config.setReadOnly(true);
        
        return new HikariDataSource(config);
    }
}
```

### 5.2 Database Sharding
```java
@Component
public class ShardingStrategy {
    private static final int SHARD_COUNT = 4;
    
    public String getShardKey(Long userId) {
        int shardIndex = (int) (userId % SHARD_COUNT);
        return "shard_" + shardIndex;
    }
    
    public DataSource getDataSourceForUser(Long userId) {
        String shardKey = getShardKey(userId);
        return dataSourceMap.get(shardKey);
    }
}

@Service
public class ShardedUserService {
    private ShardingStrategy shardingStrategy;
    private Map<String, JdbcTemplate> jdbcTemplates;
    
    public User findUser(Long userId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplateForUser(userId);
        return jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            new Object[]{userId},
            new UserRowMapper()
        );
    }
    
    private JdbcTemplate getJdbcTemplateForUser(Long userId) {
        String shardKey = shardingStrategy.getShardKey(userId);
        return jdbcTemplates.get(shardKey);
    }
}
```

## Best Practices Summary

### SQL Databases
1. **Normalize appropriately** - Usually 3NF is sufficient
2. **Index strategically** - Cover frequent queries, avoid over-indexing
3. **Use appropriate data types** - Choose the smallest suitable type
4. **Implement proper constraints** - Foreign keys, check constraints
5. **Monitor query performance** - Use query plans and profiling

### NoSQL Databases
1. **Model for your queries** - Design based on access patterns
2. **Denormalize when beneficial** - Reduce joins by duplicating data
3. **Use appropriate consistency levels** - Balance consistency vs. performance
4. **Plan for scalability** - Consider partitioning strategies early
5. **Monitor and optimize** - Track performance metrics

### Caching
1. **Cache frequently accessed data** - Focus on read-heavy operations
2. **Set appropriate TTL** - Balance freshness vs. performance
3. **Handle cache invalidation** - Keep cache consistent with data
4. **Monitor cache hit rates** - Optimize cache strategies
5. **Plan for cache failures** - Graceful degradation

### General
1. **Choose the right tool** - Different databases for different needs
2. **Plan for growth** - Consider future scalability requirements
3. **Implement monitoring** - Track performance and errors
4. **Test thoroughly** - Load testing and failover scenarios
5. **Document your decisions** - Explain why specific patterns were chosen
