# Proxy / Reverse Proxy

## Overview
A proxy is an intermediary server that forwards client requests to other servers. A reverse proxy sits in front of backend servers and forwards client requests to them, often providing additional features like load balancing, security, and caching.

## Key Concepts
- **Forward Proxy:** Sits between clients and the internet, masking client identity and controlling outbound traffic.
- **Reverse Proxy:** Sits between the internet and backend servers, masking server identity and controlling inbound traffic.

## Advanced Topics
### 1. Use Cases
- **Forward Proxy:** Content filtering, anonymity, access control, caching.
- **Reverse Proxy:** Load balancing, SSL termination, web acceleration, security (DDoS protection, hiding backend servers).

### 2. Load Balancing
- Distributes incoming requests among multiple backend servers.
- Algorithms: Round-robin, least connections, IP hash.

### 3. SSL/TLS Termination
- Reverse proxies can handle SSL decryption, offloading this resource-intensive task from backend servers.

### 4. Caching
- Proxies can cache responses to reduce backend load and improve response times.
- Reverse proxies often cache static content (images, scripts).

### 5. Security
- Protects backend servers from direct exposure to the internet.
- Can block malicious traffic, enforce authentication, and mitigate DDoS attacks.

### 6. Real-World Examples
- **Forward Proxy:** Corporate networks restricting employee internet access.
- **Reverse Proxy:** NGINX or HAProxy in front of web servers, Cloudflare as a global reverse proxy.

### 7. Best Practices
- Use reverse proxies for SSL termination and load balancing.
- Regularly update proxy software to patch vulnerabilities.
- Monitor proxy logs for suspicious activity.

### 8. Interview Questions
- What is the difference between a forward proxy and a reverse proxy?
- How does a reverse proxy improve scalability and security?
- Describe a scenario where you would use a forward proxy.

### 9. Diagram
```
[Client] -> [Forward Proxy] -> [Internet]

[Client] -> [Reverse Proxy] -> [Backend Server 1]
                                 [Backend Server 2]
                                 [Backend Server N]
```

---
Continue to the next topic for deeper mastery!