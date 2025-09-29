# Contract Testing with Pact (Conceptual Example)

## Goal
Ensure consumer and provider evolve independently without breaking API expectations.

## Core Components
- Consumer tests generate pact files (expected interactions)
- Broker stores contracts & verifies compatibility matrix
- Provider verification runs against stored contracts

## Consumer Test Sketch (Java/JUnit + Pact)
```java
@Pact(consumer="checkout-web")
public RequestResponsePact createPact(PactDslWithProvider builder) {
  return builder
    .given("product 42 exists")
    .uponReceiving("fetch product 42")
      .path("/products/42").method("GET")
    .willRespondWith()
      .status(200)
      .body(newJsonBody(o -> o.stringType("id", "42").stringType("name", "Widget")))
    .toPact();
}
```

## Provider Verification Flow
1. Fetch latest pacts tagged `main` from broker
2. Spin up provider in verification mode
3. Replay interactions; assert status + body shape
4. Publish verification results (pass/fail + version)

## Versioning Strategy
- Tag consumer builds: `main`, `staging`, `prod`
- Use semantic version for provider images; broker retains compatibility matrix

## CI Integration
| Stage | Action |
|-------|--------|
| Consumer Build | Run consumer pact tests -> publish pact |
| Provider Build | Verify against all relevant consumer pacts |
| Release Gate | Ensure no failing verification before deploy |

## Advantages
- Early failure detection
- Reduced need for full end-to-end test sprawl
- Clear change negotiation via contract diff

## Limitations
- Does not validate non-functional aspects (latency, auth)
- Overly granular endpoints produce maintenance noise

## Best Practices
| Area | Recommendation |
|------|----------------|
| Pact Scope | Model business use cases, not every 404 |
| Data Setup | Use provider states for determinism |
| Failure Diff | Integrate contract diff into PR review |
| Backward Compat | Add fields (tolerated); avoid breaking removals |

## Metrics
- % builds blocked by contract break (target low)
- Time to detect breaking change (shift-left)
- Number of redundant e2e tests removed

## Migration Path
1. Identify top 5 critical consumer-provider pairs
2. Add consumer pact tests
3. Add provider verification in CI
4. Enforce release policy (no unverified contracts)
5. Expand coverage gradually
