# Microservices Design Patterns

## Core Microservices Patterns

### 1. Decomposition Patterns

#### Decompose by Business Capability
- Organize services around business capabilities
- Each service owns its data and business logic
- Example: User Service, Order Service, Payment Service

#### Decompose by Subdomain (Domain-Driven Design)
- Align services with domain model subdomains
- Bounded contexts define service boundaries
- Example: Inventory Context, Shipping Context, Billing Context

### 2. Data Management Patterns

#### Database per Service
- Each microservice has its own database
- Data consistency through eventual consistency
- No shared databases between services

#### Shared Database Anti-Pattern
- Multiple services sharing same database
- Creates tight coupling
- Should be avoided in microservices

#### Event Sourcing
- Store events instead of current state
- Rebuild state by replaying events
- Complete audit trail

### 3. Communication Patterns

#### Synchronous Communication
- HTTP/REST APIs
- GraphQL APIs
- gRPC for high-performance

#### Asynchronous Communication
- Message queues (RabbitMQ, Apache Kafka)
- Event-driven architecture
- Publish-subscribe patterns

#### Service Mesh
- Infrastructure layer for service communication
- Traffic management, security, observability
- Examples: Istio, Linkerd, Consul Connect

### 4. Reliability Patterns

#### Circuit Breaker
- Prevent cascading failures
- Fast failure when service is down
- Automatic recovery detection

#### Bulkhead
- Isolate critical resources
- Prevent resource exhaustion
- Example: Separate thread pools

#### Timeout
- Set time limits for service calls
- Prevent hanging requests
- Graceful degradation

#### Retry
- Automatic retry for transient failures
- Exponential backoff
- Maximum retry limits

### 5. Monitoring and Observability

#### Health Check API
- Service health endpoints
- Dependency health checks
- Load balancer integration

#### Log Aggregation
- Centralized logging
- Correlation IDs for tracing
- Examples: ELK Stack, Fluentd

#### Distributed Tracing
- Track requests across services
- Performance bottleneck identification
- Examples: Jaeger, Zipkin

#### Metrics Collection
- Service performance metrics
- Business metrics
- Examples: Prometheus, Grafana

### 6. Deployment Patterns

#### Service per Container
- Each service in its own container
- Docker containerization
- Kubernetes orchestration

#### Serverless Deployment
- Functions as a Service (FaaS)
- AWS Lambda, Azure Functions
- Event-triggered execution

#### Blue-Green Deployment
- Two identical production environments
- Zero-downtime deployments
- Quick rollback capability

#### Canary Deployment
- Gradual rollout to subset of users
- Risk mitigation
- Performance monitoring

### 7. Security Patterns

#### Access Token
- Token-based authentication
- JWT tokens
- OAuth 2.0 / OpenID Connect

#### API Gateway
- Single entry point for all clients
- Authentication and authorization
- Rate limiting and throttling

### 8. Testing Patterns

#### Consumer-Driven Contract Testing
- Contracts defined by consumers
- Provider implements contracts
- Tools: Pact, Spring Cloud Contract

#### Service Component Testing
- Test service in isolation
- Mock external dependencies
- In-process testing

#### End-to-End Testing
- Test complete user journeys
- Across multiple services
- Minimal but critical scenarios

## Implementation Guidelines

### Service Size
- Small enough to be owned by one team
- Large enough to provide business value
- 2-pizza team rule (Amazon)

### Data Consistency
- Eventual consistency preferred
- Saga pattern for distributed transactions
- Compensation actions for failures

### API Design
- RESTful APIs with proper HTTP methods
- Versioning strategy
- Backward compatibility

### Error Handling
- Proper HTTP status codes
- Detailed error messages
- Client-friendly error responses
