#!/usr/bin/env python3
"""
System Design Toolkit - Production-Ready Utilities

This toolkit provides essential utilities for system design implementation,
testing, and monitoring. It includes patterns commonly used in distributed
systems and microservices architectures.

Author: System Design Mastery
Version: 1.0.0
"""

import time
import random
import threading
import json
import hashlib
import logging
from typing import Dict, List, Optional, Callable, Any
from dataclasses import dataclass
from enum import Enum
from collections import defaultdict
import asyncio
from datetime import datetime, timedelta
import uuid

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

class CircuitBreakerState(Enum):
    """Circuit breaker states"""
    CLOSED = "closed"
    OPEN = "open"
    HALF_OPEN = "half_open"

class CircuitBreaker:
    """
    Circuit Breaker Pattern Implementation
    
    Prevents cascading failures by temporarily blocking calls to a failing service.
    Useful for handling external service dependencies and preventing system overload.
    """
    
    def __init__(self, 
                 failure_threshold: int = 5,
                 success_threshold: int = 3, 
                 timeout: float = 60.0):
        self.failure_threshold = failure_threshold
        self.success_threshold = success_threshold
        self.timeout = timeout
        
        self.failure_count = 0
        self.success_count = 0
        self.last_failure_time = None
        self.state = CircuitBreakerState.CLOSED
        self.lock = threading.Lock()
        
        self.logger = logging.getLogger(f"{__name__}.CircuitBreaker")
    
    def call(self, func: Callable, *args, **kwargs) -> Any:
        """Execute function with circuit breaker protection"""
        with self.lock:
            if self.state == CircuitBreakerState.OPEN:
                if self._should_attempt_reset():
                    self.state = CircuitBreakerState.HALF_OPEN
                    self.logger.info("Circuit breaker moving to HALF_OPEN state")
                else:
                    raise CircuitBreakerException("Circuit breaker is OPEN")
            
            try:
                result = func(*args, **kwargs)
                self._on_success()
                return result
                
            except Exception as e:
                self._on_failure()
                raise e
    
    def _should_attempt_reset(self) -> bool:
        """Check if enough time has passed to attempt reset"""
        if self.last_failure_time is None:
            return True
        return time.time() - self.last_failure_time >= self.timeout
    
    def _on_success(self):
        """Handle successful call"""
        if self.state == CircuitBreakerState.HALF_OPEN:
            self.success_count += 1
            if self.success_count >= self.success_threshold:
                self.state = CircuitBreakerState.CLOSED
                self.failure_count = 0
                self.success_count = 0
                self.logger.info("Circuit breaker CLOSED - service recovered")
        else:
            self.failure_count = max(0, self.failure_count - 1)
    
    def _on_failure(self):
        """Handle failed call"""
        self.failure_count += 1
        self.last_failure_time = time.time()
        
        if self.failure_count >= self.failure_threshold:
            self.state = CircuitBreakerState.OPEN
            self.success_count = 0
            self.logger.warning(f"Circuit breaker OPEN - failure threshold reached ({self.failure_count})")

class CircuitBreakerException(Exception):
    """Exception raised when circuit breaker is open"""
    pass

class RateLimiter:
    """
    Token Bucket Rate Limiter Implementation
    
    Controls the rate of requests to prevent system overload.
    Commonly used in API gateways and service protection.
    """
    
    def __init__(self, capacity: int, refill_rate: float):
        self.capacity = capacity
        self.refill_rate = refill_rate  # tokens per second
        self.tokens = capacity
        self.last_refill = time.time()
        self.lock = threading.Lock()
        
        self.logger = logging.getLogger(f"{__name__}.RateLimiter")
    
    def is_allowed(self, tokens_requested: int = 1) -> bool:
        """Check if request is allowed under rate limit"""
        with self.lock:
            self._refill()
            
            if self.tokens >= tokens_requested:
                self.tokens -= tokens_requested
                return True
            
            self.logger.warning(f"Rate limit exceeded - tokens available: {self.tokens}, requested: {tokens_requested}")
            return False
    
    def _refill(self):
        """Refill tokens based on elapsed time"""
        now = time.time()
        elapsed = now - self.last_refill
        tokens_to_add = elapsed * self.refill_rate
        
        self.tokens = min(self.capacity, self.tokens + tokens_to_add)
        self.last_refill = now

