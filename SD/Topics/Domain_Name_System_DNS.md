# Domain Name System (DNS)

## Overview
The Domain Name System (DNS) is a hierarchical and decentralized naming system that translates human-readable domain names (like www.example.com) into IP addresses that computers use to identify each other on the network.

## Key Concepts
- **Domain Name:** The readable address (e.g., google.com).
- **DNS Resolver:** Client-side component that initiates DNS queries.
- **Root Servers:** The top of the DNS hierarchy, directing queries to TLD servers.
- **TLD Servers:** Handle top-level domains like .com, .org, .net.
- **Authoritative Name Servers:** Hold the actual DNS records for domains.

## Advanced Topics
### 1. DNS Record Types
- **A Record:** Maps a domain to an IPv4 address.
- **AAAA Record:** Maps a domain to an IPv6 address.
- **CNAME:** Alias for another domain name.
- **MX:** Mail exchange server for email routing.
- **NS:** Specifies authoritative name servers.
- **TXT:** Arbitrary text, often for verification (e.g., SPF, DKIM).

### 2. DNS Resolution Process
- Recursive vs. iterative queries
- Caching at multiple levels (browser, OS, resolver, ISP)
- TTL (Time To Live) and its impact on propagation

### 3. DNS Security
- **DNS Spoofing/Poisoning:** Attacker provides false DNS responses.
- **DNSSEC:** Adds cryptographic signatures to DNS data to ensure authenticity.
- **DDoS Attacks:** DNS servers are common targets; mitigation includes rate limiting and Anycast.

### 4. Performance Optimization
- Use of Anycast for global load balancing
- GeoDNS for location-based responses
- DNS prefetching in browsers

### 5. Real-World Example
- When you type www.facebook.com, your device queries DNS to resolve it to an IP address, which is then used to connect to Facebook's servers.

### 6. Best Practices
- Use reputable DNS providers (e.g., Cloudflare, Google DNS)
- Implement DNSSEC for critical domains
- Monitor DNS records for unauthorized changes
- Set appropriate TTLs for balancing propagation speed and caching

### 7. Interview Questions
- Explain the DNS resolution process step by step.
- What is DNS poisoning and how can it be prevented?
- How does DNS caching improve performance?

### 8. Diagram
```
[Client] -> [Local Resolver] -> [Root Server] -> [TLD Server] -> [Authoritative Server] -> [IP Address]
```

---
Continue to the next topic for deeper mastery!