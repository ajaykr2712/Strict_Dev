#!/usr/bin/env python3
"""
System Design Tools and Utilities
Practical tools for system design learning and implementation
"""

import time
import random
import threading
import json
from typing import Dict, List, Optional, Any, Callable
from dataclasses import dataclass
from collections import defaultdict, deque
from datetime import datetime, timedelta
import hashlib

# ==================== CONSISTENT HASHING ====================

class ConsistentHashRing:
    """
    Implementation of consistent hashing for distributed systems
    Used in load balancing, distributed caching, and database sharding
    """
    
    def __init__(self, nodes: List[str] = None, virtual_nodes: int = 150):
        self.virtual_nodes = virtual_nodes
        self.ring: Dict[int, str] = {}
        self.sorted_keys: List[int] = []
        
        if nodes:
            for node in nodes:
                self.add_node(node)
    
    def _hash(self, key: str) -> int:
        """Generate hash for a key"""
        return int(hashlib.md5(key.encode()).hexdigest(), 16)
    
    def add_node(self, node: str):
        """Add a node to the hash ring"""
        for i in range(self.virtual_nodes):
            virtual_key = f"{node}:{i}"
            hash_value = self._hash(virtual_key)
            self.ring[hash_value] = node
        
        self._update_sorted_keys()
        print(f"✅ Added node {node} with {self.virtual_nodes} virtual nodes")
    
    def remove_node(self, node: str):
        """Remove a node from the hash ring"""
        keys_to_remove = []
        for hash_value, ring_node in self.ring.items():
            if ring_node == node:
                keys_to_remove.append(hash_value)
        
        for key in keys_to_remove:
            del self.ring[key]
        
        self._update_sorted_keys()
        print(f"❌ Removed node {node}")
    
    def _update_sorted_keys(self):
        """Update sorted keys for binary search"""
        self.sorted_keys = sorted(self.ring.keys())
    
    def get_node(self, key: str) -> Optional[str]:
        """Get the node responsible for a key"""
        if not self.ring:
            return None
        
        hash_value = self._hash(key)
        
        # Find the first node clockwise from the hash
        for ring_key in self.sorted_keys:
            if hash_value <= ring_key:
                return self.ring[ring_key]
        
        # Wrap around to the first node
        return self.ring[self.sorted_keys[0]]
    
    def get_nodes(self, key: str, count: int = 1) -> List[str]:
        """Get multiple nodes for replication"""
        if not self.ring or count <= 0:
            return []
        
        hash_value = self._hash(key)
        nodes = []
        seen_nodes = set()
        
        # Start from the responsible node and go clockwise
        start_index = 0
        for i, ring_key in enumerate(self.sorted_keys):
            if hash_value <= ring_key:
                start_index = i
                break
        
        # Collect unique nodes
        for i in range(len(self.sorted_keys)):
            index = (start_index + i) % len(self.sorted_keys)
            node = self.ring[self.sorted_keys[index]]
            
            if node not in seen_nodes:
                nodes.append(node)
                seen_nodes.add(node)
                
                if len(nodes) >= count:
                    break
        
        return nodes
    
    def get_distribution(self, keys: List[str]) -> Dict[str, int]:
        """Analyze key distribution across nodes"""
        distribution = defaultdict(int)
        
        for key in keys:
            node = self.get_node(key)
            if node:
                distribution[node] += 1
        
        return dict(distribution)
    
    def visualize_ring(self):
        """Visualize the hash ring"""
        print(f"\n🔄 Hash Ring Visualization ({len(set(self.ring.values()))} nodes):")
        print("=" * 60)
        
        for i, (hash_value, node) in enumerate(sorted(self.ring.items())[:10]):
            print(f"Hash: {hash_value:>15} -> Node: {node}")
        
        if len(self.ring) > 10:
            print(f"... and {len(self.ring) - 10} more virtual nodes")

# ==================== RATE LIMITER ====================

class TokenBucket:
    """
    Token bucket algorithm for rate limiting
    """
    
    def __init__(self, capacity: int, refill_rate: float):
        self.capacity = capacity
        self.tokens = capacity
        self.refill_rate = refill_rate  # tokens per second
        self.last_refill = time.time()
        self.lock = threading.Lock()
    
    def consume(self, tokens: int = 1) -> bool:
        """Try to consume tokens from the bucket"""
        with self.lock:
            self._refill()
            
            if self.tokens >= tokens:
                self.tokens -= tokens
                return True
            return False
    
    def _refill(self):
        """Refill tokens based on elapsed time"""
        now = time.time()
        elapsed = now - self.last_refill
        tokens_to_add = elapsed * self.refill_rate
        
        self.tokens = min(self.capacity, self.tokens + tokens_to_add)
        self.last_refill = now
    
    def get_status(self) -> Dict[str, Any]:
        """Get current bucket status"""
        with self.lock:
            self._refill()
            return {
                'tokens': self.tokens,
                'capacity': self.capacity,
                'refill_rate': self.refill_rate,
                'utilization': (self.capacity - self.tokens) / self.capacity
            }

