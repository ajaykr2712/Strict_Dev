# Service Mesh Patterns

Service mesh provides transparent service-to-service communication features: mTLS, retries, observability, traffic shaping.

## Core Capabilities
- Secure: mTLS identity, cert rotation
- Resilient: retries, timeouts, circuit breaking
- Traffic Control: canary, A/B, shadowing
- Observability: uniform metrics, traces, access logs
- Policy: authZ, rate limits, RBAC/ABAC at mesh layer

## Key Patterns
| Pattern | Use Case | Notes |
|---------|---------|-------|
| mTLS Mesh | Encrypt east-west traffic | SPIFFE IDs as workload identity |
| Canary Release | Gradual version rollout | Weighted routing (e.g. 5% -> 50% -> 100%) |
| Traffic Shadowing | Test new service with prod traffic | Do not propagate side-effects |
| Fault Injection | Resilience validation | Delay/abort at sidecar |
| Request Shaping | Rate limit, header based routing | Multi-tenant fairness |
| Policy Enforcement | Centralized authZ | OPA or mesh native filters |

## Example VirtualService (Istio)
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: checkout
spec:
  hosts: ["checkout"]
  http:
    - route:
        - destination: { host: checkout, subset: v2, weight: 10 }
        - destination: { host: checkout, subset: v1, weight: 90 }
      retries:
        attempts: 2
        perTryTimeout: 500ms
      timeout: 2s
```

## Observability Integration
- Standardize metrics: `requests_total`, `request_duration_seconds` histogram, `tcp_open_connections`.
- Trace propagation: ensure sidecars forward W3C trace headers.

## Security Hardening
| Concern | Mitigation |
|---------|-----------|
| Key compromise | Short cert TTL (<=24h) + automated rotation |
| Sidecar bypass | Egress locking + network policies |
| Policy drift | GitOps manage CRDs |
| Excess latency | Disable unneeded filters; tune thread pools |

## Performance Considerations
- Per-hop latency overhead target < 5ms P95
- Benchmark with and without mesh to quantify tax
- Enable zero-copy or eBPF acceleration where available

## When to Skip Mesh
- Small system (<5 services) no strict security/compliance needs
- High-throughput ultra-low-latency trading path (custom lib faster)

## Migration Phases
1. Baseline metrics + golden paths without mesh
2. Deploy control plane only; validate cluster health
3. Incrementally inject sidecars (1-2 services)
4. Enable mTLS permissive -> strict
5. Add traffic policies (timeouts, retries)
6. Roll out canaries and policy enforcement
7. Optimize & prune unused features

## Success Indicators
- Reduction in custom networking code
- Uniform telemetry coverage
- Secure score: % services enforcing strict mTLS
- Faster canary rollback (< 2m)
