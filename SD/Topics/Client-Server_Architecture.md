# Client-Server Architecture

## Overview
Client-server architecture is a network design framework where client devices request resources or services from centralized servers. This model underpins most modern web and enterprise applications.

## Key Concepts
- **Client:** Initiates requests, typically a web browser, mobile app, or desktop application.
- **Server:** Responds to client requests, processes data, and manages resources.
- **Communication:** Usually over TCP/IP, HTTP/HTTPS, or WebSockets.

## Advanced Topics
### 1. Stateless vs. Stateful Servers
- **Stateless:** Each request is independent (e.g., REST APIs). Easier to scale horizontally.
- **Stateful:** Server maintains session state (e.g., traditional web apps, some multiplayer games).

### 2. Load Balancing
- Distributes client requests across multiple servers to optimize resource use, maximize throughput, and ensure reliability.
- Techniques: Round-robin, least connections, IP hash.

### 3. Scalability
- **Vertical Scaling:** Adding more power (CPU, RAM) to a single server.
- **Horizontal Scaling:** Adding more servers to handle increased load.

### 4. Security
- Authentication & Authorization (OAuth, JWT)
- TLS/SSL for encrypted communication
- DDoS protection

### 5. Real-World Example
- Web applications (e.g., Gmail): Browser (client) communicates with Google servers (server).
- Mobile apps: App (client) interacts with backend APIs (server).

### 6. Best Practices
- Decouple client and server logic
- Use API versioning
- Implement proper error handling and logging
- Monitor server health and performance

### 7. Interview Questions
- Explain the difference between stateless and stateful servers.
- How would you scale a client-server application to handle millions of users?
- What are the security risks in client-server architecture and how do you mitigate them?

### 8. Diagram
```
[Client] ---> [Load Balancer] ---> [Server 1]
                                 ---> [Server 2]
                                 ---> [Server N]
```

---
Continue to the next topic for deeper mastery!