# Java Design Patterns for System Architecture

## Overview
This guide provides Java implementations of design patterns that are essential for building robust, scalable distributed systems. Based on the system design patterns analyzed, we'll focus on patterns that directly support modern architectures like microservices, event-driven systems, and resilient distributed applications.

---

## 1. Singleton Pattern

### Purpose
Ensures a class has only one instance and provides global access to it. Critical for managing shared resources like configuration managers, connection pools, and event stores.

### Use Case in System Design
- Database connection pools
- Configuration managers
- Event store instances
- Circuit breaker registry

### Java Implementation

```java
/**
 * Thread-safe Singleton using enum (Effective Java approach)
 * Best for event store or configuration manager
 */
public enum EventStoreManager {
    INSTANCE;
    
    private final EventStore eventStore;
    
    EventStoreManager() {
        this.eventStore = new InMemoryEventStore();
    }
    
    public EventStore getEventStore() {
        return eventStore;
    }
    
    public void appendEvent(DomainEvent event) {
        eventStore.append(event);
    }
}

/**
 * Alternative: Thread-safe Singleton with double-checked locking
 * Use when you need constructor parameters
 */
public class CircuitBreakerRegistry {
    private static volatile CircuitBreakerRegistry instance;
    private final Map<String, CircuitBreaker> circuitBreakers;
    
    private CircuitBreakerRegistry() {
        this.circuitBreakers = new ConcurrentHashMap<>();
    }
    
    public static CircuitBreakerRegistry getInstance() {
        if (instance == null) {
            synchronized (CircuitBreakerRegistry.class) {
                if (instance == null) {
                    instance = new CircuitBreakerRegistry();
                }
            }
        }
        return instance;
    }
    
    public CircuitBreaker getCircuitBreaker(String serviceName) {
        return circuitBreakers.computeIfAbsent(serviceName, 
            k -> new CircuitBreaker(serviceName));
    }
}
```

---

## 2. Factory Pattern

### Purpose
Creates objects without specifying their exact classes. Essential for creating different types of events, commands, and handlers based on runtime conditions.

### Use Case in System Design
- Creating domain events
- Command factory for CQRS
- Handler factory for different event types
- Service factory for microservices

### Java Implementation

```java
/**
 * Abstract Factory for Domain Events
 * Supports Event Sourcing pattern
 */
public abstract class DomainEventFactory {
    
    public static DomainEvent createEvent(String eventType, String aggregateId, Map<String, Object> data) {
        switch (eventType) {
            case "OrderCreated":
                return new OrderCreatedEvent(aggregateId, data);
            case "OrderShipped":
                return new OrderShippedEvent(aggregateId, data);
            case "PaymentProcessed":
                return new PaymentProcessedEvent(aggregateId, data);
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }
}

/**
 * Factory Method for Command Handlers (CQRS Pattern)
 */
public abstract class CommandHandlerFactory {
    
    public static CommandHandler<?> createHandler(Command command) {
        if (command instanceof CreateOrderCommand) {
            return new CreateOrderCommandHandler();
        } else if (command instanceof UpdateOrderCommand) {
            return new UpdateOrderCommandHandler();
        } else if (command instanceof ProcessPaymentCommand) {
            return new ProcessPaymentCommandHandler();
        }
        throw new IllegalArgumentException("No handler found for command: " + command.getClass());
    }
}

/**
 * Service Factory for Microservices
 * Supports Service Discovery pattern
 */
public class ServiceFactory {
    private final ServiceRegistry serviceRegistry;
    
    public ServiceFactory(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
    
    public <T> T createService(Class<T> serviceClass, String serviceName) {
        String serviceUrl = serviceRegistry.getServiceUrl(serviceName);
        
        if (PaymentService.class.equals(serviceClass)) {
            return serviceClass.cast(new PaymentServiceImpl(serviceUrl));
        } else if (InventoryService.class.equals(serviceClass)) {
            return serviceClass.cast(new InventoryServiceImpl(serviceUrl));
        }
        
        throw new IllegalArgumentException("Unknown service: " + serviceClass);
    }
}
```

---

## 3. Observer Pattern

