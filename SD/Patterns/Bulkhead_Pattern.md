# Bulkhead Pattern

## Overview

The Bulkhead Pattern is a design pattern that isolates different parts of a system to prevent cascading failures. Named after the watertight compartments in ships that prevent the entire vessel from sinking if one compartment is breached, this pattern compartmentalizes resources and functionality to limit the impact of failures.

## Key Concepts

### 1. Resource Isolation
- Separate thread pools for different operations
- Dedicated connection pools
- Isolated memory allocation
- CPU resource partitioning

### 2. Failure Containment
- Prevents failure cascade
- Limits blast radius
- Maintains service availability
- Protects critical functions

### 3. Compartmentalization
- Logical separation of concerns
- Physical resource separation
- Independent scaling
- Isolated monitoring

## Implementation Examples

### 1. Thread Pool Isolation

```python
import threading
import queue
import time
from concurrent.futures import ThreadPoolExecutor
from typing import Callable, Any
from dataclasses import dataclass

@dataclass
class BulkheadConfig:
    name: str
    max_workers: int
    queue_size: int
    timeout: float

class ThreadPoolBulkhead:
    def __init__(self, config: BulkheadConfig):
        self.config = config
        self.executor = ThreadPoolExecutor(
            max_workers=config.max_workers,
            thread_name_prefix=f"bulkhead-{config.name}"
        )
        self.task_queue = queue.Queue(maxsize=config.queue_size)
        self.stats = {
            "submitted": 0,
            "completed": 0,
            "failed": 0,
            "rejected": 0
        }
    
    def submit_task(self, task: Callable, *args, **kwargs):
        try:
            future = self.executor.submit(self._execute_task, task, *args, **kwargs)
            self.stats["submitted"] += 1
            return future
        except RuntimeError:
            self.stats["rejected"] += 1
            raise BulkheadRejectedException(f"Bulkhead {self.config.name} rejected task")
    
    def _execute_task(self, task: Callable, *args, **kwargs):
        try:
            result = task(*args, **kwargs)
            self.stats["completed"] += 1
            return result
        except Exception as e:
            self.stats["failed"] += 1
            raise e
    
    def get_stats(self):
        return {
            "name": self.config.name,
            "active_threads": self.executor._threads.__len__(),
            "max_workers": self.config.max_workers,
            **self.stats
        }
    
    def shutdown(self):
        self.executor.shutdown(wait=True)

class BulkheadRejectedException(Exception):
    pass

# Service with multiple bulkheads
class ECommerceService:
    def __init__(self):
        # Separate bulkheads for different operations
        self.user_ops_bulkhead = ThreadPoolBulkhead(
            BulkheadConfig("user-ops", max_workers=5, queue_size=20, timeout=2.0)
        )
        self.order_ops_bulkhead = ThreadPoolBulkhead(
            BulkheadConfig("order-ops", max_workers=10, queue_size=50, timeout=5.0)
        )
        self.reporting_bulkhead = ThreadPoolBulkhead(
            BulkheadConfig("reporting", max_workers=2, queue_size=10, timeout=30.0)
        )
    
    def create_user(self, user_data):
        return self.user_ops_bulkhead.submit_task(self._create_user_task, user_data)
    
    def process_order(self, order_data):
        return self.order_ops_bulkhead.submit_task(self._process_order_task, order_data)
    
    def generate_report(self, report_type):
        return self.reporting_bulkhead.submit_task(self._generate_report_task, report_type)
    
    def _create_user_task(self, user_data):
        # Simulate user creation
        time.sleep(0.5)  # Database operation
        return f"User created: {user_data['username']}"
    
    def _process_order_task(self, order_data):
        # Simulate order processing
        time.sleep(1.0)  # Payment processing
        return f"Order processed: {order_data['order_id']}"
    
    def _generate_report_task(self, report_type):
        # Simulate heavy reporting operation
        time.sleep(5.0)  # Complex query
        return f"Report generated: {report_type}"
    
    def get_all_stats(self):
        return {
            "user_ops": self.user_ops_bulkhead.get_stats(),
            "order_ops": self.order_ops_bulkhead.get_stats(),
            "reporting": self.reporting_bulkhead.get_stats()
        }
```

### 2. Connection Pool Isolation

