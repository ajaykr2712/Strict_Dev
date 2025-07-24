# Circuit Breaker Pattern

## Overview
The Circuit Breaker pattern is a design pattern used to detect failures and encapsulates the logic of preventing a failure from constantly recurring during maintenance, temporary external system failure, or unexpected system difficulties. It acts like an electrical circuit breaker that trips when there's an overload, preventing damage to the system.

## Problem Statement
In distributed systems, remote calls can fail due to:
- Network timeouts
- Service unavailability
- Resource exhaustion
- Temporary glitches

Without proper handling, these failures can:
- Cascade through the system
- Exhaust connection pools
- Cause thread blocking
- Lead to system-wide outages

## Solution: Circuit Breaker Pattern

The Circuit Breaker pattern prevents cascading failures by:
1. **Monitoring** remote service calls
2. **Detecting** failure patterns
3. **Tripping** when failure threshold is reached
4. **Failing fast** during the open state
5. **Testing** service recovery periodically

## Circuit Breaker States

### 1. Closed State (Normal Operation)
- All requests pass through to the remote service
- Monitor failure rate and response times
- Count successful and failed calls
- Trip to Open state when failure threshold exceeded

### 2. Open State (Failure Mode)
- All requests immediately return error (fail fast)
- No calls made to the failing service
- Prevents resource exhaustion
- Periodically attempt to transition to Half-Open

### 3. Half-Open State (Recovery Testing)
- Limited number of test requests allowed through
- Monitor these test requests closely
- If successful: transition to Closed state
- If failures continue: return to Open state

## Implementation Components

### Core Components
- **Failure Counter**: Track consecutive failures
- **Success Counter**: Track consecutive successes
- **Timeout Timer**: Control state transitions
- **Failure Threshold**: Trigger point for opening circuit
- **Recovery Timeout**: Time before testing recovery
- **Success Threshold**: Required successes to close circuit

### State Machine
```
    [CLOSED] ──failure_threshold──> [OPEN]
        ↑                             ↓
        │                        timeout_period
        │                             ↓
    success_threshold <──── [HALF-OPEN]
                              ↓
                          failure
                              ↓
                           [OPEN]
```

## Advanced Features

### 1. Adaptive Thresholds
- **Dynamic thresholds** based on historical data
- **Time-based windows** for failure rate calculation
- **Statistical analysis** of service performance
- **Machine learning** for predictive failure detection

### 2. Multiple Failure Criteria
- **Response time** thresholds
- **Error rate** percentages
- **Specific exception types**
- **HTTP status codes** (5xx errors)
- **Resource exhaustion** indicators

### 3. Fallback Mechanisms
- **Default values** for non-critical operations
- **Cached responses** for read operations
- **Alternative services** or endpoints
- **Degraded functionality** modes
- **Queue-based delayed processing**

### 4. Monitoring and Observability
- **Circuit state metrics** (open/closed/half-open)
- **Failure rate tracking** over time
- **Response time percentiles**
- **Circuit trip events** and reasons
- **Recovery time measurements**

## Implementation Example

### Basic Circuit Breaker
```python
import time
import threading
from enum import Enum
from typing import Callable, Any, Optional

class CircuitState(Enum):
    CLOSED = "closed"
    OPEN = "open"
    HALF_OPEN = "half_open"

class CircuitBreaker:
    def __init__(self, 
                 failure_threshold: int = 5,
                 recovery_timeout: int = 60,
                 expected_exception: type = Exception):
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.expected_exception = expected_exception
        
        self.failure_count = 0
        self.last_failure_time = None
        self.state = CircuitState.CLOSED
        self.lock = threading.Lock()
    
    def call(self, func: Callable, *args, **kwargs) -> Any:
        with self.lock:
            if self.state == CircuitState.OPEN:
                if self._should_attempt_reset():
                    self.state = CircuitState.HALF_OPEN
                else:
                    raise Exception("Circuit breaker is OPEN")
            
            try:
                result = func(*args, **kwargs)
                self._on_success()
                return result
            
            except self.expected_exception as e:
                self._on_failure()
                raise e
    
    def _should_attempt_reset(self) -> bool:
        return (time.time() - self.last_failure_time) >= self.recovery_timeout
    
    def _on_success(self):
        self.failure_count = 0
        self.state = CircuitState.CLOSED
    
    def _on_failure(self):
        self.failure_count += 1
        self.last_failure_time = time.time()
        
        if self.failure_count >= self.failure_threshold:
            self.state = CircuitState.OPEN
```

