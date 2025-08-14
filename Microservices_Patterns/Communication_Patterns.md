# Microservices Communication Patterns

This document outlines essential communication patterns for microservices architecture, with real-world examples from companies like Netflix, Uber, and WhatsApp.

## 1. Synchronous Communication Patterns

### Request-Response Pattern
**Description:** Direct communication between services using HTTP/REST or RPC.

**Netflix Example:**
- User service calls Recommendation service for personalized content
- API Gateway orchestrates calls to multiple services for dashboard data

**Implementation Considerations:**
```java
// Service-to-service communication with resilience
@Component
public class RecommendationServiceClient {
    
    @Retryable(value = {Exception.class}, maxAttempts = 3)
    @CircuitBreaker(name = "recommendation-service")
    public List<Movie> getRecommendations(String userId) {
        return restTemplate.getForObject(
            "/recommendations/" + userId, 
            List.class
        );
    }
}
```

**Best Practices:**
- Implement circuit breakers for fault tolerance
- Use connection pooling and timeouts
- Add retry mechanisms with exponential backoff
- Implement proper error handling and fallbacks

### API Gateway Pattern
**Description:** Single entry point for all client requests, routing to appropriate microservices.

**Uber Example:**
- Mobile app communicates through API Gateway
- Gateway handles authentication, rate limiting, and request routing
- Routes ride requests to Driver Service, Payment Service, and Notification Service

**Benefits:**
- Centralized cross-cutting concerns (auth, logging, monitoring)
- Request aggregation and transformation
- Protocol translation (HTTP to gRPC)
- Load balancing and service discovery

## 2. Asynchronous Communication Patterns

### Event-Driven Architecture
**Description:** Services communicate through events published to message brokers.

**WhatsApp Example:**
- Message sent event triggers multiple services:
  - Delivery Service (marks as delivered)
  - Notification Service (push notification)
  - Analytics Service (usage metrics)
  - Backup Service (message archival)

**Implementation:**
```java
// Event publishing
@EventListener
public void handleMessageSent(MessageSentEvent event) {
    // Publish to multiple downstream services
    eventPublisher.publishEvent(new DeliveryTrackingEvent(event));
    eventPublisher.publishEvent(new NotificationEvent(event));
    eventPublisher.publishEvent(new AnalyticsEvent(event));
}
```

### Message Queue Pattern
**Description:** Asynchronous communication using message queues for decoupling.

**Uber Example:**
- Trip completion triggers:
  - Payment processing (RabbitMQ)
  - Driver rating request
  - Receipt generation
  - Analytics data collection

**Queue Types:**
- **Point-to-Point:** Single consumer processes each message
- **Publish-Subscribe:** Multiple consumers receive each message
- **Topic-based:** Messages routed based on topics/routing keys

### Saga Pattern
**Description:** Managing distributed transactions across multiple services.

**Netflix Billing Example:**
```java
// Choreography-based saga
public class SubscriptionSaga {
    
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        // Activate subscription
        subscriptionService.activateSubscription(event.getUserId());
        
        // If activation fails, compensate
        if (!activated) {
            paymentService.refundPayment(event.getPaymentId());
        }
    }
}
```

## 3. Data Consistency Patterns

### Event Sourcing
**Description:** Store all changes as a sequence of events rather than current state.

**Uber Trip Example:**
- Trip requested event
- Driver assigned event
- Trip started event
- Trip completed event
- Payment processed event

**Benefits:**
- Complete audit trail
- Ability to rebuild state from events
- Time-travel debugging
- Support for multiple read models

### CQRS (Command Query Responsibility Segregation)
**Description:** Separate read and write models for better performance and scalability.

**Netflix Content Management:**
- **Command Side:** Content ingestion, metadata updates
- **Query Side:** Search, recommendations, content discovery
- Different databases optimized for each use case

## 4. Service Discovery Patterns

### Client-Side Discovery
**Description:** Client is responsible for determining service locations.

