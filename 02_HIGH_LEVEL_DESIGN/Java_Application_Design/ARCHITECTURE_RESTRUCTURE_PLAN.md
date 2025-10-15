# Architecture Restructuring Plan

This plan applies Backend Engineering Principles to the codebase.

## Goals
- Separation of concerns (API, Service, Repository, Domain, DTO, Config)
- Improve maintainability & testability
- Introduce consistent API layer
- Add observability, caching, validation, resilience
- Prepare for CI/CD & modular growth

## Target Package Layout
com.ecommerce
  ├── api (REST controllers)
  ├── service (business logic)
  ├── repository (data access)
  ├── entity (JPA entities)
  ├── dto (API data transfer objects)
  ├── mapper (entity ↔ DTO mapping)
  ├── exception (custom & global handlers)
  ├── config (infra & cross-cutting concerns)
  ├── security (future authN/Z)
  └── util (helpers)

## Phases
1. Baseline inventory (DONE)
2. Introduce DTO + Mapper + Controller for Product
3. Add GlobalExceptionHandler
4. Add Cache + OpenAPI config
5. Add main Spring Boot application entrypoint
6. Add observability (metrics/tracing) integration stubs
7. Harden service layer (validation & transactional boundaries)
8. Prepare testing scaffolding (unit + integration)
9. Cleanup legacy overlap (domain vs entity) – converge on entity for persistence, optional pure domain models later

## Immediate Actions (this commit)
- Add ProductDTO, ProductMapper
- Add ProductController with CRUD endpoints
- Add CacheConfig & OpenApiConfig
- Add GlobalExceptionHandler
- Add EcommerceApplication

## Next Actions
- Add pagination & filtering to ProductController
- Add request validation annotations on DTO
- Introduce tracing (OpenTelemetry) instrumentation
- Add integration tests with Testcontainers (PostgreSQL)
- Add GitHub Actions workflow (build + test)

## Deprecated Legacy Models
Legacy classes under `com.ecommerce.domain` are marked with `@Deprecated` in favor of JPA entities under `com.ecommerce.entity`.

## Upcoming Refinements
- Add Order/Payment DTOs & controllers
- Introduce pagination & filtering (price range, category)
- Add OpenTelemetry instrumentation stubs
- Add integration tests with Testcontainers (PostgreSQL, Redis, Kafka)
- Add GitHub Actions CI workflow (build, test, security scan)
- Introduce MapStruct for mapper generation (optional)
- Add Bean Validation annotations to request DTOs
- Implement caching TTL strategy & eviction tests

## References
See `Backend_Engineering_Principles.md` for principle alignment.