class SlidingWindowRateLimiter:
    """
    Sliding window rate limiter
    """
    
    def __init__(self, window_size: int, max_requests: int):
        self.window_size = window_size  # seconds
        self.max_requests = max_requests
        self.requests: Dict[str, deque] = defaultdict(deque)
        self.lock = threading.Lock()
    
    def is_allowed(self, client_id: str) -> bool:
        """Check if request is allowed for client"""
        with self.lock:
            now = time.time()
            client_requests = self.requests[client_id]
            
            # Remove old requests outside the window
            while client_requests and client_requests[0] <= now - self.window_size:
                client_requests.popleft()
            
            # Check if we can accept new request
            if len(client_requests) < self.max_requests:
                client_requests.append(now)
                return True
            
            return False
    
    def get_stats(self, client_id: str) -> Dict[str, Any]:
        """Get statistics for a client"""
        with self.lock:
            now = time.time()
            client_requests = self.requests[client_id]
            
            # Clean old requests
            while client_requests and client_requests[0] <= now - self.window_size:
                client_requests.popleft()
            
            return {
                'current_requests': len(client_requests),
                'max_requests': self.max_requests,
                'window_size': self.window_size,
                'requests_remaining': self.max_requests - len(client_requests)
            }

# ==================== CIRCUIT BREAKER ====================

class CircuitBreakerState:
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"

class AdvancedCircuitBreaker:
    """
    Advanced circuit breaker with statistics and configuration
    """
    
    def __init__(self, 
                 failure_threshold: int = 5,
                 recovery_timeout: int = 60,
                 expected_exception: type = Exception,
                 half_open_max_calls: int = 3):
        
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.expected_exception = expected_exception
        self.half_open_max_calls = half_open_max_calls
        
        # State
        self.state = CircuitBreakerState.CLOSED
        self.failure_count = 0
        self.last_failure_time = None
        self.half_open_calls = 0
        
        # Statistics
        self.total_calls = 0
        self.successful_calls = 0
        self.failed_calls = 0
        self.state_changes = []
        
        self.lock = threading.Lock()
    
    def call(self, func: Callable, *args, **kwargs):
        """Execute function with circuit breaker protection"""
        with self.lock:
            self.total_calls += 1
            
            if self.state == CircuitBreakerState.OPEN:
                if self._should_attempt_reset():
                    self._change_state(CircuitBreakerState.HALF_OPEN)
                    self.half_open_calls = 0
                else:
                    raise Exception("Circuit breaker is OPEN")
            
            if self.state == CircuitBreakerState.HALF_OPEN:
                if self.half_open_calls >= self.half_open_max_calls:
                    raise Exception("Circuit breaker HALF_OPEN limit reached")
                self.half_open_calls += 1
        
        try:
            result = func(*args, **kwargs)
            self._on_success()
            return result
        except self.expected_exception as e:
            self._on_failure()
            raise e
    
    def _should_attempt_reset(self) -> bool:
        return (self.last_failure_time and 
                time.time() - self.last_failure_time >= self.recovery_timeout)
    
    def _on_success(self):
        with self.lock:
            self.successful_calls += 1
            self.failure_count = 0
            
            if self.state == CircuitBreakerState.HALF_OPEN:
                self._change_state(CircuitBreakerState.CLOSED)
    
    def _on_failure(self):
        with self.lock:
            self.failed_calls += 1
            self.failure_count += 1
            self.last_failure_time = time.time()
            
            if (self.state == CircuitBreakerState.CLOSED and 
                self.failure_count >= self.failure_threshold):
                self._change_state(CircuitBreakerState.OPEN)
            elif self.state == CircuitBreakerState.HALF_OPEN:
                self._change_state(CircuitBreakerState.OPEN)
    
    def _change_state(self, new_state: str):
        old_state = self.state
        self.state = new_state
        self.state_changes.append({
            'from': old_state,
            'to': new_state,
            'timestamp': time.time(),
            'failure_count': self.failure_count
        })
        print(f"🔄 Circuit breaker: {old_state} -> {new_state}")
    
    def get_stats(self) -> Dict[str, Any]:
        """Get circuit breaker statistics"""
        with self.lock:
            success_rate = (self.successful_calls / self.total_calls * 100 
                          if self.total_calls > 0 else 0)
            
            return {
                'state': self.state,
                'total_calls': self.total_calls,
                'successful_calls': self.successful_calls,
                'failed_calls': self.failed_calls,
                'success_rate': round(success_rate, 2),
                'failure_count': self.failure_count,
                'state_changes': len(self.state_changes),
                'last_failure_time': self.last_failure_time
            }