**Netflix Eureka Example:**
```java
@Component
public class ServiceDiscoveryClient {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    public String getServiceUrl(String serviceName) {
        List<ServiceInstance> instances = 
            discoveryClient.getInstances(serviceName);
        
        return loadBalancer.choose(instances).getUri().toString();
    }
}
```

### Server-Side Discovery
**Description:** Load balancer handles service discovery and routing.

**Benefits:**
- Simpler client implementation
- Centralized load balancing logic
- Better control over routing strategies

## 5. Resilience Patterns

### Circuit Breaker
**Description:** Prevent cascading failures by stopping calls to failing services.

**Implementation States:**
- **Closed:** Normal operation, requests pass through
- **Open:** Service is failing, requests fail immediately
- **Half-Open:** Testing if service has recovered

### Bulkhead Pattern
**Description:** Isolate critical resources to prevent total system failure.

**Uber Example:**
- Separate thread pools for:
  - Critical ride matching operations
  - Non-critical analytics
  - Background batch processing

### Timeout and Retry
**Description:** Set appropriate timeouts and implement smart retry logic.

```java
@Component
public class ResilientServiceClient {
    
    @Retryable(
        value = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @TimeLimiter(name = "user-service", fallbackMethod = "fallbackGetUser")
    public User getUser(String userId) {
        return userServiceClient.getUser(userId);
    }
    
    public User fallbackGetUser(String userId, Exception ex) {
        return User.defaultUser();
    }
}
```

## 6. Performance Optimization Patterns

### Caching Strategies
**WhatsApp Message Caching:**
- **L1 Cache:** Recent messages in memory
- **L2 Cache:** Redis for frequently accessed messages
- **L3 Cache:** Database with proper indexing

### Database per Service
**Benefits:**
- Technology diversity
- Independent scaling
- Failure isolation
- Team autonomy

**Challenges:**
- Data consistency
- Cross-service queries
- Transaction management

## 7. Monitoring and Observability

### Distributed Tracing
**Implementation:**
```java
@RestController
public class OrderController {
    
    @GetMapping("/orders/{id}")
    @Traced(operationName = "get-order")
    public Order getOrder(@PathVariable String id) {
        Span span = tracer.nextSpan()
            .name("order-retrieval")
            .tag("order.id", id)
            .start();
        
        try {
            return orderService.getOrder(id);
        } finally {
            span.end();
        }
    }
}
```

### Health Checks
**Multi-level Health Checks:**
- **Shallow:** Service is responding
- **Deep:** Dependencies are healthy
- **Business:** Core functionality works

## 8. Security Patterns

### Token-based Authentication
**JWT Implementation:**
```java
@Component
public class JwtTokenProvider {
    
    public String createToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
}
```

### API Rate Limiting
**Distributed Rate Limiting:**
- Token bucket algorithm
- Redis-based counters
- Per-user and per-service limits

## Real-World Implementation Guidelines

### Netflix Approach
1. **API Gateway:** Zuul for request routing
2. **Service Discovery:** Eureka for service registration
3. **Circuit Breaker:** Hystrix for fault tolerance
4. **Monitoring:** Custom dashboards and alerting

### Uber's Strategy
1. **Event-Driven:** Kafka for real-time event processing
2. **Microservices:** Domain-driven service boundaries
3. **Polyglot Persistence:** Different databases for different needs
4. **Distributed Tracing:** Jaeger for request tracing

### WhatsApp's Pattern
1. **Message Queues:** High-throughput message processing
2. **Caching:** Multi-level caching for message delivery
3. **Load Balancing:** Geographic distribution
4. **Data Replication:** Cross-region message storage

## Best Practices Summary

1. **Design for Failure:** Assume services will fail
2. **Loose Coupling:** Minimize service dependencies
3. **High Cohesion:** Group related functionality
4. **Autonomous Teams:** Enable independent development
5. **Continuous Monitoring:** Observe system behavior
6. **Gradual Rollouts:** Deploy changes incrementally
7. **Documentation:** Keep communication patterns documented
8. **Testing:** Include integration and contract testing