### Purpose
Defines a one-to-many dependency between objects so that when one object changes state, all dependents are automatically notified. Perfect for event-driven architectures.

### Use Case in System Design
- Event handling in Event-Driven Architecture
- Saga pattern coordination
- CQRS read model updates
- Microservice communication

### Java Implementation

```java
/**
 * Event Bus implementation using Observer Pattern
 * Core component for Event-Driven Architecture
 */
public interface EventObserver<T extends DomainEvent> {
    void handle(T event);
}

public class EventBus {
    private final Map<Class<?>, List<EventObserver<?>>> observers = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventObserver<T> observer) {
        observers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(observer);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void publish(T event) {
        List<EventObserver<?>> eventObservers = observers.get(event.getClass());
        if (eventObservers != null) {
            for (EventObserver<?> observer : eventObservers) {
                // Async processing for better performance
                executorService.submit(() -> {
                    try {
                        ((EventObserver<T>) observer).handle(event);
                    } catch (Exception e) {
                        // Log error and continue processing other observers
                        System.err.println("Error processing event: " + e.getMessage());
                    }
                });
            }
        }
    }
}

/**
 * CQRS Read Model Updater using Observer Pattern
 */
public class OrderReadModelUpdater implements EventObserver<OrderEvent> {
    private final OrderReadModel readModel;
    
    public OrderReadModelUpdater(OrderReadModel readModel) {
        this.readModel = readModel;
    }
    
    @Override
    public void handle(OrderEvent event) {
        switch (event.getEventType()) {
            case "OrderCreated":
                readModel.createOrderProjection((OrderCreatedEvent) event);
                break;
            case "OrderShipped":
                readModel.updateOrderStatus((OrderShippedEvent) event);
                break;
            default:
                System.out.println("Unhandled event type: " + event.getEventType());
        }
    }
}

/**
 * Saga Participant using Observer Pattern
 */
public class PaymentSagaParticipant implements EventObserver<DomainEvent> {
    private final PaymentService paymentService;
    private final EventBus eventBus;
    
    public PaymentSagaParticipant(PaymentService paymentService, EventBus eventBus) {
        this.paymentService = paymentService;
        this.eventBus = eventBus;
    }
    
    @Override
    public void handle(DomainEvent event) {
        if (event instanceof OrderCreatedEvent) {
            processPayment((OrderCreatedEvent) event);
        } else if (event instanceof PaymentFailedEvent) {
            compensatePayment((PaymentFailedEvent) event);
        }
    }
    
    private void processPayment(OrderCreatedEvent event) {
        try {
            paymentService.processPayment(event.getOrderId(), event.getAmount());
            eventBus.publish(new PaymentProcessedEvent(event.getOrderId()));
        } catch (Exception e) {
            eventBus.publish(new PaymentFailedEvent(event.getOrderId(), e.getMessage()));
        }
    }
    
    private void compensatePayment(PaymentFailedEvent event) {
        // Implement compensation logic
        paymentService.refundPayment(event.getOrderId());
    }
}
```

---

## 4. Strategy Pattern

### Purpose
Defines a family of algorithms, encapsulates each one, and makes them interchangeable. Essential for handling different business rules and fallback strategies.

### Use Case in System Design
- Circuit breaker fallback strategies
- Different event processing strategies
- Load balancing algorithms
- Retry strategies

### Java Implementation

