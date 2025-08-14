# Database Design Patterns for High-Scale Systems

This guide covers advanced database design patterns used by companies like Netflix, Uber, and WhatsApp to handle massive scale and ensure high availability.

## 1. Sharding Strategies

### Horizontal Sharding (Partitioning)

#### Range-Based Sharding
**Netflix Content Catalog Example:**
```sql
-- Shard by content ID ranges
-- Shard 1: content_id 1-1000000
-- Shard 2: content_id 1000001-2000000
-- Shard 3: content_id 2000001-3000000

CREATE TABLE movies_shard_1 (
    content_id INT PRIMARY KEY CHECK (content_id BETWEEN 1 AND 1000000),
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    release_year INT,
    rating DECIMAL(3,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_genre_year (genre, release_year),
    INDEX idx_rating (rating DESC)
);
```

**Pros:** Simple, predictable, good for range queries
**Cons:** Hot spotting, uneven distribution

#### Hash-Based Sharding
**WhatsApp User Messages:**
```sql
-- Shard routing function: HASH(user_id) % num_shards

-- Java sharding logic
public class MessageShardingStrategy {
    private static final int NUM_SHARDS = 64;
    
    public int getShardId(String userId) {
        return Math.abs(userId.hashCode()) % NUM_SHARDS;
    }
    
    public String getShardTableName(String userId) {
        return "messages_shard_" + getShardId(userId);
    }
}

-- Example shard table
CREATE TABLE messages_shard_0 (
    message_id BIGINT PRIMARY KEY,
    chat_id VARCHAR(50) NOT NULL,
    sender_id VARCHAR(50) NOT NULL,
    content TEXT,
    message_type ENUM('text', 'image', 'video', 'file'),
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    INDEX idx_chat_sent (chat_id, sent_at DESC),
    INDEX idx_sender_sent (sender_id, sent_at DESC)
);
```

#### Directory-Based Sharding
**Uber Driver Location Tracking:**
```sql
-- Shard mapping table
CREATE TABLE shard_mapping (
    region_id VARCHAR(20) PRIMARY KEY,
    shard_id INT NOT NULL,
    shard_host VARCHAR(255) NOT NULL,
    shard_port INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Location data per shard
CREATE TABLE driver_locations_shard_1 (
    driver_id VARCHAR(50) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    region_id VARCHAR(20) NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (driver_id, last_updated),
    SPATIAL INDEX idx_location (latitude, longitude),
    INDEX idx_region_updated (region_id, last_updated)
);
```

## 2. Replication Patterns

### Master-Slave Replication
**Netflix User Profiles:**
```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    @Primary
    public DataSource masterDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://master-db:3306/netflix");
        config.setUsername("app_user");
        config.setPassword("secure_password");
        config.setMaximumPoolSize(20);
        return new HikariDataSource(config);
    }
    
    @Bean
    public DataSource slaveDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://slave-db:3306/netflix");
        config.setUsername("readonly_user");
        config.setPassword("readonly_password");
        config.setMaximumPoolSize(15);
        config.setReadOnly(true);
        return new HikariDataSource(config);
    }
}

@Service
public class UserProfileService {
    
    @Autowired
    @Qualifier("masterDataSource")
    private JdbcTemplate masterTemplate;
    
    @Autowired
    @Qualifier("slaveDataSource")
    private JdbcTemplate slaveTemplate;
    
    // Write operations go to master
    @Transactional
    public void updateUserProfile(String userId, UserProfile profile) {
        masterTemplate.update(
            "UPDATE user_profiles SET preferences = ?, updated_at = ? WHERE user_id = ?",
            profile.getPreferences(), Timestamp.from(Instant.now()), userId
        );
    }
    
    // Read operations can use slave
    @Transactional(readOnly = true)
    public UserProfile getUserProfile(String userId) {
        return slaveTemplate.queryForObject(
            "SELECT * FROM user_profiles WHERE user_id = ?",
            new Object[]{userId},
            new UserProfileRowMapper()
        );
    }
}
```

### Multi-Master Replication
**WhatsApp Global Message Synchronization:**
```sql
-- Conflict resolution with vector clocks
CREATE TABLE message_events (
    event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(50) NOT NULL,
    chat_id VARCHAR(50) NOT NULL,
    sender_id VARCHAR(50) NOT NULL,
    event_type ENUM('SENT', 'DELIVERED', 'READ', 'DELETED') NOT NULL,
    vector_clock JSON NOT NULL, -- {"dc1": 123, "dc2": 456, "dc3": 789}
    event_data JSON,
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    node_id VARCHAR(20) NOT NULL,
    UNIQUE KEY uk_message_event (message_id, event_type, node_id),
    INDEX idx_chat_occurred (chat_id, occurred_at DESC)
);
```

