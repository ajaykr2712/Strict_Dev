# Vertical Scaling

## Overview
Vertical scaling ("scaling up") involves increasing the capacity of a single server or node by adding more resources—CPU, RAM, storage, or network bandwidth. It is a common approach for improving performance and handling increased load in monolithic or legacy systems.

## Key Concepts
- **Single Node Enhancement:** Upgrade hardware or virtual resources on a single machine.
- **No Change to Application Logic:** The application continues running on the same server, often with minimal code changes.
- **Limits:** Physical and cost constraints eventually cap vertical scaling.

## Advanced Topics
### 1. Hardware Upgrades
- **CPU:** More cores, higher clock speeds for parallel processing.
- **Memory:** Increased RAM for caching, in-memory databases, and large working sets.
- **Storage:** Faster SSDs/NVMe for I/O-bound workloads.
- **Network:** Higher bandwidth NICs for network-intensive applications.

### 2. Virtualization & Cloud
- **Cloud Providers:** Easily resize VMs (AWS EC2, Azure VMs, GCP Compute Engine).
- **Hot Swapping:** Some platforms allow live resource upgrades with minimal downtime.
- **Cost Considerations:** Larger instances are disproportionately more expensive.

### 3. Performance Bottlenecks
- **Diminishing Returns:** Doubling resources does not always double performance due to software and hardware limits.
- **Single Point of Failure:** If the node fails, the entire application is impacted.
- **Resource Contention:** Multiple processes/services may compete for limited resources.

### 4. Use Cases
- **Databases:** Vertical scaling is often used for RDBMS before sharding.
- **Legacy Apps:** Monolithic applications not designed for distributed scaling.
- **Quick Fixes:** Temporary solution for sudden traffic spikes.

### 5. Best Practices
- Monitor resource utilization (CPU, memory, disk, network).
- Plan for downtime or use live migration features.
- Regularly benchmark after upgrades.
- Combine with horizontal scaling for hybrid approaches.

### 6. Real-World Example
- Upgrading a database server from 16GB to 128GB RAM to handle larger datasets in-memory.
- Moving from a 4-core to a 32-core VM to support more concurrent users.

### 7. Interview Questions
- What are the pros and cons of vertical scaling?
- When would you choose vertical scaling over horizontal scaling?
- How do you mitigate the risks of single points of failure in vertically scaled systems?

### 8. Diagram
```
[App] --> [Server: 4 CPU, 16GB RAM]  --(Upgrade)--> [Server: 32 CPU, 128GB RAM]
```

---
Continue to the next topic for deeper mastery!