```java
/**
 * Circuit Breaker Fallback Strategy
 */
public interface FallbackStrategy<T> {
    T execute();
}

public class CachedResponseFallback implements FallbackStrategy<OrderResponse> {
    private final OrderCache cache;
    private final String orderId;
    
    public CachedResponseFallback(OrderCache cache, String orderId) {
        this.cache = cache;
        this.orderId = orderId;
    }
    
    @Override
    public OrderResponse execute() {
        return cache.getCachedOrder(orderId);
    }
}

public class DefaultValueFallback implements FallbackStrategy<OrderResponse> {
    @Override
    public OrderResponse execute() {
        return OrderResponse.defaultResponse();
    }
}

/**
 * Circuit Breaker with Strategy Pattern
 */
public class CircuitBreaker {
    private CircuitState state = CircuitState.CLOSED;
    private int failureCount = 0;
    private long lastFailureTime;
    private final int failureThreshold;
    private final long timeout;
    private FallbackStrategy<?> fallbackStrategy;
    
    public CircuitBreaker(int failureThreshold, long timeout) {
        this.failureThreshold = failureThreshold;
        this.timeout = timeout;
    }
    
    public void setFallbackStrategy(FallbackStrategy<?> fallbackStrategy) {
        this.fallbackStrategy = fallbackStrategy;
    }
    
    public <T> T execute(Supplier<T> operation) {
        if (state == CircuitState.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > timeout) {
                state = CircuitState.HALF_OPEN;
            } else {
                return executeFallback();
            }
        }
        
        try {
            T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            return executeFallback();
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T executeFallback() {
        if (fallbackStrategy != null) {
            return (T) fallbackStrategy.execute();
        }
        throw new RuntimeException("Service unavailable and no fallback configured");
    }
    
    private void onSuccess() {
        failureCount = 0;
        state = CircuitState.CLOSED;
    }
    
    private void onFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
        if (failureCount >= failureThreshold) {
            state = CircuitState.OPEN;
        }
    }
}

/**
 * Event Processing Strategy for different event types
 */
public interface EventProcessingStrategy {
    void processEvent(DomainEvent event);
}

public class BatchEventProcessingStrategy implements EventProcessingStrategy {
    private final List<DomainEvent> eventBatch = new ArrayList<>();
    private final int batchSize;
    
    public BatchEventProcessingStrategy(int batchSize) {
        this.batchSize = batchSize;
    }
    
    @Override
    public void processEvent(DomainEvent event) {
        eventBatch.add(event);
        if (eventBatch.size() >= batchSize) {
            processBatch();
            eventBatch.clear();
        }
    }
    
    private void processBatch() {
        // Process events in batch for better performance
        System.out.println("Processing batch of " + eventBatch.size() + " events");
    }
}

public class RealTimeEventProcessingStrategy implements EventProcessingStrategy {
    @Override
    public void processEvent(DomainEvent event) {
        // Process immediately for real-time requirements
        System.out.println("Processing event immediately: " + event.getEventType());
    }
}
```

---

## 5. Command Pattern

### Purpose
Encapsulates a request as an object, allowing you to parameterize clients with different requests, queue operations, and support undo operations. Perfect for CQRS architecture.

### Use Case in System Design
- CQRS command handling
- Saga step execution
- Event sourcing command processing
- Distributed transaction management

### Java Implementation

