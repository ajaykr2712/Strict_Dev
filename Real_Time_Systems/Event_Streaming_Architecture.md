# Advanced Event Streaming Architecture

## Overview
Event streaming architecture patterns for building real-time, scalable systems like those used by Netflix, Uber, and modern fintech companies.

## Key Concepts

### Event Streaming Fundamentals
- **Event**: Immutable record of something that happened
- **Stream**: Continuous flow of events
- **Topic**: Named stream of events
- **Partition**: Ordered sequence within a topic
- **Consumer Group**: Set of consumers processing events in parallel

### Event Sourcing vs Event Streaming

```
Event Sourcing:
- Store events as the source of truth
- Rebuild state by replaying events
- Focus on data persistence

Event Streaming:
- Real-time processing of event flows
- Multiple consumers of same events
- Focus on data in motion
```

## Architecture Patterns

### 1. Lambda Architecture
```
Batch Layer (Historical data)
    ↓
Speed Layer (Real-time) → Serving Layer → Query Interface
    ↓
Data Sources
```

**Use Cases:**
- Real-time analytics (Netflix viewing statistics)
- Fraud detection (Uber payment processing)
- Recommendation engines

### 2. Kappa Architecture
```
Stream Processing Only
Data Sources → Stream → Processing → Serving → Query
```

**Benefits:**
- Simplified architecture
- Single processing paradigm
- Lower latency

### 3. Event-Driven Microservices
```
Service A → Event Bus → Service B
    ↓           ↓         ↓
  Event      Router   Consumer
Producer              Groups
```

## Technology Stack

### Apache Kafka
```yaml
# Kafka Configuration
server:
  bootstrap.servers: kafka1:9092,kafka2:9092,kafka3:9092
  
producer:
  acks: all
  retries: 2147483647
  max.in.flight.requests.per.connection: 5
  enable.idempotence: true
  
consumer:
  auto.offset.reset: earliest
  enable.auto.commit: false
  max.poll.records: 500
```

### Apache Pulsar
```yaml
# Pulsar Configuration
webServiceUrl: http://localhost:8080
serviceUrl: pulsar://localhost:6650

tenant: public
namespace: default

producer:
  topic: persistent://public/default/events
  batching: true
  batchingMaxPublishDelay: 10ms
```

### AWS Kinesis
```yaml
# Kinesis Configuration
stream:
  name: event-stream
  shards: 10
  retention: 24 hours
  
producer:
  aggregation: true
  compression: gzip
  
consumer:
  checkpoint_interval: 5000ms
  shard_iterator_type: TRIM_HORIZON
```

## Event Schema Design

### Schema Evolution
```json
{
  "namespace": "com.company.events",
  "type": "record",
  "name": "UserEvent",
  "version": "v2",
  "fields": [
    {"name": "userId", "type": "string"},
    {"name": "eventType", "type": "string"},
    {"name": "timestamp", "type": "long"},
    {"name": "data", "type": "map", "values": "string"},
    {"name": "schemaVersion", "type": "string", "default": "v2"}
  ]
}
```

### Event Types
```json
{
  "userEvents": {
    "registration": {
      "userId": "string",
      "email": "string",
      "timestamp": "long"
    },
    "login": {
      "userId": "string",
      "sessionId": "string",
      "timestamp": "long"
    }
  },
  
  "orderEvents": {
    "created": {
      "orderId": "string",
      "userId": "string",
      "amount": "decimal",
      "timestamp": "long"
    },
    "completed": {
      "orderId": "string",
      "status": "string",
      "timestamp": "long"
    }
  }
}
```

## Stream Processing Patterns

### 1. Filtering and Routing
```sql
-- Kafka Streams DSL
KStream<String, UserEvent> userEvents = builder.stream("user-events");

KStream<String, UserEvent> premiumUsers = userEvents
    .filter((key, event) -> event.getUserType().equals("PREMIUM"));

premiumUsers.to("premium-user-events");
```

### 2. Aggregation and Windowing
```sql
-- Stream aggregation example
KTable<Windowed<String>, Long> eventCounts = userEvents
    .groupByKey()
    .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
    .count();
```

### 3. Join Operations
```sql
-- Stream-Stream Join
KStream<String, OrderEvent> orders = builder.stream("orders");
KStream<String, PaymentEvent> payments = builder.stream("payments");

KStream<String, EnrichedOrder> enrichedOrders = orders
    .join(payments,
          (order, payment) -> new EnrichedOrder(order, payment),
          JoinWindows.of(Duration.ofMinutes(5)));
```

### 4. Event Deduplication
```java
// Exactly-once semantics
Properties props = new Properties();
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, 
          StreamsConfig.EXACTLY_ONCE_V2);

// Idempotent producer
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

## Real-World Implementation Examples

### Netflix Event Streaming
```
Content Events → Kafka → 
    ├── Viewing Analytics Service
    ├── Recommendation Engine
    ├── A/B Testing Platform
    └── Personalization Service
