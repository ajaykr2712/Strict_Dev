# Load Balancer

## Overview
A load balancer is a system component that distributes incoming network traffic across multiple servers to ensure reliability, scalability, and high availability. It is a critical part of modern distributed architectures, preventing any single server from becoming a bottleneck or point of failure.

## Key Concepts
- **Layer 4 (Transport Layer) Load Balancing:** Operates at TCP/UDP level, routing packets based on IP address and port.
- **Layer 7 (Application Layer) Load Balancing:** Makes routing decisions based on HTTP headers, cookies, or application data.
- **Health Checks:** Monitors server health and removes unhealthy nodes from rotation.
- **Sticky Sessions:** Ensures a client is consistently routed to the same server (useful for session state).

## Advanced Topics
### 1. Algorithms
- **Round Robin:** Evenly distributes requests in order.
- **Least Connections:** Sends traffic to the server with the fewest active connections.
- **IP Hash:** Routes requests based on client IP for session stickiness.
- **Weighted Distribution:** Assigns more traffic to powerful servers.

### 2. High Availability
- **Active-Passive:** One load balancer is active, another is standby.
- **Active-Active:** Multiple load balancers share the load, providing redundancy.
- **Failover:** Automatic rerouting if a load balancer fails.

### 3. SSL/TLS Termination
- Offloads encryption/decryption from backend servers, improving performance.
- Centralizes certificate management.

### 4. Global Load Balancing
- **GeoDNS:** Routes users to the nearest data center.
- **Anycast:** Uses the same IP address in multiple locations; routes to the closest node.
- **Multi-Region Failover:** Ensures service continuity during regional outages.

### 5. Security
- **DDoS Protection:** Absorbs and mitigates large-scale attacks.
- **Web Application Firewall (WAF):** Filters malicious traffic.
- **Rate Limiting:** Prevents abuse by limiting requests per client.

### 6. Real-World Examples
- **NGINX, HAProxy:** Popular open-source load balancers.
- **AWS Elastic Load Balancer (ELB), Google Cloud Load Balancing, Azure Load Balancer:** Managed cloud solutions.

### 7. Best Practices
- Regularly test failover and health check mechanisms.
- Use multiple availability zones or regions for redundancy.
- Monitor latency, error rates, and server health.
- Secure the load balancer with firewalls and access controls.

### 8. Interview Questions
- What are the differences between Layer 4 and Layer 7 load balancing?
- How do you design a highly available load balancing solution?
- How does SSL termination work in a load balancer?

### 9. Diagram
```
[Clients]
   |
[Load Balancer]
   |-----------------------------
   |      |      |      |      |
[Server1][Server2][Server3][ServerN]
```

---
Continue to the next topic for deeper mastery!