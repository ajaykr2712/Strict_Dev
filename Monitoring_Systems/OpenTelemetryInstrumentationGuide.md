# OpenTelemetry Instrumentation Guide

Practical steps to introduce consistent traces, metrics, and logs into a polyglot system.

## Goals
- Uniform correlation across services
- Minimal overhead
- Vendor neutral export pipeline
- Progressive adoption

## Core Concepts
| Concept | Purpose |
|---------|---------|
| TracerProvider | Creates tracers (per instrumentation scope) |
| Resource | Entity metadata (service.name, version) |
| Span | Timed operation with attributes + events |
| MeterProvider | Emits metrics instruments |
| Context Propagation | Carries trace + baggage headers |

## Minimal Java Setup (Pseudo)
```java
SdkTracerProvider tp = SdkTracerProvider.builder()
  .addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build())
  .setResource(Resource.getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME, "orders-svc"))))
  .build();
OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tp)
  .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
  .build();
```

## Semantic Conventions
Adopt official keys: `http.method`, `db.system`, `messaging.system`, `exception.type`, etc. Extend with custom namespace: `custom.domain.*`.

## Instrumentation Order of Adoption
1. Edge (API gateway) spans
2. Critical business flows (checkout, payment)
3. Async boundaries (message publish/consume)
4. External dependencies (DB, cache, SaaS APIs)
5. Background jobs + schedulers

## Metrics to Start
| Domain | Metric | Type | Notes |
|--------|--------|------|-------|
| HTTP | http.server.duration | Histogram | latency SLIs |
| DB | db.client.calls | Counter | cardinality: db.operation |
| JVM | runtime.memory.used | UpDownCounter | per pool |
| Business | order.completed | Counter | dimension: channel |
| Messaging | messaging.publish.duration | Histogram | per topic |

## Logs Integration
Emit logs with trace + span IDs: enrich logger MDC -> enables pivot from error log to full trace.

## Export Pipeline
- Collector as central fan-in
- Receivers: OTLP gRPC/HTTP
- Processors: batch, tail-sampling (error + latency policies)
- Exporters: Prometheus, Tempo/Jaeger, Loki, Elastic, Azure Monitor

## Tail Sampling Example Policies
- Always sample error spans
- Sample 100% of `/checkout` route
- Sample 1% of healthy remainder

## Sampling Strategy
Start head 100% in lower env; drop to 5-20% in prod + tail amplify interesting traces.

## Reducing Cardinality Risk
| Area | Mitigation |
|------|------------|
| High-card attributes | Hash or bucket values |
| Unbounded user IDs | Replace with anonymized tokens |
| Dynamic DB table names | Limit attribute set |

## Dashboards
- Golden signals: latency, traffic, errors, saturation
- Trace waterfall P95 view
- Error rate by service + dependency heat map

## Alerting
Multi-window multi-burn for SLOs: 5m + 1h burn alerts -> page; 6h slow burn -> ticket.

## Rollout Plan
1. Enable auto-instrumentation for HTTP + JDBC
2. Add manual spans for domain steps
3. Configure collector + exemplars for metrics->traces linking
4. Set retention (traces 7d, metrics 30d, logs 14d baseline)
5. Educate devs: cheat sheet of attribute keys

## Pitfalls
| Pitfall | Avoid |
|---------|-------|
| Over-instrumentation noise | Define span naming standard |
| Missing context across async hops | Propagate context explicitly |
| Blocking exporter on request path | Always batch + async |

## Success Indicators
- MTTR reduction
- Trace coverage % of critical flows
- Sampling cost within budget
- Faster root cause isolation (less log grep)