# ==================== LOAD BALANCER SIMULATOR ====================

class LoadBalancerAlgorithm:
    """Base class for load balancing algorithms"""
    
    def __init__(self, servers: List[str]):
        self.servers = servers
        self.server_stats = {server: {'requests': 0, 'response_time': 0} 
                           for server in servers}
    
    def select_server(self) -> str:
        raise NotImplementedError
    
    def record_request(self, server: str, response_time: float):
        self.server_stats[server]['requests'] += 1
        self.server_stats[server]['response_time'] += response_time

class RoundRobinBalancer(LoadBalancerAlgorithm):
    def __init__(self, servers: List[str]):
        super().__init__(servers)
        self.current = 0
    
    def select_server(self) -> str:
        server = self.servers[self.current]
        self.current = (self.current + 1) % len(self.servers)
        return server

class WeightedRoundRobinBalancer(LoadBalancerAlgorithm):
    def __init__(self, servers: List[str], weights: List[int]):
        super().__init__(servers)
        self.weights = weights
        self.weighted_servers = []
        
        for server, weight in zip(servers, weights):
            self.weighted_servers.extend([server] * weight)
        
        self.current = 0
    
    def select_server(self) -> str:
        server = self.weighted_servers[self.current]
        self.current = (self.current + 1) % len(self.weighted_servers)
        return server

class LeastConnectionsBalancer(LoadBalancerAlgorithm):
    def __init__(self, servers: List[str]):
        super().__init__(servers)
        self.connections = {server: 0 for server in servers}
    
    def select_server(self) -> str:
        return min(self.connections.keys(), key=lambda x: self.connections[x])
    
    def record_request(self, server: str, response_time: float):
        super().record_request(server, response_time)
        self.connections[server] += 1
    
    def release_connection(self, server: str):
        if self.connections[server] > 0:
            self.connections[server] -= 1

# ==================== DEMONSTRATIONS ====================

def demonstrate_consistent_hashing():
    """Demonstrate consistent hashing"""
    print("🔄 Consistent Hashing Demonstration")
    print("=" * 50)
    
    # Create hash ring with initial nodes
    ring = ConsistentHashRing(['server1', 'server2', 'server3'])
    ring.visualize_ring()
    
    # Test key distribution
    test_keys = [f"key_{i}" for i in range(100)]
    distribution = ring.get_distribution(test_keys)
    
    print(f"\n📊 Initial Distribution (100 keys):")
    for node, count in distribution.items():
        print(f"{node}: {count} keys ({count}%)")
    
    # Add a new node and see redistribution
    print(f"\n➕ Adding server4...")
    ring.add_node('server4')
    
    new_distribution = ring.get_distribution(test_keys)
    print(f"\n📊 After Adding server4:")
    for node, count in new_distribution.items():
        print(f"{node}: {count} keys ({count}%)")
    
    # Calculate data movement
    moved_keys = 0
    for key in test_keys:
        old_node = None
        for node, old_count in distribution.items():
            if ring.get_node(key) != node and key in [f"key_{i}" for i in range(old_count)]:
                moved_keys += 1
                break
    
    print(f"\n📈 Keys redistributed: ~{25}% (expected for adding 1 node to 3)")