```java
/**
 * Command Pattern for CQRS Architecture
 */
public interface Command {
    String getCommandId();
    String getAggregateId();
    LocalDateTime getTimestamp();
}

public abstract class BaseCommand implements Command {
    private final String commandId;
    private final String aggregateId;
    private final LocalDateTime timestamp;
    
    protected BaseCommand(String aggregateId) {
        this.commandId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.timestamp = LocalDateTime.now();
    }
    
    @Override
    public String getCommandId() { return commandId; }
    
    @Override
    public String getAggregateId() { return aggregateId; }
    
    @Override
    public LocalDateTime getTimestamp() { return timestamp; }
}

public class CreateOrderCommand extends BaseCommand {
    private final String customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    
    public CreateOrderCommand(String orderId, String customerId, List<OrderItem> items, BigDecimal totalAmount) {
        super(orderId);
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
    }
    
    // Getters...
}

/**
 * Command Handler Interface
 */
public interface CommandHandler<T extends Command> {
    void handle(T command);
}

public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand> {
    private final OrderRepository orderRepository;
    private final EventStore eventStore;
    
    public CreateOrderCommandHandler(OrderRepository orderRepository, EventStore eventStore) {
        this.orderRepository = orderRepository;
        this.eventStore = eventStore;
    }
    
    @Override
    public void handle(CreateOrderCommand command) {
        // Business logic validation
        validateOrder(command);
        
        // Create domain event
        OrderCreatedEvent event = new OrderCreatedEvent(
            command.getAggregateId(),
            command.getCustomerId(),
            command.getItems(),
            command.getTotalAmount()
        );
        
        // Store event (Event Sourcing)
        eventStore.append(event);
        
        // Update aggregate
        Order order = Order.fromCommand(command);
        orderRepository.save(order);
    }
    
    private void validateOrder(CreateOrderCommand command) {
        if (command.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (command.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order total must be positive");
        }
    }
}

/**
 * Command Bus for dispatching commands
 */
public class CommandBus {
    private final Map<Class<?>, CommandHandler<?>> handlers = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    public <T extends Command> void registerHandler(Class<T> commandType, CommandHandler<T> handler) {
        handlers.put(commandType, handler);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Command> void dispatch(T command) {
        CommandHandler<T> handler = (CommandHandler<T>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for command: " + command.getClass());
        }
        handler.handle(command);
    }
}

/**
 * Saga Step as Command (for Saga Pattern)
 */
public class SagaStep implements Command {
    private final String stepId;
    private final String sagaId;
    private final LocalDateTime timestamp;
    private final Runnable action;
    private final Runnable compensation;
    private boolean executed = false;
    
    public SagaStep(String sagaId, Runnable action, Runnable compensation) {
        this.stepId = UUID.randomUUID().toString();
        this.sagaId = sagaId;
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.compensation = compensation;
    }
    
    public void execute() {
        if (!executed) {
            action.run();
            executed = true;
        }
    }
    
    public void compensate() {
        if (executed) {
            compensation.run();
        }
    }
    
    @Override
    public String getCommandId() { return stepId; }
    
    @Override
    public String getAggregateId() { return sagaId; }
    
    @Override
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

---

## 6. Builder Pattern

### Purpose
Constructs complex objects step by step. Essential for creating complex configurations, event objects, and query objects with many optional parameters.

### Use Case in System Design
- Building complex domain events
- Configuration objects for circuit breakers
- Complex query objects in CQRS
- Saga orchestrator configuration

### Java Implementation

```java
/**
 * Domain Event Builder for Event Sourcing
 */
public class DomainEventBuilder {
    private String eventId;
    private String aggregateId;
    private String eventType;
    private Map<String, Object> eventData;
    private LocalDateTime timestamp;
    private int version = 1;
    private Map<String, String> metadata;
    
    public DomainEventBuilder eventId(String eventId) {
        this.eventId = eventId;
        return this;
    }
    
    public DomainEventBuilder aggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
        return this;
    }
    
    public DomainEventBuilder eventType(String eventType) {
        this.eventType = eventType;
        return this;
    }
    
    public DomainEventBuilder eventData(Map<String, Object> eventData) {
        this.eventData = new HashMap<>(eventData);
        return this;
    }
    
    public DomainEventBuilder addData(String key, Object value) {
        if (this.eventData == null) {
            this.eventData = new HashMap<>();
        }
        this.eventData.put(key, value);
        return this;
    }
    
    public DomainEventBuilder timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public DomainEventBuilder version(int version) {
        this.version = version;
        return this;
    }
    
    public DomainEventBuilder metadata(Map<String, String> metadata) {
        this.metadata = new HashMap<>(metadata);
        return this;
    }
    
    public DomainEvent build() {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (eventData == null) {
            eventData = new HashMap<>();
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        
        return new DomainEvent(eventId, aggregateId, eventType, eventData, timestamp, version, metadata);
    }
}

/**
 * Circuit Breaker Configuration Builder
 */
public class CircuitBreakerConfig {
    private final int failureThreshold;
    private final long timeoutInMillis;
    private final int successThreshold;
    private final boolean enableFallback;
    private final FallbackStrategy<?> fallbackStrategy;
    private final List<Class<? extends Exception>> ignoredExceptions;
    
    private CircuitBreakerConfig(Builder builder) {
        this.failureThreshold = builder.failureThreshold;
        this.timeoutInMillis = builder.timeoutInMillis;
        this.successThreshold = builder.successThreshold;
        this.enableFallback = builder.enableFallback;
        this.fallbackStrategy = builder.fallbackStrategy;
        this.ignoredExceptions = builder.ignoredExceptions;
    }
    
    public static class Builder {
        private int failureThreshold = 5;
        private long timeoutInMillis = 60000;
        private int successThreshold = 2;
        private boolean enableFallback = true;
        private FallbackStrategy<?> fallbackStrategy;
        private List<Class<? extends Exception>> ignoredExceptions = new ArrayList<>();
        
