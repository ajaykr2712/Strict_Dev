# Microservices

## Overview
Microservices architecture is an approach to building software systems as a suite of small, independently deployable services, each running in its own process and communicating via lightweight mechanisms, often HTTP APIs. This enables agility, scalability, and resilience in large-scale applications.

## Key Concepts
- **Service Independence:** Each microservice is developed, deployed, and scaled independently.
- **Bounded Context:** Each service owns its data and domain logic.
- **API Communication:** Services interact via APIs (REST, gRPC, messaging).
- **Decentralized Data Management:** Each service manages its own database, avoiding shared schemas.

## Advanced Topics
### 1. Service Discovery
- **Dynamic Registration:** Services register themselves with a discovery server (e.g., Consul, Eureka).
- **Client-Side vs Server-Side Discovery:** Clients or load balancers resolve service locations dynamically.

### 2. Inter-Service Communication
- **Synchronous:** REST, gRPC for request/response.
- **Asynchronous:** Message queues (Kafka, RabbitMQ) for event-driven patterns.
- **Circuit Breakers:** Prevent cascading failures (e.g., Hystrix, Resilience4j).

### 3. Data Consistency
- **Eventual Consistency:** Use events to synchronize data across services.
- **Saga Pattern:** Manage distributed transactions via orchestrated or choreographed workflows.

### 4. Observability
- **Centralized Logging:** Aggregate logs from all services (e.g., ELK stack).
- **Distributed Tracing:** Track requests across services (e.g., Jaeger, Zipkin).
- **Metrics:** Monitor health and performance (e.g., Prometheus, Grafana).

### 5. Deployment
- **Containers:** Package services with Docker.
- **Orchestration:** Use Kubernetes or similar for scaling and resilience.
- **CI/CD Pipelines:** Automate build, test, and deployment for each service.

## Real-World Example
- Netflix, Amazon, and Uber use microservices to scale development and operations across global teams.
- E-commerce platforms split user, order, payment, and inventory into separate services.

## Best Practices
- Keep services small and focused on a single responsibility.
- Automate testing and deployment.
- Use API gateways for routing, security, and aggregation.
- Design for failure: implement retries, timeouts, and fallback logic.

## Interview Questions
- What are the benefits and challenges of microservices?
- How do you handle data consistency across microservices?
- Explain the role of service discovery in microservices.
- How would you migrate a monolith to microservices?

## Diagram
```
[Client] -> [API Gateway] -> [Service A] -> [DB A]
                               [Service B] -> [DB B]
                               [Service C] -> [DB C]
```

---
Microservices enable scalable, resilient, and agile systems, but require careful design and operational maturity.