## 3. CQRS (Command Query Responsibility Segregation)

### Netflix Recommendation System
```java
// Command side - Write model
@Entity
@Table(name = "user_interactions")
public class UserInteraction {
    @Id
    private String interactionId;
    private String userId;
    private String contentId;
    private InteractionType type; // VIEW, LIKE, SHARE, RATE
    private Timestamp timestamp;
    private Map<String, Object> metadata;
    
    // Getters and setters
}

@Repository
public class UserInteractionCommandRepository {
    
    @Autowired
    private JdbcTemplate writeTemplate;
    
    @Transactional
    public void saveInteraction(UserInteraction interaction) {
        writeTemplate.update(
            "INSERT INTO user_interactions (interaction_id, user_id, content_id, type, timestamp, metadata) VALUES (?, ?, ?, ?, ?, ?)",
            interaction.getInteractionId(),
            interaction.getUserId(),
            interaction.getContentId(),
            interaction.getType().name(),
            interaction.getTimestamp(),
            new JsonWrapper(interaction.getMetadata())
        );
        
        // Publish event for read model updates
        eventPublisher.publishEvent(new InteractionCreatedEvent(interaction));
    }
}

// Query side - Read model (denormalized)
@Entity
@Table(name = "user_recommendation_profiles")
public class UserRecommendationProfile {
    @Id
    private String userId;
    private Map<String, Double> genrePreferences;
    private Map<String, Double> actorPreferences;
    private List<String> watchedContentIds;
    private Timestamp lastUpdated;
    
    // Optimized for fast reads
}

@Repository
public class RecommendationQueryRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate; // NoSQL for flexible schema
    
    public UserRecommendationProfile getRecommendationProfile(String userId) {
        return mongoTemplate.findById(userId, UserRecommendationProfile.class);
    }
    
    public List<ContentRecommendation> getRecommendations(String userId, int limit) {
        // Complex aggregation pipeline optimized for reads
        Aggregation aggregation = Aggregation.newAggregation(
            match(Criteria.where("userId").is(userId)),
            lookup("content_metadata", "recommendedContentIds", "contentId", "content"),
            sort(Sort.Direction.DESC, "score"),
            limit(limit)
        );
        
        return mongoTemplate.aggregate(aggregation, "recommendations", ContentRecommendation.class)
                           .getMappedResults();
    }
}
```

## 4. Event Sourcing Pattern

### Uber Trip Event Store
```java
// Event store schema
@Entity
@Table(name = "trip_events")
public class TripEvent {
    @Id
    private String eventId;
    private String tripId;
    private String eventType;
    private String eventData; // JSON
    private Long version;
    private Timestamp timestamp;
    private String causationId; // What caused this event
    private String correlationId; // Related events
    
    @Index(name = "idx_trip_version", columnList = "tripId, version")
    @Index(name = "idx_timestamp", columnList = "timestamp")
    // Additional indexes for performance
}

// Event types
public enum TripEventType {
    TRIP_REQUESTED,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVED,
    TRIP_STARTED,
    TRIP_COMPLETED,
    TRIP_CANCELLED,
    PAYMENT_PROCESSED
}

// Event sourcing repository
@Repository
public class TripEventStore {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Transactional
    public void appendEvent(TripEvent event) {
        // Optimistic concurrency control
        jdbcTemplate.update(
            "INSERT INTO trip_events (event_id, trip_id, event_type, event_data, version, timestamp, causation_id, correlation_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            event.getEventId(),
            event.getTripId(),
            event.getEventType(),
            event.getEventData(),
            event.getVersion(),
            event.getTimestamp(),
            event.getCausationId(),
            event.getCorrelationId()
        );
    }
    
    public List<TripEvent> getEventsByTripId(String tripId) {
        return jdbcTemplate.query(
            "SELECT * FROM trip_events WHERE trip_id = ? ORDER BY version ASC",
            new Object[]{tripId},
            new TripEventRowMapper()
        );
    }
    
    public TripSnapshot rebuildTripFromEvents(String tripId) {
        List<TripEvent> events = getEventsByTripId(tripId);
        
        TripSnapshot snapshot = new TripSnapshot();
        for (TripEvent event : events) {
            snapshot = applyEvent(snapshot, event);
        }
        
        return snapshot;
    }
}
```

## 5. Polyglot Persistence