```

**Key Patterns:**
- Multi-region replication
- Schema registry for evolution
- Dead letter queues for error handling

### Uber Real-Time Platform
```
Ride Events → Apache Kafka → 
    ├── Surge Pricing Service
    ├── Driver Matching Service
    ├── Analytics Pipeline
    └── Fraud Detection
```

**Architecture Components:**
- Event sourcing for trip lifecycle
- CQRS for read/write separation
- Saga pattern for distributed transactions

### WhatsApp Message Streaming
```
Message Events → Custom Message Broker →
    ├── Delivery Service
    ├── Push Notification Service
    ├── Analytics Service
    └── Compliance Service
```

## Error Handling and Reliability

### Dead Letter Queues
```java
// Configure DLQ
Properties props = new Properties();
props.put("default.deserialization.exception.handler", 
          "org.apache.kafka.streams.errors.LogAndContinueExceptionHandler");

// Custom error handling
KStream<String, String> events = builder.stream("input-topic");
events.mapValues(value -> {
    try {
        return processEvent(value);
    } catch (Exception e) {
        // Send to DLQ
        dlqProducer.send(new ProducerRecord<>("dlq-topic", value));
        return null;
    }
}).filter((key, value) -> value != null);
```

### Circuit Breaker Pattern
```java
// Resilience4j circuit breaker
CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("eventProcessor");

Supplier<String> decoratedSupplier = CircuitBreaker
    .decorateSupplier(circuitBreaker, this::processEvent);

String result = decoratedSupplier.get();
```

### Retry Mechanisms
```java
// Exponential backoff retry
RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)
    .waitDuration(Duration.ofMillis(500))
    .retryOnException(throwable -> throwable instanceof TimeoutException)
    .build();

Retry retry = Retry.of("eventRetry", config);
Supplier<String> retryableSupplier = Retry.decorateSupplier(retry, this::processEvent);
```

## Monitoring and Observability

### Key Metrics
```yaml
Producer Metrics:
  - record-send-rate
  - record-error-rate
  - request-latency-avg
  - batch-size-avg

Consumer Metrics:
  - records-consumed-rate
  - records-lag-max
  - commit-latency-avg
  - fetch-latency-avg

Stream Processing:
  - processing-rate
  - error-rate
  - state-store-size
  - punctuate-latency
```

### Distributed Tracing
```java
// OpenTelemetry tracing
Span span = tracer.nextSpan()
    .name("event-processing")
    .tag("event.type", event.getType())
    .tag("partition", partition)
    .start();

try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
    processEvent(event);
} finally {
    span.end();
}
```

## Performance Optimization

### Throughput Optimization
```properties
# Producer tuning
batch.size=65536
linger.ms=5
compression.type=snappy
buffer.memory=33554432

# Consumer tuning
fetch.min.bytes=50000
fetch.max.wait.ms=500
max.partition.fetch.bytes=1048576
```

### Latency Optimization
```properties
# Low latency settings
batch.size=0
linger.ms=0
acks=1
compression.type=none

# Consumer settings
fetch.min.bytes=1
fetch.max.wait.ms=0
```

### Memory Management
```java
// Stream processing state stores
StoreBuilder<KeyValueStore<String, Long>> storeBuilder = 
    Stores.keyValueStoreBuilder(
        Stores.persistentKeyValueStore("event-counts"),
        Serdes.String(),
        Serdes.Long())
    .withCachingEnabled()
    .withLoggingEnabled(Collections.emptyMap());
```

## Security Considerations

### Authentication and Authorization
```properties
# SASL/SCRAM authentication
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-256
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required \
    username="user" password="password";

# SSL configuration
ssl.truststore.location=/path/to/truststore.jks
ssl.truststore.password=password
ssl.keystore.location=/path/to/keystore.jks
ssl.keystore.password=password
```

### Data Encryption
```java
// Message encryption
public class EncryptedEventSerializer implements Serializer<Event> {
    private final AESUtil encryptor;
    
    @Override
    public byte[] serialize(String topic, Event event) {
        byte[] serialized = jsonSerializer.serialize(topic, event);
        return encryptor.encrypt(serialized);
    }
}
```

## Best Practices

### Schema Design
1. **Use schema registry** for evolution management
2. **Version your schemas** with backward compatibility
3. **Include metadata** (timestamp, source, version)
4. **Use Avro or Protobuf** for efficient serialization

### Partitioning Strategy
1. **Partition by entity ID** for ordering guarantees
2. **Avoid hot partitions** with even distribution
3. **Consider downstream processing** requirements
4. **Plan for scaling** with adequate partitions

### Error Handling
1. **Implement dead letter queues** for poison messages
2. **Use circuit breakers** for external dependencies
3. **Configure appropriate timeouts** and retries
4. **Monitor error rates** and alert thresholds

### Testing Strategies
1. **Unit test** stream processors with TopologyTestDriver
2. **Integration test** with embedded Kafka
3. **Contract test** schema evolution
4. **Load test** throughput and latency characteristics
