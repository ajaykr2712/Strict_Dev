# Content Delivery Network (CDN)

## Overview
A Content Delivery Network (CDN) is a distributed network of servers that delivers web content and media to users based on their geographic location, the origin of the content, and a content delivery server. CDNs improve performance, scalability, and reliability for websites and applications.

## Key Concepts
- **Edge Servers:** Servers located close to end-users to cache and serve content quickly.
- **Caching:** Storing copies of static and dynamic content at edge locations.
- **Origin Server:** The primary server where the original content resides.
- **Geographic Distribution:** Reduces latency by serving content from the nearest edge node.

## Advanced Topics
### 1. CDN Architectures
- **Push vs Pull:** Push CDNs require content to be uploaded to edge nodes; pull CDNs fetch content from the origin as needed.
- **Anycast Routing:** Uses the same IP address for multiple edge nodes, routing users to the nearest one.
- **Dynamic Content Acceleration:** Optimizes delivery of non-cacheable, dynamic content.

### 2. Security Features
- **DDoS Protection:** Absorbs and mitigates large-scale attacks.
- **TLS/SSL Offloading:** Handles encryption at the edge to reduce load on origin servers.
- **WAF (Web Application Firewall):** Protects against common web vulnerabilities.

### 3. Real-World Example
- Streaming platforms use CDNs to deliver video content globally with minimal buffering.
- E-commerce sites use CDNs to ensure fast page loads during high-traffic events.

### 4. Best Practices
- Set appropriate cache-control headers for static and dynamic content.
- Use CDN analytics to monitor performance and detect anomalies.
- Integrate CDN with your DNS for seamless failover and routing.
- Regularly purge and update cached content to avoid serving stale data.

### 5. Interview Questions
- How does a CDN improve website performance?
- What are the differences between push and pull CDN architectures?
- How do CDNs handle dynamic content?

### 6. Diagram
```
[User] <--> [Nearest CDN Edge Server] <--> [Origin Server]
```

---
CDNs are essential for delivering fast, reliable, and secure web experiences at scale.