class ConsistentHashing:
    """
    Consistent Hashing Implementation
    
    Distributes keys across nodes with minimal redistribution when nodes are added/removed.
    Essential for distributed caching and database sharding.
    """
    
    def __init__(self, nodes: List[str] = None, replicas: int = 100):
        self.replicas = replicas
        self.ring = {}
        self.sorted_keys = []
        
        if nodes:
            for node in nodes:
                self.add_node(node)
    
    def _hash(self, key: str) -> int:
        """Generate hash for a key"""
        return int(hashlib.md5(key.encode()).hexdigest(), 16)
    
    def add_node(self, node: str):
        """Add a node to the hash ring"""
        for i in range(self.replicas):
            replica_key = f"{node}:{i}"
            hash_value = self._hash(replica_key)
            self.ring[hash_value] = node
            self.sorted_keys.append(hash_value)
        
        self.sorted_keys.sort()
    
    def remove_node(self, node: str):
        """Remove a node from the hash ring"""
        for i in range(self.replicas):
            replica_key = f"{node}:{i}"
            hash_value = self._hash(replica_key)
            if hash_value in self.ring:
                del self.ring[hash_value]
                self.sorted_keys.remove(hash_value)
    
    def get_node(self, key: str) -> Optional[str]:
        """Get the node responsible for a key"""
        if not self.ring:
            return None
        
        hash_value = self._hash(key)
        
        # Find the first node with hash >= key hash
        for ring_key in self.sorted_keys:
            if ring_key >= hash_value:
                return self.ring[ring_key]
        
        # Wrap around to the first node
        return self.ring[self.sorted_keys[0]]
    
    def get_nodes(self, key: str, count: int) -> List[str]:
        """Get multiple nodes for replication"""
        if not self.ring or count <= 0:
            return []
        
        hash_value = self._hash(key)
        nodes = []
        seen_nodes = set()
        
        # Start from the position in the ring
        start_idx = 0
        for i, ring_key in enumerate(self.sorted_keys):
            if ring_key >= hash_value:
                start_idx = i
                break
        
        # Collect unique nodes
        idx = start_idx
        while len(nodes) < count and len(seen_nodes) < len(set(self.ring.values())):
            node = self.ring[self.sorted_keys[idx]]
            if node not in seen_nodes:
                nodes.append(node)
                seen_nodes.add(node)
            idx = (idx + 1) % len(self.sorted_keys)
        
        return nodes

@dataclass
class HealthCheck:
    """Health check result"""
    service_name: str
    status: str
    response_time_ms: float
    message: str
    timestamp: datetime

class HealthChecker:
    """
    Health Check System
    
    Monitors service health and provides status endpoints.
    Essential for load balancers and service discovery.
    """
    
    def __init__(self):
        self.checks = {}
        self.results = {}
        self.logger = logging.getLogger(f"{__name__}.HealthChecker")
    
    def register_check(self, name: str, check_func: Callable[[], bool], 
                      timeout: float = 5.0):
        """Register a health check function"""
        self.checks[name] = {
            'func': check_func,
            'timeout': timeout
        }
    
    def run_check(self, name: str) -> HealthCheck:
        """Run a specific health check"""
        if name not in self.checks:
            return HealthCheck(
                service_name=name,
                status="UNKNOWN",
                response_time_ms=0.0,
                message="Health check not registered",
                timestamp=datetime.now()
            )
        
        start_time = time.time()
        try:
            check_func = self.checks[name]['func']
            timeout = self.checks[name]['timeout']
            
            # Simple timeout implementation
            result = check_func()
            response_time = (time.time() - start_time) * 1000
            
            status = "HEALTHY" if result else "UNHEALTHY"
            message = "OK" if result else "Check failed"
            
            return HealthCheck(
                service_name=name,
                status=status,
                response_time_ms=response_time,
                message=message,
                timestamp=datetime.now()
            )
            
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            return HealthCheck(
                service_name=name,
                status="UNHEALTHY",
                response_time_ms=response_time,
                message=str(e),
                timestamp=datetime.now()
            )
    
    def run_all_checks(self) -> Dict[str, HealthCheck]:
        """Run all registered health checks"""
        results = {}
        for name in self.checks:
            results[name] = self.run_check(name)
        
        self.results = results
        return results
    
    def get_overall_status(self) -> str:
        """Get overall system health status"""
        if not self.results:
            return "UNKNOWN"
        
        unhealthy_count = sum(1 for result in self.results.values() 
                            if result.status == "UNHEALTHY")
        
        if unhealthy_count == 0:
            return "HEALTHY"
        elif unhealthy_count < len(self.results):
            return "DEGRADED"
        else:
            return "UNHEALTHY"