### Advanced Circuit Breaker with Metrics
```python
import statistics
from collections import deque
from dataclasses import dataclass
from typing import Dict, List

@dataclass
class CircuitMetrics:
    total_requests: int = 0
    successful_requests: int = 0
    failed_requests: int = 0
    total_failures: int = 0
    circuit_open_count: int = 0
    response_times: deque = None
    
    def __post_init__(self):
        if self.response_times is None:
            self.response_times = deque(maxlen=100)

class AdvancedCircuitBreaker:
    def __init__(self, 
                 failure_threshold: int = 5,
                 success_threshold: int = 3,
                 timeout: int = 60,
                 response_timeout: float = 30.0):
        self.failure_threshold = failure_threshold
        self.success_threshold = success_threshold
        self.timeout = timeout
        self.response_timeout = response_timeout
        
        self.state = CircuitState.CLOSED
        self.failure_count = 0
        self.success_count = 0
        self.last_failure_time = None
        self.metrics = CircuitMetrics()
        self.lock = threading.Lock()
    
    def execute(self, operation: Callable, *args, **kwargs) -> Any:
        with self.lock:
            self.metrics.total_requests += 1
            
            if self.state == CircuitState.OPEN:
                if time.time() - self.last_failure_time < self.timeout:
                    raise Exception("Circuit breaker is OPEN - failing fast")
                else:
                    self.state = CircuitState.HALF_OPEN
                    self.success_count = 0
            
            start_time = time.time()
            
            try:
                result = operation(*args, **kwargs)
                execution_time = time.time() - start_time
                
                self.metrics.response_times.append(execution_time)
                self._handle_success()
                
                return result
                
            except Exception as e:
                self._handle_failure()
                raise e
    
    def _handle_success(self):
        self.metrics.successful_requests += 1
        self.failure_count = 0
        
        if self.state == CircuitState.HALF_OPEN:
            self.success_count += 1
            if self.success_count >= self.success_threshold:
                self.state = CircuitState.CLOSED
                self.success_count = 0
    
    def _handle_failure(self):
        self.metrics.failed_requests += 1
        self.metrics.total_failures += 1
        self.failure_count += 1
        self.last_failure_time = time.time()
        
        if self.failure_count >= self.failure_threshold:
            self.state = CircuitState.OPEN
            self.metrics.circuit_open_count += 1
    
    def get_metrics(self) -> Dict[str, Any]:
        response_times = list(self.metrics.response_times)
        
        return {
            'state': self.state.value,
            'total_requests': self.metrics.total_requests,
            'success_rate': (self.metrics.successful_requests / 
                           max(1, self.metrics.total_requests)),
            'failure_count': self.failure_count,
            'circuit_opens': self.metrics.circuit_open_count,
            'avg_response_time': (statistics.mean(response_times) 
                                if response_times else 0),
            'response_time_p95': (statistics.quantiles(response_times, n=20)[18] 
                                if len(response_times) > 10 else 0)
        }
```

## Benefits

### 1. System Resilience
- **Prevents cascading failures** in distributed systems
- **Protects resources** from being exhausted
- **Enables graceful degradation** of functionality
- **Improves overall system stability**

### 2. Performance
- **Fails fast** instead of waiting for timeouts
- **Reduces response times** during failures
- **Prevents thread pool exhaustion**
- **Maintains system responsiveness**

### 3. Operational Benefits
- **Early failure detection** and alerting
- **Automatic recovery** testing
- **Detailed metrics** for monitoring
- **Improved debugging** capabilities

## Common Use Cases

### 1. Microservices Communication
- **Service-to-service** API calls
- **Database connections** and queries
- **External service** integrations
- **Message queue** operations

### 2. External Dependencies
- **Third-party APIs** (payment, authentication)
- **Cloud services** (AWS, Azure, GCP)
- **Legacy systems** integration
- **Partner service** communications

### 3. Resource Protection
- **Database connection pools**
- **Memory-intensive operations**
- **CPU-bound computations**
- **File system operations**

## Best Practices

### 1. Configuration
- **Tune thresholds** based on service characteristics
- **Monitor and adjust** timeout values
- **Use different settings** for different services
- **Consider business impact** when setting parameters

### 2. Monitoring
- **Track circuit state changes** and duration
- **Monitor failure rates** and patterns
- **Set up alerts** for circuit trips
- **Analyze recovery times** and success rates

### 3. Testing
- **Test circuit behavior** under load
- **Verify fallback mechanisms** work correctly
- **Simulate failure scenarios** regularly
- **Validate recovery procedures**

### 4. Integration
- **Combine with retry mechanisms** (with backoff)
- **Implement proper logging** and metrics
- **Use with bulkhead pattern** for isolation
- **Consider timeout hierarchies**

## Interview Questions

1. **What problems does the Circuit Breaker pattern solve?**
   - Cascading failures, resource exhaustion, system instability

2. **Explain the three states of a circuit breaker.**
   - Closed (normal), Open (failure mode), Half-Open (testing recovery)

3. **How do you determine the failure threshold?**
   - Based on service characteristics, SLA requirements, and business impact

4. **What's the difference between circuit breaker and retry patterns?**
   - Circuit breaker prevents calls, retry attempts again with backoff

5. **How do you handle partial failures in microservices?**
   - Combine circuit breaker with fallback mechanisms and graceful degradation

## Real-world Examples

### Netflix Hystrix
- Popularized the circuit breaker pattern
- Provides real-time monitoring dashboard
- Integrates with Spring Boot applications
- Includes thread pool isolation

### AWS Application Load Balancer
- Built-in circuit breaker functionality
- Health checks and automatic failover
- Integration with Auto Scaling groups

### Istio Service Mesh
- Circuit breaker policies at service mesh level
- Automatic retry and timeout configuration
- Observability and metrics collection

---
Continue to the next pattern for deeper system design mastery!