### Multi-Database Architecture
```java
// Netflix microservices data strategy
@Configuration
public class PolyglotPersistenceConfig {
    
    // User profiles - PostgreSQL (ACID, complex queries)
    @Bean
    @Primary
    public DataSource userProfileDataSource() {
        return DataSourceBuilder.create()
            .driverClassName("org.postgresql.Driver")
            .url("jdbc:postgresql://postgres:5432/user_profiles")
            .build();
    }
    
    // Content metadata - MongoDB (flexible schema)
    @Bean
    public MongoTemplate contentMongoTemplate() {
        return new MongoTemplate(new SimpleMongoClientDbFactory(
            "mongodb://mongo:27017/content_metadata"
        ));
    }
    
    // Real-time analytics - Redis (fast access)
    @Bean
    public RedisTemplate<String, Object> analyticsRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(new JedisConnectionFactory());
        return template;
    }
    
    // Search - Elasticsearch (full-text search)
    @Bean
    public ElasticsearchRestTemplate searchTemplate() {
        return new ElasticsearchRestTemplate(
            RestClients.create(ClientConfiguration.localhost())
        );
    }
    
    // Graph relationships - Neo4j (social connections)
    @Bean
    public Neo4jTemplate neo4jTemplate() {
        return new Neo4jTemplate(new Neo4jMappingContext(), neo4jClient());
    }
}

// Service using multiple data stores
@Service
public class ContentService {
    
    @Autowired
    private JdbcTemplate userProfileTemplate;
    
    @Autowired
    private MongoTemplate contentTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> analyticsTemplate;
    
    @Autowired
    private ElasticsearchRestTemplate searchTemplate;
    
    public ContentRecommendation getPersonalizedRecommendations(String userId) {
        // 1. Get user preferences from PostgreSQL
        UserProfile profile = getUserProfile(userId);
        
        // 2. Get real-time viewing data from Redis
        ViewingSession session = getCurrentViewingSession(userId);
        
        // 3. Search content from Elasticsearch
        List<Content> candidates = searchContent(profile.getPreferences());
        
        // 4. Get social recommendations from Neo4j
        List<Content> socialRecs = getSocialRecommendations(userId);
        
        // 5. Combine and rank recommendations
        return rankRecommendations(candidates, socialRecs, session);
    }
}
```

## 6. Data Versioning and Migration

### Schema Evolution Strategy
```sql
-- Migration scripts with versioning
-- V001__initial_schema.sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- V002__add_user_preferences.sql
ALTER TABLE users ADD COLUMN preferences JSON;
CREATE INDEX idx_users_preferences ON users ((CAST(preferences->'$.language' AS CHAR(10))));

-- V003__partition_user_activities.sql
CREATE TABLE user_activities (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    activity_data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

### Backward Compatibility Pattern
```java
// API versioning for database changes
@Entity
@Table(name = "content_metadata")
public class ContentMetadata {
    @Id
    private String contentId;
    
    // V1 fields
    private String title;
    private String description;
    
    // V2 fields (added later)
    @Column(name = "extended_metadata")
    private String extendedMetadataJson;
    
    // V3 fields (latest)
    @Column(name = "ai_generated_tags")
    private String aiGeneratedTags;
    
    // Version-aware getters
    public Map<String, Object> getMetadataForVersion(int apiVersion) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Always include V1 fields
        metadata.put("title", title);
        metadata.put("description", description);
        
        // Include V2 fields for version 2+
        if (apiVersion >= 2 && extendedMetadataJson != null) {
            metadata.putAll(parseJsonMetadata(extendedMetadataJson));
        }
        
        // Include V3 fields for version 3+
        if (apiVersion >= 3 && aiGeneratedTags != null) {
            metadata.put("aiTags", parseJsonArray(aiGeneratedTags));
        }
        