class ServiceRegistry:
    """
    Service Discovery Registry
    
    Maintains a registry of available services and their locations.
    Essential for microservices communication.
    """
    
    def __init__(self):
        self.services = defaultdict(list)
        self.lock = threading.Lock()
        self.logger = logging.getLogger(f"{__name__}.ServiceRegistry")
    
    def register_service(self, service_name: str, host: str, port: int, 
                        metadata: Dict = None):
        """Register a service instance"""
        service_info = {
            'host': host,
            'port': port,
            'metadata': metadata or {},
            'registered_at': datetime.now(),
            'last_heartbeat': datetime.now()
        }
        
        with self.lock:
            self.services[service_name].append(service_info)
        
        self.logger.info(f"Registered service {service_name} at {host}:{port}")
    
    def deregister_service(self, service_name: str, host: str, port: int):
        """Deregister a service instance"""
        with self.lock:
            self.services[service_name] = [
                service for service in self.services[service_name]
                if not (service['host'] == host and service['port'] == port)
            ]
        
        self.logger.info(f"Deregistered service {service_name} at {host}:{port}")
    
    def discover_service(self, service_name: str) -> List[Dict]:
        """Discover available instances of a service"""
        with self.lock:
            return list(self.services.get(service_name, []))
    
    def heartbeat(self, service_name: str, host: str, port: int):
        """Update heartbeat for a service instance"""
        with self.lock:
            for service in self.services[service_name]:
                if service['host'] == host and service['port'] == port:
                    service['last_heartbeat'] = datetime.now()
                    break
    
    def cleanup_stale_services(self, timeout_seconds: int = 30):
        """Remove services that haven't sent heartbeat recently"""
        cutoff_time = datetime.now() - timedelta(seconds=timeout_seconds)
        
        with self.lock:
            for service_name in self.services:
                self.services[service_name] = [
                    service for service in self.services[service_name]
                    if service['last_heartbeat'] > cutoff_time
                ]

class LoadBalancer:
    """
    Load Balancer with Multiple Strategies
    
    Distributes requests across multiple service instances.
    """
    
    def __init__(self, strategy: str = "round_robin"):
        self.strategy = strategy
        self.current_index = 0
        self.lock = threading.Lock()
        self.logger = logging.getLogger(f"{__name__}.LoadBalancer")
    
    def select_instance(self, instances: List[Dict]) -> Optional[Dict]:
        """Select an instance based on the load balancing strategy"""
        if not instances:
            return None
        
        if self.strategy == "round_robin":
            return self._round_robin(instances)
        elif self.strategy == "random":
            return self._random(instances)
        elif self.strategy == "least_connections":
            return self._least_connections(instances)
        else:
            return self._round_robin(instances)
    
    def _round_robin(self, instances: List[Dict]) -> Dict:
        """Round-robin selection"""
        with self.lock:
            instance = instances[self.current_index % len(instances)]
            self.current_index += 1
            return instance
    
    def _random(self, instances: List[Dict]) -> Dict:
        """Random selection"""
        return random.choice(instances)
    
    def _least_connections(self, instances: List[Dict]) -> Dict:
        """Select instance with least connections (simplified)"""
        # In a real implementation, you'd track actual connection counts
        return min(instances, key=lambda x: x.get('connections', 0))