```python
import sqlite3
from contextlib import contextmanager
from typing import Dict, Any

class ConnectionPoolBulkhead:
    def __init__(self, db_path: str, pool_name: str, max_connections: int):
        self.db_path = db_path
        self.pool_name = pool_name
        self.max_connections = max_connections
        self.connections = queue.Queue(maxsize=max_connections)
        self.stats = {
            "connections_created": 0,
            "connections_in_use": 0,
            "connection_errors": 0
        }
        
        # Pre-populate connection pool
        for _ in range(max_connections):
            conn = sqlite3.connect(db_path)
            self.connections.put(conn)
            self.stats["connections_created"] += 1
    
    @contextmanager
    def get_connection(self, timeout: float = 5.0):
        conn = None
        try:
            conn = self.connections.get(timeout=timeout)
            self.stats["connections_in_use"] += 1
            yield conn
        except queue.Empty:
            raise ConnectionPoolExhaustedException(f"No connections available in {self.pool_name}")
        except Exception as e:
            self.stats["connection_errors"] += 1
            raise e
        finally:
            if conn:
                self.connections.put(conn)
                self.stats["connections_in_use"] -= 1

class ConnectionPoolExhaustedException(Exception):
    pass

class DatabaseService:
    def __init__(self):
        # Separate connection pools for different operations
        self.read_pool = ConnectionPoolBulkhead("app.db", "read-pool", max_connections=10)
        self.write_pool = ConnectionPoolBulkhead("app.db", "write-pool", max_connections=3)
        self.analytics_pool = ConnectionPoolBulkhead("analytics.db", "analytics-pool", max_connections=2)
    
    def read_user(self, user_id: int):
        with self.read_pool.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM users WHERE id = ?", (user_id,))
            return cursor.fetchone()
    
    def write_user(self, user_data: Dict[str, Any]):
        with self.write_pool.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO users (username, email) VALUES (?, ?)",
                (user_data["username"], user_data["email"])
            )
            conn.commit()
            return cursor.lastrowid
    
    def generate_analytics(self):
        with self.analytics_pool.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM users")
            return cursor.fetchone()[0]
```

### 3. Memory Isolation

```python
import psutil
import gc
from typing import Dict, Any, Optional

class MemoryBulkhead:
    def __init__(self, name: str, max_memory_mb: int):
        self.name = name
        self.max_memory_mb = max_memory_mb
        self.allocated_objects: Dict[str, Any] = {}
        self.stats = {
            "allocations": 0,
            "deallocations": 0,
            "memory_limit_exceeded": 0,
            "current_memory_mb": 0
        }
    
    def allocate(self, key: str, data: Any) -> bool:
        current_memory = self._get_current_memory_usage()
        
        if current_memory > self.max_memory_mb:
            self.stats["memory_limit_exceeded"] += 1
            return False
        
        self.allocated_objects[key] = data
        self.stats["allocations"] += 1
        self.stats["current_memory_mb"] = current_memory
        return True
    
    def deallocate(self, key: str) -> bool:
        if key in self.allocated_objects:
            del self.allocated_objects[key]
            self.stats["deallocations"] += 1
            gc.collect()  # Force garbage collection
            self.stats["current_memory_mb"] = self._get_current_memory_usage()
            return True
        return False
    
    def _get_current_memory_usage(self) -> float:
        process = psutil.Process()
        return process.memory_info().rss / 1024 / 1024  # Convert to MB
    
    def get_stats(self) -> Dict[str, Any]:
        return {
            "name": self.name,
            "max_memory_mb": self.max_memory_mb,
            "objects_count": len(self.allocated_objects),
            **self.stats
        }

class CacheService:
    def __init__(self):
        # Separate memory bulkheads for different cache types
        self.user_cache = MemoryBulkhead("user-cache", max_memory_mb=100)
        self.session_cache = MemoryBulkhead("session-cache", max_memory_mb=50)
        self.static_cache = MemoryBulkhead("static-cache", max_memory_mb=200)
    
    def cache_user(self, user_id: str, user_data: Dict[str, Any]) -> bool:
        return self.user_cache.allocate(f"user:{user_id}", user_data)
    
    def cache_session(self, session_id: str, session_data: Dict[str, Any]) -> bool:
        return self.session_cache.allocate(f"session:{session_id}", session_data)
    
    def cache_static_content(self, key: str, content: bytes) -> bool:
        return self.static_cache.allocate(f"static:{key}", content)
```

### 4. Circuit Breaker Integration

