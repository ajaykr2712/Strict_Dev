# Web Hooks

## Overview
Web hooks are user-defined HTTP callbacks that allow one system to notify another system in real time when certain events occur. They are widely used for integrating external services, automating workflows, and enabling event-driven architectures.

## Key Concepts
- **Event-Driven:** Web hooks are triggered by specific events (e.g., new user signup, payment received).
- **HTTP Callback:** The source system sends an HTTP POST request to a pre-configured URL.
- **Payload:** The request typically contains event data in JSON or XML format.

## Advanced Topics
### 1. Security
- **Secret Tokens:** Include a secret in the payload or header to verify authenticity.
- **IP Whitelisting:** Accept requests only from known IP addresses.
- **Replay Protection:** Use unique IDs or timestamps to prevent duplicate processing.

### 2. Reliability
- **Retries:** Implement retry logic for failed deliveries with exponential backoff.
- **Idempotency:** Ensure repeated events do not cause unintended side effects.
- **Dead Letter Queues:** Store undeliverable events for later inspection or manual processing.

### 3. Scalability
- **Async Processing:** Queue incoming web hook events for background processing.
- **Rate Limiting:** Protect endpoints from being overwhelmed by bursts of events.
- **Fan-out:** Distribute a single event to multiple downstream systems.

### 4. Real-World Example
- Payment gateways (e.g., Stripe, PayPal) use web hooks to notify merchants of completed transactions.
- GitHub web hooks trigger CI/CD pipelines or send notifications to chat apps.

### 5. Best Practices
- Validate payload signatures to ensure authenticity.
- Respond quickly (e.g., 2xx status) and process events asynchronously.
- Log all received events for auditing and troubleshooting.
- Document web hook endpoints and expected payloads clearly.

### 6. Interview Questions
- How do you secure a web hook endpoint?
- What strategies can you use to ensure reliable web hook delivery?
- How would you handle duplicate or out-of-order web hook events?

### 7. Diagram
```
[Source System] --(HTTP POST)--> [Your Web Hook Endpoint] --(Process Event)--> [Internal Service]
```

---
Web hooks are essential for building loosely coupled, event-driven integrations across modern platforms.