        return metadata;
    }
}
```

## 7. Database Performance Optimization

### Index Strategies
```sql
-- Netflix content discovery optimization
CREATE TABLE content_catalog (
    content_id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    subgenre VARCHAR(100),
    release_year INT NOT NULL,
    rating DECIMAL(3,1),
    duration_minutes INT,
    content_type ENUM('movie', 'series', 'documentary'),
    country_code CHAR(2),
    language_code CHAR(2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Composite indexes for common query patterns
CREATE INDEX idx_genre_year_rating ON content_catalog (genre, release_year DESC, rating DESC);
CREATE INDEX idx_type_country_year ON content_catalog (content_type, country_code, release_year DESC);
CREATE INDEX idx_language_genre ON content_catalog (language_code, genre, rating DESC);

-- Covering index for frequently accessed data
CREATE INDEX idx_catalog_summary ON content_catalog (genre, release_year, rating) 
INCLUDE (title, duration_minutes, content_type);

-- Partial index for active content only
CREATE INDEX idx_active_content ON content_catalog (genre, rating DESC) 
WHERE created_at > NOW() - INTERVAL 5 YEAR;
```

### Query Optimization Patterns
```java
// Uber ride history with pagination
@Repository
public class RideHistoryRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // Cursor-based pagination for large datasets
    public List<Ride> getRideHistory(String userId, String cursor, int limit) {
        String sql = """
            SELECT ride_id, start_time, end_time, start_location, end_location, fare
            FROM rides 
            WHERE user_id = ? 
            AND (? IS NULL OR ride_id < ?)
            ORDER BY ride_id DESC 
            LIMIT ?
            """;
        
        return jdbcTemplate.query(sql, 
            new Object[]{userId, cursor, cursor, limit},
            new RideRowMapper());
    }
    
    // Batch processing for analytics
    @Async
    public CompletableFuture<Map<String, Object>> calculateRideStatistics(String userId) {
        String sql = """
            SELECT 
                COUNT(*) as total_rides,
                AVG(fare) as avg_fare,
                SUM(fare) as total_spent,
                AVG(TIMESTAMPDIFF(MINUTE, start_time, end_time)) as avg_duration
            FROM rides 
            WHERE user_id = ? 
            AND start_time >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
            """;
        
        Map<String, Object> stats = jdbcTemplate.queryForMap(sql, userId);
        return CompletableFuture.completedFuture(stats);
    }
}
```

## 8. Consistency Patterns

### Eventual Consistency
```java
// WhatsApp message delivery with eventual consistency
@Service
public class MessageDeliveryService {
    
    @Autowired
    private MessageEventPublisher eventPublisher;
    
    @Transactional
    public void deliverMessage(String messageId, String recipientId) {
        // Local transaction - immediately consistent
        updateLocalDeliveryStatus(messageId, recipientId, "DELIVERED");
        
        // Async propagation - eventually consistent
        eventPublisher.publishDeliveryEvent(
            new MessageDeliveryEvent(messageId, recipientId, "DELIVERED")
        );
        
        // Async notification - fire and forget
        CompletableFuture.runAsync(() -> 
            sendPushNotification(recipientId, messageId)
        );
    }
    
    @EventListener
    public void handleDeliveryEvent(MessageDeliveryEvent event) {
        // Propagate to other replicas
        replicateToOtherDataCenters(event);
        
        // Update analytics store
        updateAnalyticsStore(event);
        
        // Update caches
        invalidateMessageCache(event.getMessageId());
    }
}
```

### Saga Pattern for Distributed Transactions
```java
// Uber trip booking saga
@Component
public class TripBookingSaga {
    
    @SagaOrchestrationStart
    public void startTripBooking(TripBookingCommand command) {
        // Step 1: Reserve driver
        commandGateway.send(new ReserveDriverCommand(
            command.getDriverId(), command.getTripId()
        ));
    }
    
    @SagaHandler
    public void handle(DriverReservedEvent event) {
        // Step 2: Process payment
        commandGateway.send(new ProcessPaymentCommand(
            event.getTripId(), event.getEstimatedFare()
        ));
    }
    
    @SagaHandler
    public void handle(PaymentProcessedEvent event) {
        // Step 3: Confirm trip
        commandGateway.send(new ConfirmTripCommand(event.getTripId()));
    }
    
    @SagaHandler
    public void handle(PaymentFailedEvent event) {
        // Compensating action: Release driver
        commandGateway.send(new ReleaseDriverCommand(
            event.getTripId()
        ));
        
        // Notify user of failure
        commandGateway.send(new NotifyUserCommand(
            event.getUserId(), "Payment failed. Please try again."
        ));
    }
}
```

## Best Practices Summary

1. **Choose the Right Consistency Model**: Strong consistency for financial data, eventual consistency for social features
2. **Design for Scale**: Use sharding and replication from the beginning
3. **Optimize for Access Patterns**: Design indexes based on actual query patterns
4. **Plan for Growth**: Use partition-friendly schemas and auto-scaling strategies
5. **Monitor Performance**: Track query performance, index usage, and replication lag
6. **Handle Failures Gracefully**: Implement circuit breakers and fallback strategies
7. **Use Appropriate Data Models**: Relational for ACID, NoSQL for flexibility, Graph for relationships
8. **Version Your Schemas**: Plan for backward compatibility and gradual migrations
