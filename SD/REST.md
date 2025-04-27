# REST (Representational State Transfer)

## Overview
REST is an architectural style for designing networked applications. It relies on stateless, client-server communication, typically over HTTP, and emphasizes resources, standard methods, and representations.

## Key Concepts
- **Resource:** Any entity (user, order, product) identified by a URI.
- **Representation:** The format (JSON, XML, HTML) in which a resource is transferred.
- **Statelessness:** Each request contains all information needed; server does not store client context.
- **Uniform Interface:** Standardized HTTP methods (GET, POST, PUT, DELETE, PATCH).
- **Client-Server Separation:** Decouples frontend and backend, enabling independent evolution.
- **Cacheability:** Responses must define themselves as cacheable or not.

## Advanced Topics
### 1. REST Constraints
- **Stateless:** No session state on server; improves scalability.
- **Layered System:** Intermediaries (proxies, gateways) can exist between client and server.
- **Code on Demand (optional):** Servers can send executable code (e.g., JavaScript) to clients.

### 2. Resource Modeling
- Use nouns for URIs (e.g., /users, /orders/123).
- Hierarchical relationships (e.g., /users/123/orders).
- Avoid verbs in URIs; use HTTP methods for actions.

### 3. HTTP Methods in Depth
- **GET:** Retrieve resource(s); safe and idempotent.
- **POST:** Create new resource; not idempotent.
- **PUT:** Replace resource; idempotent.
- **PATCH:** Partial update; idempotent.
- **DELETE:** Remove resource; idempotent.

### 4. Status Codes
- Use standard codes (200 OK, 201 Created, 204 No Content, 400 Bad Request, 404 Not Found, 409 Conflict, 500 Internal Server Error).
- Provide meaningful error messages in response bodies.

### 5. HATEOAS (Hypermedia as the Engine of Application State)
- Responses include links to related actions/resources.
- Enables discoverability and decouples client from server structure.

### 6. Versioning
- URI versioning (/v1/users), header versioning, or content negotiation.
- Plan for backward compatibility and graceful deprecation.

### 7. Security
- Use HTTPS for all endpoints.
- Implement authentication (OAuth2, JWT) and authorization.
- Validate and sanitize all inputs.
- Rate limit and log requests.

### 8. Performance
- Use caching (ETag, Cache-Control headers).
- Pagination for large collections.
- Compression (gzip, Brotli).
- Minimize payload size.

### 9. Real-World Example
- GitHub API: RESTful endpoints for repositories, issues, users.
- Twitter API (v1): REST endpoints for tweets, timelines, users.

### 10. Best Practices
- Consistent naming and structure.
- Use plural nouns for collections.
- Provide clear, machine-readable error responses.
- Document all endpoints and expected behaviors.

### 11. Interview Questions
- What are the constraints of REST?
- How do you handle versioning in REST APIs?
- Explain HATEOAS and its benefits.

### 12. Diagram
```
[Client] --(GET /users/123)--> [REST API Server] --(DB Query)--> [Database]
         <--(200 OK, JSON User Data)--
```

---
Continue to the next topic for deeper mastery!