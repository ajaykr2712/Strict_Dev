# Latency

## Overview
Latency is the time delay experienced in a system, typically measured as the time taken for a data packet to travel from source to destination and back (round-trip time). In distributed systems and networking, minimizing latency is crucial for performance and user experience.

## Key Concepts
- **Propagation Delay:** Time for a signal to travel through a medium (fiber, copper, wireless).
- **Transmission Delay:** Time to push all packet bits onto the wire.
- **Processing Delay:** Time routers/switches take to process packet headers.
- **Queuing Delay:** Time a packet waits in routing queues.

## Advanced Topics
### 1. Sources of Latency
- **Physical Distance:** Speed of light limits; longer distances mean higher latency.
- **Network Hops:** Each router/switch adds processing and queuing delay.
- **Congestion:** High traffic increases queuing delays.
- **Protocol Overheads:** Handshakes, encryption, and retransmissions add to latency.

### 2. Measuring Latency
- **Ping:** Measures round-trip time (RTT) between hosts.
- **Traceroute:** Identifies each hop and its latency.
- **Application Metrics:** End-to-end latency as perceived by users.

### 3. Impact on System Design
- **User Experience:** High latency degrades responsiveness (e.g., web apps, games).
- **Distributed Systems:** Consensus protocols (e.g., Paxos, Raft) are sensitive to latency.
- **Microservices:** Inter-service calls can accumulate latency; use asynchronous patterns where possible.

### 4. Techniques to Reduce Latency
- **Edge Computing:** Move computation closer to users.
- **Content Delivery Networks (CDNs):** Cache content near users.
- **Connection Pooling:** Reuse connections to avoid handshake overhead.
- **Protocol Optimization:** Use UDP for lower latency, HTTP/2 multiplexing, QUIC.
- **Load Balancing:** Distribute requests to the nearest/least loaded server.

### 5. Real-World Example
- Online gaming platforms use edge servers to minimize latency for players worldwide.
- Financial trading systems colocate servers near exchanges to reduce latency.

### 6. Best Practices
- Monitor latency at every layer (network, application, database).
- Set Service Level Objectives (SLOs) for latency.
- Use synthetic and real-user monitoring.
- Optimize serialization/deserialization and avoid unnecessary data transfers.

### 7. Interview Questions
- What are the main contributors to network latency?
- How would you design a low-latency system for global users?
- How do CDNs help reduce latency?

### 8. Diagram
```
[User] --(Propagation Delay)--> [Router] --(Queuing/Processing Delay)--> [Server]
```

---
Continue to the next topic for deeper mastery!