def demonstrate_rate_limiting():
    """Demonstrate rate limiting algorithms"""
    print("\n🚦 Rate Limiting Demonstration")
    print("=" * 50)
    
    # Token bucket demonstration
    print("🪣 Token Bucket Algorithm:")
    bucket = TokenBucket(capacity=5, refill_rate=2.0)  # 2 tokens/second
    
    # Burst of requests
    print("Initial burst of 7 requests:")
    for i in range(7):
        allowed = bucket.consume()
        status = bucket.get_status()
        print(f"Request {i+1}: {'✅ Allowed' if allowed else '❌ Rejected'} "
              f"(tokens: {status['tokens']:.1f})")
    
    # Wait and try again
    print("\nWaiting 2 seconds for refill...")
    time.sleep(2)
    
    print("After refill:")
    for i in range(3):
        allowed = bucket.consume()
        status = bucket.get_status()
        print(f"Request {i+1}: {'✅ Allowed' if allowed else '❌ Rejected'} "
              f"(tokens: {status['tokens']:.1f})")
    
    # Sliding window demonstration
    print(f"\n🪟 Sliding Window Algorithm:")
    limiter = SlidingWindowRateLimiter(window_size=5, max_requests=3)
    
    client_id = "user123"
    print(f"Client {client_id} making requests (3 requests per 5 seconds):")
    
    for i in range(5):
        allowed = limiter.is_allowed(client_id)
        stats = limiter.get_stats(client_id)
        print(f"Request {i+1}: {'✅ Allowed' if allowed else '❌ Rejected'} "
              f"(remaining: {stats['requests_remaining']})")
        time.sleep(0.5)

def demonstrate_circuit_breaker():
    """Demonstrate circuit breaker pattern"""
    print("\n⚡ Circuit Breaker Demonstration")
    print("=" * 50)
    
    # Create circuit breaker
    cb = AdvancedCircuitBreaker(failure_threshold=3, recovery_timeout=5)
    
    def unreliable_service(fail_rate: float = 0.7):
        """Simulate an unreliable external service"""
        time.sleep(0.1)  # Simulate processing time
        if random.random() < fail_rate:
            raise Exception("Service unavailable")
        return "Success"
    
    # Test circuit breaker behavior
    print("Testing with 70% failure rate:")
    
    for i in range(10):
        try:
            result = cb.call(unreliable_service)
            print(f"Call {i+1}: ✅ {result}")
        except Exception as e:
            print(f"Call {i+1}: ❌ {str(e)}")
        
        time.sleep(0.1)
    
    # Show statistics
    stats = cb.get_stats()
    print(f"\n📊 Circuit Breaker Statistics:")
    print(f"State: {stats['state']}")
    print(f"Total calls: {stats['total_calls']}")
    print(f"Success rate: {stats['success_rate']}%")
    print(f"State changes: {stats['state_changes']}")

def demonstrate_load_balancing():
    """Demonstrate load balancing algorithms"""
    print("\n⚖️ Load Balancing Demonstration")
    print("=" * 50)
    
    servers = ['server1', 'server2', 'server3']
    
    # Round Robin
    print("🔄 Round Robin Algorithm:")
    rr_balancer = RoundRobinBalancer(servers)
    
    for i in range(6):
        server = rr_balancer.select_server()
        print(f"Request {i+1} -> {server}")
    
    # Weighted Round Robin
    print(f"\n⚖️ Weighted Round Robin (weights: [3, 2, 1]):")
    weights = [3, 2, 1]
    wrr_balancer = WeightedRoundRobinBalancer(servers, weights)
    
    for i in range(12):
        server = wrr_balancer.select_server()
        print(f"Request {i+1} -> {server}")
    
    # Least Connections
    print(f"\n🔗 Least Connections Algorithm:")
    lc_balancer = LeastConnectionsBalancer(servers)
    
    # Simulate varying connection loads
    for i in range(8):
        server = lc_balancer.select_server()
        response_time = random.uniform(0.1, 0.5)
        lc_balancer.record_request(server, response_time)
        
        print(f"Request {i+1} -> {server} "
              f"(connections: {lc_balancer.connections[server]})")
        
        # Randomly release some connections
        if random.random() < 0.3:
            lc_balancer.release_connection(server)

def main():
    """Main demonstration function"""
    print("🛠️ System Design Tools & Utilities Demo")
    print("=" * 60)
    
    try:
        demonstrate_consistent_hashing()
        demonstrate_rate_limiting()
        demonstrate_circuit_breaker()
        demonstrate_load_balancing()
        
        print("\n" + "=" * 60)
        print("🎯 Tools Demonstrated:")
        print("• Consistent Hashing: Distributed data placement")
        print("• Rate Limiting: API protection and fair usage")
        print("• Circuit Breaker: Fault tolerance and resilience")
        print("• Load Balancing: Traffic distribution algorithms")
        
        print("\n💡 Practical Applications:")
        print("• Use these patterns in production systems")
        print("• Combine multiple patterns for robust architecture")
        print("• Monitor and tune parameters based on real traffic")
        print("• Test failure scenarios before production deployment")
        
    except Exception as e:
        print(f"❌ Demo failed: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