        public Builder failureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
            return this;
        }
        
        public Builder timeout(long timeoutInMillis) {
            this.timeoutInMillis = timeoutInMillis;
            return this;
        }
        
        public Builder successThreshold(int successThreshold) {
            this.successThreshold = successThreshold;
            return this;
        }
        
        public Builder enableFallback(boolean enableFallback) {
            this.enableFallback = enableFallback;
            return this;
        }
        
        public Builder fallbackStrategy(FallbackStrategy<?> fallbackStrategy) {
            this.fallbackStrategy = fallbackStrategy;
            return this;
        }
        
        public Builder ignoreException(Class<? extends Exception> exceptionClass) {
            this.ignoredExceptions.add(exceptionClass);
            return this;
        }
        
        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(this);
        }
    }
    
    // Getters...
}

/**
 * Complex Query Builder for CQRS
 */
public class OrderQueryBuilder {
    private String customerId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private OrderStatus status;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int pageSize = 20;
    private int pageNumber = 0;
    private String sortBy = "createdDate";
    private SortDirection sortDirection = SortDirection.DESC;
    
    public OrderQueryBuilder forCustomer(String customerId) {
        this.customerId = customerId;
        return this;
    }
    
    public OrderQueryBuilder dateRange(LocalDateTime startDate, LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }
    
    public OrderQueryBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }
    
    public OrderQueryBuilder amountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        return this;
    }
    
    public OrderQueryBuilder pagination(int pageNumber, int pageSize) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        return this;
    }
    
    public OrderQueryBuilder sortBy(String sortBy, SortDirection direction) {
        this.sortBy = sortBy;
        this.sortDirection = direction;
        return this;
    }
    
    public OrderQuery build() {
        return new OrderQuery(customerId, startDate, endDate, status, 
                            minAmount, maxAmount, pageSize, pageNumber, 
                            sortBy, sortDirection);
    }
}
```

---

## Supporting Classes and Enums

```java
/**
 * Supporting enums and classes
 */
public enum CircuitState {
    CLOSED, OPEN, HALF_OPEN
}

public enum SortDirection {
    ASC, DESC
}

public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

/**
 * Base Domain Event class
 */
public class DomainEvent {
    private final String eventId;
    private final String aggregateId;
    private final String eventType;
    private final Map<String, Object> eventData;
    private final LocalDateTime timestamp;
    private final int version;
    private final Map<String, String> metadata;
    
    public DomainEvent(String eventId, String aggregateId, String eventType, 
                      Map<String, Object> eventData, LocalDateTime timestamp, 
                      int version, Map<String, String> metadata) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.timestamp = timestamp;
        this.version = version;
        this.metadata = metadata;
    }
    
    // Getters...
}
```

---

## Best Practices for System Design

### 1. Event-Driven Architecture
- Use **Observer Pattern** for event handling
- Implement **Command Pattern** for CQRS
- Apply **Factory Pattern** for event creation

### 2. Resilience Patterns
- Use **Strategy Pattern** for fallback mechanisms
- Implement **Singleton Pattern** for circuit breaker registry
- Apply **Builder Pattern** for complex configurations

### 3. Microservices Communication
- Use **Observer Pattern** for service-to-service communication
- Implement **Factory Pattern** for service discovery
- Apply **Command Pattern** for distributed transactions (Saga)

### 4. Performance Optimization
- Use **Strategy Pattern** for different processing strategies
- Implement **Builder Pattern** for complex queries
- Apply **Singleton Pattern** for shared resources

---

## Conclusion

These design patterns form the foundation for building robust, scalable distributed systems. They directly support the system design patterns analyzed in your documentation:

- **Event Sourcing** → Command, Observer, Factory Patterns
- **CQRS** → Command, Builder, Strategy Patterns  
- **Circuit Breaker** → Strategy, Singleton Patterns
- **Saga Pattern** → Command, Observer Patterns
- **Microservices** → Factory, Observer, Strategy Patterns

Each pattern addresses specific challenges in distributed systems while maintaining clean, maintainable code that follows Java best practices.