```python
from enum import Enum
import time

class CircuitState(Enum):
    CLOSED = "closed"
    OPEN = "open"
    HALF_OPEN = "half_open"

class CircuitBreakerBulkhead:
    def __init__(self, name: str, failure_threshold: int, recovery_timeout: float):
        self.name = name
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.failure_count = 0
        self.last_failure_time = None
        self.state = CircuitState.CLOSED
        self.stats = {
            "total_requests": 0,
            "successful_requests": 0,
            "failed_requests": 0,
            "circuit_opened": 0
        }
    
    def call(self, func, *args, **kwargs):
        self.stats["total_requests"] += 1
        
        if self.state == CircuitState.OPEN:
            if time.time() - self.last_failure_time > self.recovery_timeout:
                self.state = CircuitState.HALF_OPEN
            else:
                raise CircuitBreakerOpenException(f"Circuit breaker {self.name} is open")
        
        try:
            result = func(*args, **kwargs)
            self._on_success()
            return result
        except Exception as e:
            self._on_failure()
            raise e
    
    def _on_success(self):
        self.failure_count = 0
        self.state = CircuitState.CLOSED
        self.stats["successful_requests"] += 1
    
    def _on_failure(self):
        self.failure_count += 1
        self.last_failure_time = time.time()
        self.stats["failed_requests"] += 1
        
        if self.failure_count >= self.failure_threshold:
            self.state = CircuitState.OPEN
            self.stats["circuit_opened"] += 1

class CircuitBreakerOpenException(Exception):
    pass

class ExternalServiceBulkheads:
    def __init__(self):
        self.payment_service = CircuitBreakerBulkhead("payment", failure_threshold=3, recovery_timeout=30)
        self.email_service = CircuitBreakerBulkhead("email", failure_threshold=5, recovery_timeout=60)
        self.analytics_service = CircuitBreakerBulkhead("analytics", failure_threshold=2, recovery_timeout=120)
    
    def process_payment(self, payment_data):
        return self.payment_service.call(self._call_payment_api, payment_data)
    
    def send_email(self, email_data):
        return self.email_service.call(self._call_email_api, email_data)
    
    def track_event(self, event_data):
        return self.analytics_service.call(self._call_analytics_api, event_data)
    
    def _call_payment_api(self, payment_data):
        # Simulate external API call
        time.sleep(0.1)
        if payment_data.get("amount", 0) > 10000:  # Simulate failure for large amounts
            raise Exception("Payment service unavailable")
        return {"status": "success", "transaction_id": "txn_123"}
    
    def _call_email_api(self, email_data):
        # Simulate external API call
        time.sleep(0.05)
        return {"status": "sent", "message_id": "msg_456"}
    
    def _call_analytics_api(self, event_data):
        # Simulate external API call
        time.sleep(0.02)
        return {"status": "tracked", "event_id": "evt_789"}
```

## Advantages

### 1. Failure Isolation
- Prevents cascade failures
- Contains issues to specific components
- Maintains overall system stability

### 2. Resource Protection
- Prevents resource exhaustion
- Protects critical operations
- Enables resource prioritization

### 3. Independent Scaling
- Scale different components independently
- Optimize resource allocation
- Better performance tuning

### 4. Improved Monitoring
- Isolated metrics per bulkhead
- Better observability
- Easier troubleshooting

## Best Practices

### 1. Identify Boundaries
```python
# Group by business function
user_bulkhead = ThreadPoolBulkhead("user-operations", ...)
order_bulkhead = ThreadPoolBulkhead("order-operations", ...)

# Group by performance characteristics
fast_bulkhead = ThreadPoolBulkhead("fast-operations", max_workers=20, ...)
slow_bulkhead = ThreadPoolBulkhead("slow-operations", max_workers=2, ...)
```

### 2. Size Appropriately
```python
# Consider load patterns and requirements
critical_bulkhead = ThreadPoolBulkhead("critical", max_workers=10, queue_size=100)
background_bulkhead = ThreadPoolBulkhead("background", max_workers=2, queue_size=1000)
```

### 3. Monitor Health
```python
class BulkheadMonitor:
    def check_health(self, bulkhead: ThreadPoolBulkhead):
        stats = bulkhead.get_stats()
        
        # Check utilization
        utilization = stats["active_threads"] / stats["max_workers"]
        if utilization > 0.8:
            self.alert("High utilization", bulkhead.config.name)
        
        # Check failure rate
        total_requests = stats["completed"] + stats["failed"]
        if total_requests > 0:
            failure_rate = stats["failed"] / total_requests
            if failure_rate > 0.1:
                self.alert("High failure rate", bulkhead.config.name)
```

## When to Use Bulkhead Pattern

### Good Fit
- Multiple distinct operations
- Different SLA requirements
- Resource contention issues
- External service dependencies

### Poor Fit
- Simple applications
- Highly interdependent operations
- Resource-constrained environments
- Over-engineering concerns

## Related Patterns

- **Circuit Breaker**: Often used together for resilience
- **Timeout Pattern**: Complements resource isolation
- **Retry Pattern**: Works with bulkheads for reliability
- **Rate Limiting**: Can be applied per bulkhead

## Conclusion

The Bulkhead Pattern is essential for building resilient systems that can handle partial failures gracefully. By isolating resources and operations, you can prevent the "noisy neighbor" problem and ensure that critical functionality remains available even when parts of the system experience issues.
