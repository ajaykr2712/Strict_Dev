# Web Sockets

## Overview
Web Sockets provide a full-duplex communication channel over a single, long-lived TCP connection, enabling real-time, bidirectional data exchange between clients and servers. They are ideal for applications requiring instant updates, such as chat apps, online gaming, and live dashboards.

## Key Concepts
- **Full-Duplex:** Both client and server can send messages independently at any time.
- **Persistent Connection:** Unlike HTTP, the connection remains open, reducing overhead.
- **Low Latency:** Eliminates the need for repeated HTTP requests for real-time data.

## Advanced Topics
### 1. WebSocket Protocol
- **Handshake:** Starts as an HTTP request, then upgrades to WebSocket protocol.
- **Frames:** Data is sent in small packets (frames) for efficiency.
- **Subprotocols:** Allows negotiation of custom protocols on top of WebSockets.

### 2. Scalability Challenges
- **Connection Management:** Handling thousands or millions of concurrent connections.
- **Load Balancing:** Requires sticky sessions or distributed state management.
- **Horizontal Scaling:** Use message brokers (e.g., Redis Pub/Sub) to synchronize state across nodes.

### 3. Security
- **WSS (WebSocket Secure):** Encrypts traffic using TLS.
- **Authentication:** Use tokens or cookies during handshake; validate on every message if needed.
- **Rate Limiting:** Prevent abuse by limiting messages per client.

### 4. Real-World Example
- Collaborative editing tools (e.g., Google Docs) use WebSockets for live updates.
- Financial trading platforms push real-time market data to clients.

### 5. Best Practices
- Use heartbeats/pings to detect dropped connections.
- Implement backpressure to avoid overwhelming clients or servers.
- Monitor connection health and resource usage.
- Fallback to HTTP polling for clients that do not support WebSockets.

### 6. Interview Questions
- How does WebSocket differ from HTTP long polling?
- What are the challenges of scaling WebSocket servers?
- How do you secure a WebSocket connection?

### 7. Diagram
```
[Client] <== WebSocket Connection ==> [Server]
```

---
Web Sockets are essential for building interactive, real-time web applications at scale.