# IP Addresses

## Overview
An IP (Internet Protocol) address is a unique identifier assigned to each device connected to a network that uses the Internet Protocol for communication. IP addresses enable devices to locate and communicate with each other across networks.

## Types of IP Addresses
- **IPv4:** 32-bit address, written as four decimal numbers separated by dots (e.g., 192.168.1.1). Supports ~4.3 billion addresses.
- **IPv6:** 128-bit address, written as eight groups of four hexadecimal digits (e.g., 2001:0db8:85a3:0000:0000:8a2e:0370:7334). Designed to solve IPv4 exhaustion.

## Advanced Topics
### 1. Public vs. Private IP Addresses
- **Public:** Routable on the global internet, assigned by ISPs.
- **Private:** Used within local networks (e.g., 10.0.0.0/8, 192.168.0.0/16), not routable on the internet.

### 2. Subnetting
- Divides a network into smaller logical segments (subnets).
- Uses subnet masks (e.g., 255.255.255.0 or /24) to define network and host portions.
- Enables efficient IP address allocation and improved security.

### 3. CIDR (Classless Inter-Domain Routing)
- Replaces the old class-based system (A, B, C) with flexible prefix lengths (e.g., 192.168.1.0/24).
- Allows for more efficient use of IP address space.

### 4. NAT (Network Address Translation)
- Maps private IP addresses to a public IP for internet access.
- Types: Static NAT, Dynamic NAT, PAT (Port Address Translation).
- Essential for conserving public IPs and providing security.

### 5. DHCP (Dynamic Host Configuration Protocol)
- Automatically assigns IP addresses to devices on a network.
- Reduces manual configuration and IP conflicts.

### 6. Security Considerations
- IP spoofing: Faking source IP addresses to bypass security controls.
- Blacklisting/whitelisting: Controlling access based on IP.
- Geolocation: Determining user location from IP (with limitations).

### 7. Real-World Example
- Home router assigns private IPs to devices via DHCP, uses NAT to share a single public IP with the internet.
- Cloud providers allocate public and private IPs to VMs for internal and external communication.

### 8. Best Practices
- Use private IPs for internal resources.
- Implement subnetting for network segmentation.
- Monitor and log IP address usage for security.

### 9. Interview Questions
- Explain the difference between IPv4 and IPv6.
- How does NAT work and why is it important?
- What is subnetting and why is it used?

### 10. Diagram
```
[Device 1: 192.168.1.2] --|
[Device 2: 192.168.1.3] --|--> [Router: NAT] --> [Public IP: 203.0.113.5] --> [Internet]
[Device 3: 192.168.1.4] --|
```

---
Proceed to the next topic for deeper mastery!