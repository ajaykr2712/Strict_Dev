# Consistent Hashing

## Overview
Consistent Hashing is a distributed hashing technique that minimizes the number of keys that need to be remapped when hash table slots are added or removed. It's particularly useful in distributed systems where nodes can join or leave the cluster dynamically without causing massive data redistribution.

## Key Concepts
- **Hash Ring**: Circular hash space where both data and nodes are hashed
- **Virtual Nodes**: Multiple positions for each physical node on the ring
- **Minimal Reshuffling**: Only affects keys between failed node and its successor
- **Load Distribution**: Evenly distributes data across available nodes

## Advanced Topics

### 1. Hash Ring Implementation
```
Hash Space: 0 to 2^32-1 (or 2^64-1)

    Node A (hash: 100)
         ↑
    ┌────────────┐
    │            │
Node D ←        → Node B  
(hash: 300)      (hash: 150)
    │            │
    └────────────┘
         ↓
    Node C (hash: 250)
```

### 2. Virtual Nodes Strategy
- **Problem**: Uneven distribution with few physical nodes
- **Solution**: Each physical node gets multiple virtual positions
- **Benefits**: Better load distribution, gradual migration
- **Implementation**: Hash node_id + replica_number

### 3. Data Placement Algorithm
```python
def find_node(key, ring):
    """Find responsible node for a key"""
    key_hash = hash(key)
    
    # Find first node clockwise from key position
    for node_hash in sorted(ring.keys()):
        if key_hash <= node_hash:
            return ring[node_hash]
    
    # Wrap around to first node
    return ring[min(ring.keys())]
```

### 4. Node Addition/Removal
- **Addition**: Only data between new node and predecessor moves
- **Removal**: Data moves to successor node
- **Replication**: Consider replication factor for fault tolerance
- **Gradual Migration**: Transfer data incrementally

### 5. Applications in Distributed Systems

#### Amazon DynamoDB
- Uses consistent hashing for data partitioning
- Virtual nodes for load balancing
- Automatic scaling and rebalancing

#### Apache Cassandra
- Consistent hashing for token assignment
- Virtual nodes (vnodes) for better distribution
- Configurable replication strategies

#### Content Delivery Networks (CDN)
- Route requests to nearest cache servers
- Handle server failures gracefully
- Minimize cache misses during topology changes

### 6. Implementation Considerations
- **Hash Function**: Use cryptographic hash (SHA-1, MD5)
- **Virtual Node Count**: Balance between distribution and overhead
- **Replication Factor**: Multiple copies for fault tolerance
- **Monitoring**: Track load distribution across nodes

### 7. Benefits
- **Scalability**: Easy to add/remove nodes
- **Fault Tolerance**: Minimal impact from node failures
- **Load Distribution**: Even data distribution
- **Efficiency**: Minimal data movement during changes

### 8. Challenges
- **Hotspots**: Popular keys can create load imbalances
- **Heterogeneous Nodes**: Different node capacities need special handling
- **Network Partitions**: Handling split-brain scenarios
- **Consistency**: Maintaining data consistency during migrations

### 9. Interview Questions
- How does consistent hashing solve the cache invalidation problem?
- What are virtual nodes and why are they important?
- How would you handle hotspots in consistent hashing?
- Explain data migration when nodes join/leave the cluster

### 10. Real-world Example
```python
class ConsistentHashRing:
    def __init__(self, virtual_nodes=100):
        self.virtual_nodes = virtual_nodes
        self.ring = {}
        self.nodes = set()
    
    def add_node(self, node):
        """Add a node to the hash ring"""
        for i in range(self.virtual_nodes):
            virtual_node = f"{node}:{i}"
            hash_value = self._hash(virtual_node)
            self.ring[hash_value] = node
        self.nodes.add(node)
    
    def remove_node(self, node):
        """Remove a node from the hash ring"""
        for i in range(self.virtual_nodes):
            virtual_node = f"{node}:{i}"
            hash_value = self._hash(virtual_node)
            if hash_value in self.ring:
                del self.ring[hash_value]
        self.nodes.discard(node)
    
    def get_node(self, key):
        """Get responsible node for a key"""
        if not self.ring:
            return None
        
        key_hash = self._hash(key)
        sorted_hashes = sorted(self.ring.keys())
        
        for hash_value in sorted_hashes:
            if key_hash <= hash_value:
                return self.ring[hash_value]
        
        # Wrap around to first node
        return self.ring[sorted_hashes[0]]
```

---
Continue to the next topic for deeper mastery!