class MetricsCollector:
    """
    Metrics Collection System
    
    Collects and stores system metrics for monitoring and alerting.
    """
    
    def __init__(self):
        self.metrics = defaultdict(list)
        self.lock = threading.Lock()
        self.logger = logging.getLogger(f"{__name__}.MetricsCollector")
    
    def record_counter(self, name: str, value: float = 1, tags: Dict = None):
        """Record a counter metric"""
        metric = {
            'type': 'counter',
            'value': value,
            'tags': tags or {},
            'timestamp': datetime.now()
        }
        
        with self.lock:
            self.metrics[name].append(metric)
    
    def record_gauge(self, name: str, value: float, tags: Dict = None):
        """Record a gauge metric"""
        metric = {
            'type': 'gauge',
            'value': value,
            'tags': tags or {},
            'timestamp': datetime.now()
        }
        
        with self.lock:
            self.metrics[name].append(metric)
    
    def record_histogram(self, name: str, value: float, tags: Dict = None):
        """Record a histogram metric"""
        metric = {
            'type': 'histogram',
            'value': value,
            'tags': tags or {},
            'timestamp': datetime.now()
        }
        
        with self.lock:
            self.metrics[name].append(metric)
    
    def get_metrics(self, name: str, since: datetime = None) -> List[Dict]:
        """Get metrics for a given name"""
        with self.lock:
            metrics = self.metrics.get(name, [])
            
            if since:
                metrics = [m for m in metrics if m['timestamp'] >= since]
            
            return list(metrics)
    
    def get_summary(self, name: str, since: datetime = None) -> Dict:
        """Get summary statistics for a metric"""
        metrics = self.get_metrics(name, since)
        
        if not metrics:
            return {}
        
        values = [m['value'] for m in metrics]
        
        return {
            'count': len(values),
            'sum': sum(values),
            'min': min(values),
            'max': max(values),
            'avg': sum(values) / len(values),
            'latest': values[-1] if values else None
        }

# Demo and testing functions
def demo_circuit_breaker():
    """Demonstrate circuit breaker functionality"""
    print("\\n=== Circuit Breaker Demo ===")
    
    def unreliable_service():
        """Simulates an unreliable service"""
        if random.random() < 0.7:  # 70% failure rate
            raise Exception("Service unavailable")
        return "Success"
    
    cb = CircuitBreaker(failure_threshold=3, timeout=5.0)
    
    for i in range(10):
        try:
            result = cb.call(unreliable_service)
            print(f"Call {i+1}: {result} (State: {cb.state.value})")
        except Exception as e:
            print(f"Call {i+1}: Failed - {e} (State: {cb.state.value})")
        
        time.sleep(0.5)

def demo_rate_limiter():
    """Demonstrate rate limiter functionality"""
    print("\\n=== Rate Limiter Demo ===")
    
    limiter = RateLimiter(capacity=5, refill_rate=1.0)  # 5 requests, 1 per second refill
    
    for i in range(8):
        allowed = limiter.is_allowed()
        print(f"Request {i+1}: {'ALLOWED' if allowed else 'DENIED'} (Tokens: {limiter.tokens:.2f})")
        time.sleep(0.5)

def demo_consistent_hashing():
    """Demonstrate consistent hashing functionality"""
    print("\\n=== Consistent Hashing Demo ===")
    
    # Create hash ring with 3 nodes
    nodes = ["server1", "server2", "server3"]
    ch = ConsistentHashing(nodes)
    
    # Test key distribution
    keys = ["user1", "user2", "user3", "user4", "user5"]
    print("Initial distribution:")
    for key in keys:
        node = ch.get_node(key)
        print(f"  {key} -> {node}")
    
    # Add a new node
    print("\\nAfter adding server4:")
    ch.add_node("server4")
    for key in keys:
        node = ch.get_node(key)
        print(f"  {key} -> {node}")

def demo_health_checker():
    """Demonstrate health checker functionality"""
    print("\\n=== Health Checker Demo ===")
    
    hc = HealthChecker()
    
    # Register health checks
    def db_check():
        return random.random() > 0.2  # 80% success rate
    
    def cache_check():
        return random.random() > 0.1  # 90% success rate
    
    def api_check():
        return random.random() > 0.3  # 70% success rate
    
    hc.register_check("database", db_check)
    hc.register_check("cache", cache_check)
    hc.register_check("external_api", api_check)
    
    # Run checks
    results = hc.run_all_checks()
    
    print(f"Overall Status: {hc.get_overall_status()}")
    for name, result in results.items():
        print(f"  {name}: {result.status} ({result.response_time_ms:.2f}ms) - {result.message}")

if __name__ == "__main__":
    print("System Design Toolkit Demo")
    print("=" * 40)
    
    # Run all demos
    demo_circuit_breaker()
    demo_rate_limiter()
    demo_consistent_hashing()
    demo_health_checker()
    
    print("\\n=== Demo Complete ===")
    print("\\nThis toolkit provides essential patterns for building robust distributed systems.")
    print("Each component can be used independently or combined for comprehensive system design.")
