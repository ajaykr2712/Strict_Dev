# Project Navigation Guide

## Quick Start

### Running the Demo
```bash
# Clone and navigate to the project
cd 02_HIGH_LEVEL_DESIGN/Java_Application_Design

# Run the simplified demo
javac SimpleECommerceDemo.java && java SimpleECommerceDemo

# Or use the build script
./build.sh
```

## Project Structure Overview

```
Java_Application_Design/
├── 📁 src/main/java/com/ecommerce/     # Source code
│   ├── 🏠 ECommerceApplication.java    # Main application
│   ├── 📁 domain/                      # Domain entities (DDD)
│   ├── 📁 service/                     # Application services
│   ├── 📁 infrastructure/              # Infrastructure components
│   └── 📁 config/                      # Configuration classes
├── 📁 docs/                           # Documentation
│   ├── 📖 ARCHITECTURE.md             # System architecture
│   ├── 📖 DATABASE_DESIGN.md          # Database design
│   └── 📖 DEPLOYMENT.md               # Deployment guide
├── 🔨 build.sh                       # Build script
├── 🚀 SimpleECommerceDemo.java       # Runnable demo
└── 📚 README.md                      # Main documentation
```

## Code Navigation

### 🏗️ Architecture Layers

#### 1. **Main Application** (`ECommerceApplication.java`)
- **Purpose**: Application entry point and dependency wiring
- **Key Features**: Graceful startup/shutdown, system demonstration
- **Patterns**: Dependency injection, factory pattern

#### 2. **Domain Layer** (`domain/`)
Rich domain models following DDD principles:

- **`User.java`** - User aggregate with immutable design
- **`Product.java`** - Product entity with inventory management
- **`Order.java`** - Order aggregate root with state machine
- **`Payment.java`** - Payment entity with transaction states

#### 3. **Service Layer** (`service/`)
Application services orchestrating business operations:

- **`UserService.java`** - User management with caching
- **`ProductService.java`** - Product catalog with search
- **`OrderService.java`** - Order processing workflow
- **`PaymentService.java`** - Payment processing with circuit breaker

#### 4. **Infrastructure Layer** (`infrastructure/`)
Technical concerns and cross-cutting functionality:

- **`MetricsCollector.java`** - Application monitoring
- **`CircuitBreakerRegistry.java`** - Fault tolerance

#### 5. **Configuration Layer** (`config/`)
System configuration and setup:

- **`DatabaseConfig.java`** - Database connection management
- **`CacheConfig.java`** - Multi-level caching setup
- **`MessageQueueConfig.java`** - Event streaming configuration

## 📖 Documentation Guide

### Core Documentation
1. **[README.md](README.md)** - Main project documentation
2. **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture and patterns
3. **[DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md)** - Data modeling and persistence
4. **[DEPLOYMENT.md](docs/DEPLOYMENT.md)** - Operations and deployment

### Key Concepts Covered

#### From "Designing Data-Intensive Applications"
- ✅ **Reliability**: Circuit breakers, retries, graceful degradation
- ✅ **Scalability**: Horizontal scaling, caching, async processing
- ✅ **Maintainability**: Clean architecture, monitoring, documentation

#### Design Patterns Implemented
- 🏛️ **Domain-Driven Design**: Bounded contexts, aggregates, entities
- 🔄 **Event Sourcing**: Complete audit trail and state reconstruction
- 📊 **CQRS**: Separate read/write models for optimization
- 🛡️ **Circuit Breaker**: Fault tolerance and resilience
- 💾 **Repository Pattern**: Data access abstraction
- 🏭 **Factory Pattern**: Object creation and dependency injection

## 🔍 Code Examples

### Domain Entity Usage
```java
// Immutable domain entity with rich behavior
User user = new User("john@example.com", "John Doe");
User updatedUser = user.withLastLogin(LocalDateTime.now());
```

### Service Layer Operations
```java
// Service with caching and metrics
ProductService productService = new ProductService(metricsCollector);
Product product = productService.createProduct("MacBook", "Laptop", price);
List<Product> results = productService.searchProducts("MacBook");
```

### Infrastructure Components
```java
// Circuit breaker for resilience
CircuitBreaker cb = registry.getCircuitBreaker("payment-service");
Payment result = cb.execute(() -> externalPaymentGateway.process(payment));
```

