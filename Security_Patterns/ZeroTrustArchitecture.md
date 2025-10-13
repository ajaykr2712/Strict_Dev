# Zero Trust Architecture (ZTA)

## Essence
"Never trust, always verify" — every request is strongly authenticated, authorized, context-evaluated, and minimally privileged.

## Core Pillars
1. Strong Identity (human + workload)
2. Device Posture & Health Attestation
3. Least Privilege & Just-In-Time (JIT) Access
4. Network Micro-Segmentation & Software Defined Perimeter
5. Continuous Authorization & Risk-Adaptive Policies
6. Strong Telemetry + Automated Response
7. Secure Software Supply Chain

## Reference Flow
```
User/Service -> Identity Provider -> Policy Decision Point (PDP)
  -> Context Signals (device, geo, anomaly, time, risk score)
  -> Policy Enforcement Point (PEP) (API Gateway / Service Mesh / Sidecar)
  -> Target Resource (Service / Data)
```

## Recommended Implementation Layers
| Layer | Practices |
|-------|-----------|
| Identity | OIDC, FIDO2/WebAuthn, mTLS SPIFFE IDs |
| AuthZ | ABAC + ReBAC (OPA / Cedar) |
| Network | Service Mesh (mTLS, identity propagation) |
| Data | Row/column level security + tokenization |
| Secrets | Vault + short‑lived credentials |
| Observability | Trace correlation + audit immutability |
| Supply Chain | SBOM, SLSA, provenance attestation |

## Policy Example (OPA Rego Sketch)
```rego
package access

default allow = false

allow {
  input.subject.assurance_level >= 3
  input.resource.classification == "internal"
  input.device.trusted
  time.within(work_hours)
}
```

## Migration Roadmap
1. Inventory identities, services, data domains
2. Establish central IdP + federation; eliminate shared secrets
3. Enforce mTLS between all east-west service calls
4. Introduce sidecar or gateway enforcing OPA policies
5. Roll out fine-grained scopes + least privilege tokens
6. Add continuous risk scoring (UEBA, anomaly detection)
7. Instrument all calls with trace IDs; route to SIEM + ML detectors
8. Automate credential rotation & ephemeral build signing

## Metrics
- % services with enforced mTLS
- Mean token lifetime (target < 15m)
- Policy evaluation latency (P95 < 10ms)
- Unauthorized lateral movement attempts blocked

## Common Pitfalls
| Pitfall | Mitigation |
|---------|------------|
| Lift-and-shift RBAC only | Evolve toward contextual ABAC/ReBAC |
| Overly long JWT lifetimes | Use refresh + short access tokens |
| Blind trust in network zone | Remove flat networks; segment by identity |
| Policy sprawl | Version + test policies (gitops) |
| Latency from mesh/policy | Co-locate PDP cache; pre-compute decisions |

## Tooling Stack Suggestions
- Identity: Keycloak, Azure AD, Okta, SPIRE
- Policy: OPA, Cedar, Zanzibar-inspired graph
- Mesh: Istio, Linkerd, Kuma (mTLS + identity)
- Secrets: HashiCorp Vault, AWS KMS, GCP KMS
- Supply Chain: Sigstore (cosign), SLSA framework, Syft/Grype for SBOM

## Advanced Enhancements
- Adaptive session hardening based on anomaly score
- Hardware-backed keys (TPM/SE) for workload identities
- Continuous verification loops (every request path)
- Data egress policy enforcement + DLP integration

## When to Use
- Regulated environments (finance, healthcare)
- Multi-tenant SaaS
- Microservices with high lateral movement risk

## Not Always Needed
- Monolithic internal prototype with no sensitive data (overhead > value)

## Integration Notes
Standardize an identity propagation header (e.g. `x-subject-spiffe-id`) and map to trace context for unified audit surfaces.
