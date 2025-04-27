# HTTP / HTTPS

## Overview
HTTP (Hypertext Transfer Protocol) is the foundational protocol for data communication on the web. HTTPS (HTTP Secure) is the encrypted version of HTTP, providing confidentiality, integrity, and authentication over insecure networks.

## Key Concepts
- **HTTP:** Stateless, application-layer protocol for transmitting hypermedia documents (HTML, JSON, etc.).
- **HTTPS:** HTTP over TLS/SSL, encrypting data between client and server.
- **Request/Response Model:** Client sends requests (GET, POST, etc.), server responds with status codes and data.

## Advanced Topics
### 1. HTTP Methods
- **GET:** Retrieve data (idempotent, safe).
- **POST:** Submit data (not idempotent).
- **PUT/PATCH:** Update resources.
- **DELETE:** Remove resources.
- **HEAD/OPTIONS:** Metadata and supported methods.

### 2. Status Codes
- **1xx:** Informational
- **2xx:** Success (200 OK, 201 Created)
- **3xx:** Redirection (301, 302, 304)
- **4xx:** Client errors (400 Bad Request, 401 Unauthorized, 404 Not Found)
- **5xx:** Server errors (500 Internal Server Error, 502 Bad Gateway)

### 3. Headers & Cookies
- **Headers:** Metadata (Content-Type, Authorization, Cache-Control, etc.)
- **Cookies:** State management, authentication, tracking
- **CORS:** Cross-Origin Resource Sharing for browser security

### 4. HTTPS & TLS
- **TLS Handshake:** Negotiates encryption keys, authenticates server (and optionally client)
- **Certificates:** Issued by Certificate Authorities (CA), validate server identity
- **Perfect Forward Secrecy:** Ensures session keys cannot be compromised retroactively

### 5. Performance
- **HTTP/1.1:** Persistent connections, pipelining
- **HTTP/2:** Multiplexing, header compression, server push
- **HTTP/3 (QUIC):** UDP-based, faster handshakes, improved performance

### 6. Security
- **Man-in-the-Middle Attacks:** Prevented by HTTPS
- **HSTS:** Enforces HTTPS
- **Content Security Policy (CSP):** Mitigates XSS
- **Secure Cookies, SameSite, HttpOnly**

### 7. Real-World Example
- Browsing a website: Browser sends HTTPS requests, receives HTML, CSS, JS, images.
- REST APIs: Mobile apps communicate securely with backend servers using HTTPS.

### 8. Best Practices
- Always use HTTPS, redirect HTTP to HTTPS
- Use strong TLS configurations (disable weak ciphers, use modern protocols)
- Set secure headers (CSP, HSTS, X-Frame-Options)
- Monitor certificate expiration and renew proactively

### 9. Interview Questions
- Explain the TLS handshake process.
- What are the differences between HTTP/1.1, HTTP/2, and HTTP/3?
- How do you secure cookies in web applications?

### 10. Diagram
```
[Client] --(HTTPS Request)--> [Server]
         <--(HTTPS Response)--
```

---
Continue to the next topic for deeper mastery!