## 🎯 Learning Objectives

### System Design Principles
1. **Data-Intensive Applications**: Handling large volumes of data efficiently
2. **Distributed Systems**: Scalability and fault tolerance patterns
3. **Event-Driven Architecture**: Asynchronous processing and loose coupling
4. **Microservices Patterns**: Service decomposition and communication

### Java Best Practices
1. **Clean Code**: Readable, maintainable, and testable code
2. **Design Patterns**: Proven solutions to common problems
3. **Concurrency**: Thread-safe operations and performance optimization
4. **Error Handling**: Comprehensive exception management

### DevOps Integration
1. **Containerization**: Docker and Kubernetes deployment
2. **CI/CD Pipelines**: Automated testing and deployment
3. **Monitoring**: Application observability and alerting
4. **Security**: Authentication, authorization, and data protection

## 🚀 Running Different Components

### Full Application (when dependencies are available)
```bash
# With proper classpath and dependencies
javac -cp "lib/*" src/main/java/com/ecommerce/*.java
java -cp "lib/*:src/main/java" com.ecommerce.ECommerceApplication
```

### Simplified Demo (no dependencies needed)
```bash
# Self-contained demonstration
javac SimpleECommerceDemo.java
java SimpleECommerceDemo
```

### Using Build Script
```bash
# Automated build and run
./build.sh
```

## 🧪 Testing Strategy

### Unit Testing Examples
```java
@Test
void shouldCreateUserWithValidEmail() {
    User user = new User("test@example.com", "Test User");
    assertThat(user.getEmail()).isEqualTo("test@example.com");
    assertThat(user.isActive()).isTrue();
}

@Test
void shouldCalculateOrderTotal() {
    Product product = new Product("Test", "Description", new BigDecimal("100.00"));
    // Test order total calculation logic
}
```

### Integration Testing
```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void shouldProcessOrderEndToEnd() {
        // Test complete order processing workflow
    }
}
```

## 🎓 Advanced Topics

### Event Sourcing Implementation
```java
// Event store for complete audit trail
public class OrderEventStore {
    public void saveEvent(OrderEvent event) {
        eventRepository.save(new EventRecord(
            event.getAggregateId(),
            event.getEventType(),
            event.getEventData()
        ));
    }
}
```

### CQRS Pattern
```java
// Separate read and write models
@Component
public class OrderCommandHandler {
    public void handle(CreateOrderCommand command) {
        // Write model for commands
    }
}

@Component
public class OrderQueryHandler {
    public OrderView handle(GetOrderQuery query) {
        // Optimized read model for queries
    }
}
```

### Metrics and Monitoring
```java
// Application metrics collection
@Service
public class OrderMetrics {
    private final MeterRegistry meterRegistry;
    
    public void recordOrderCreated() {
        Counter.builder("orders.created")
            .tag("type", "new")
            .register(meterRegistry)
            .increment();
    }
}
```

## 🔧 Development Workflow

### Local Development
1. **Setup**: Install Java 17+, Docker, and IDE
2. **Build**: Use `./build.sh` or IDE build tools
3. **Run**: Execute `SimpleECommerceDemo` for quick start
4. **Test**: Run unit and integration tests
5. **Debug**: Use IDE debugging capabilities

### Production Deployment
1. **Build**: Create Docker image with optimized JVM settings
2. **Deploy**: Use Kubernetes manifests for orchestration
3. **Monitor**: Prometheus metrics and Grafana dashboards
4. **Scale**: Horizontal pod autoscaling based on metrics
5. **Maintain**: Rolling updates and health checks

## 📚 Further Reading

### Books Referenced
- **"Designing Data-Intensive Applications"** - Martin Kleppmann
- **"Domain-Driven Design"** - Eric Evans
- **"Microservices Patterns"** - Chris Richardson
- **"Building Event-Driven Microservices"** - Adam Bellemare

### Online Resources
- Spring Boot documentation
- Kubernetes best practices
- Apache Kafka documentation
- PostgreSQL performance tuning

---

**Note**: This project is designed as a comprehensive learning resource for high-level system design in Java. It demonstrates production-ready patterns and practices while remaining accessible for educational purposes.
