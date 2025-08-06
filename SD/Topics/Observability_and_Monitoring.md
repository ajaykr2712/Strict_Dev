# Observability and Monitoring

## Overview
Observability is the ability to understand the internal state of a system by examining its external outputs. It goes beyond traditional monitoring by providing deep insights into system behavior, enabling faster debugging, better performance optimization, and proactive issue resolution.

## The Three Pillars of Observability

### 1. Metrics
Numerical measurements collected over time intervals.

```python
# Example: Custom metrics collection
import time
from collections import defaultdict

class MetricsCollector:
    def __init__(self):
        self.counters = defaultdict(int)
        self.gauges = defaultdict(float)
        self.histograms = defaultdict(list)
        self.start_time = time.time()
    
    def increment_counter(self, name, value=1, tags=None):
        key = f"{name}:{tags}" if tags else name
        self.counters[key] += value
    
    def set_gauge(self, name, value, tags=None):
        key = f"{name}:{tags}" if tags else name
        self.gauges[key] = value
    
    def record_histogram(self, name, value, tags=None):
        key = f"{name}:{tags}" if tags else name
        self.histograms[key].append(value)
    
    def get_rate(self, counter_name, window_seconds=60):
        current_time = time.time()
        elapsed = current_time - self.start_time
        return self.counters[counter_name] / min(elapsed, window_seconds)
```

#### Key Metrics Types
- **Counters**: Monotonically increasing values (requests served, errors occurred)
- **Gauges**: Point-in-time values (CPU usage, memory consumption)
- **Histograms**: Distribution of values (response times, request sizes)
- **Summaries**: Similar to histograms but with quantiles

### 2. Logs
Discrete events with timestamps and contextual information.

```python
# Example: Structured logging
import json
import logging
from datetime import datetime

class StructuredLogger:
    def __init__(self, service_name):
        self.service_name = service_name
        self.logger = logging.getLogger(service_name)
    
    def log_event(self, level, message, **context):
        log_entry = {
            'timestamp': datetime.utcnow().isoformat(),
            'service': self.service_name,
            'level': level,
            'message': message,
            'context': context
        }
        
        if level == 'ERROR':
            self.logger.error(json.dumps(log_entry))
        elif level == 'WARN':
            self.logger.warning(json.dumps(log_entry))
        else:
            self.logger.info(json.dumps(log_entry))
    
    def log_request(self, method, path, status_code, duration_ms, user_id=None):
        self.log_event(
            'INFO',
            'HTTP Request',
            method=method,
            path=path,
            status_code=status_code,
            duration_ms=duration_ms,
            user_id=user_id
        )
```

### 3. Traces
Request flows through distributed systems showing service interactions.

```python
# Example: Distributed tracing
import uuid
import time

class TracingContext:
    def __init__(self, trace_id=None, span_id=None, parent_span_id=None):
        self.trace_id = trace_id or str(uuid.uuid4())
        self.span_id = span_id or str(uuid.uuid4())
        self.parent_span_id = parent_span_id
        self.start_time = time.time()
        self.spans = []
    
    def create_child_span(self, operation_name):
        return TracingContext(
            trace_id=self.trace_id,
            parent_span_id=self.span_id
        )
    
    def finish_span(self, operation_name, tags=None):
        span = {
            'trace_id': self.trace_id,
            'span_id': self.span_id,
            'parent_span_id': self.parent_span_id,
            'operation_name': operation_name,
            'start_time': self.start_time,
            'duration': time.time() - self.start_time,
            'tags': tags or {}
        }
        self.spans.append(span)
        return span
```

## Advanced Monitoring Concepts

### 1. Service Level Objectives (SLOs)
Reliability targets for your service defined in terms of Service Level Indicators (SLIs).

```python
# Example: SLO monitoring
class SLOMonitor:
    def __init__(self, target_availability=99.9):
        self.target_availability = target_availability
        self.total_requests = 0
        self.successful_requests = 0
        self.error_budget_consumed = 0
    
    def record_request(self, success):
        self.total_requests += 1
        if success:
            self.successful_requests += 1
        else:
            self.error_budget_consumed += 1
    
    def current_availability(self):
        if self.total_requests == 0:
            return 100.0
        return (self.successful_requests / self.total_requests) * 100
    
    def error_budget_remaining(self):
        allowed_errors = self.total_requests * (100 - self.target_availability) / 100
        return max(0, allowed_errors - self.error_budget_consumed)
    
    def is_burning_error_budget(self):
        return self.current_availability() < self.target_availability
```

### 2. Alerting Strategy
- **Error Rate Alerts**: When error rates exceed thresholds
- **Latency Alerts**: When response times degrade
- **Saturation Alerts**: When resource utilization is high
- **Availability Alerts**: When services become unreachable

### 3. The Four Golden Signals
1. **Latency**: Time to process requests
2. **Traffic**: Demand on your system
3. **Errors**: Rate of failed requests
4. **Saturation**: Resource utilization

## Observability Tools and Platforms

### Open Source Solutions
| Tool | Category | Strengths |
|------|----------|-----------|
| Prometheus | Metrics | Time-series database, powerful querying |
| Grafana | Visualization | Rich dashboards, multiple data sources |
| Jaeger | Tracing | Distributed tracing, root cause analysis |
| ELK Stack | Logs | Search, analysis, visualization |
| OpenTelemetry | Instrumentation | Vendor-neutral, standardized APIs |

### Commercial Platforms
- **DataDog**: Full-stack monitoring and analytics
- **New Relic**: Application performance monitoring
- **Splunk**: Log analysis and SIEM
- **Dynatrace**: AI-powered full-stack monitoring

## Implementation Best Practices

### 1. Instrumentation Strategy
```python
# Example: Application instrumentation
from contextlib import contextmanager
import time

class ApplicationInstrumentation:
    def __init__(self, metrics_collector, logger, tracer):
        self.metrics = metrics_collector
        self.logger = logger
        self.tracer = tracer
    
    @contextmanager
    def trace_operation(self, operation_name, tags=None):
        start_time = time.time()
        span = self.tracer.create_child_span(operation_name)
        
        try:
            yield span
            self.metrics.increment_counter(f"{operation_name}.success")
        except Exception as e:
            self.metrics.increment_counter(f"{operation_name}.error")
            self.logger.log_event(
                'ERROR',
                f"Operation {operation_name} failed",
                error=str(e),
                trace_id=span.trace_id
            )
            raise
        finally:
            duration = time.time() - start_time
            self.metrics.record_histogram(f"{operation_name}.duration", duration)
            span.finish_span(operation_name, tags)

# Usage example
instrumentation = ApplicationInstrumentation(metrics, logger, tracer)

def process_order(order_data):
    with instrumentation.trace_operation("process_order", {"order_id": order_data["id"]}):
        # Process the order
        validate_order(order_data)
        charge_payment(order_data["payment"])
        fulfill_order(order_data)
```

### 2. Correlation IDs
Track requests across service boundaries.

```python
# Example: Correlation ID implementation
import threading
from contextvars import ContextVar

correlation_id_var = ContextVar('correlation_id', default=None)

class CorrelationIdMiddleware:
    def __init__(self):
        self.local = threading.local()
    
    def set_correlation_id(self, correlation_id):
        correlation_id_var.set(correlation_id)
    
    def get_correlation_id(self):
        return correlation_id_var.get()
    
    def process_request(self, request):
        correlation_id = request.headers.get('X-Correlation-ID') or str(uuid.uuid4())
        self.set_correlation_id(correlation_id)
        return correlation_id
```

### 3. Sampling Strategies
Control the volume of observability data.

```python
# Example: Adaptive sampling
import random

class AdaptiveSampler:
    def __init__(self, base_rate=0.1, max_rate=1.0):
        self.base_rate = base_rate
        self.max_rate = max_rate
        self.current_rate = base_rate
        self.error_count = 0
        self.total_count = 0
    
    def should_sample(self, is_error=False):
        self.total_count += 1
        if is_error:
            self.error_count += 1
            # Increase sampling rate for errors
            self.current_rate = min(self.max_rate, self.current_rate * 1.1)
            return True
        
        # Adaptive sampling based on error rate
        error_rate = self.error_count / self.total_count if self.total_count > 0 else 0
        if error_rate > 0.01:  # If error rate > 1%
            self.current_rate = min(self.max_rate, self.base_rate * 10)
        else:
            self.current_rate = max(self.base_rate, self.current_rate * 0.99)
        
        return random.random() < self.current_rate
```

## Microservices Observability

### 1. Service Mesh Integration
- **Istio**: Automatic telemetry collection
- **Linkerd**: Lightweight service mesh with observability
- **Consul Connect**: Service discovery with monitoring

### 2. Distributed System Challenges
- **Clock Skew**: Time synchronization across services
- **Network Partitions**: Handling connectivity issues
- **Cascade Failures**: Preventing failure propagation

### 3. Cross-Service Tracing
```python
# Example: Service-to-service tracing
import requests

class TracedHTTPClient:
    def __init__(self, tracer):
        self.tracer = tracer
    
    def make_request(self, method, url, **kwargs):
        with self.tracer.trace_operation(f"http.{method.lower()}", 
                                       {"url": url, "method": method}) as span:
            # Inject trace context into headers
            headers = kwargs.get('headers', {})
            headers.update({
                'X-Trace-ID': span.trace_id,
                'X-Span-ID': span.span_id
            })
            kwargs['headers'] = headers
            
            response = requests.request(method, url, **kwargs)
            
            # Record response metrics
            span.tags.update({
                'status_code': response.status_code,
                'response_size': len(response.content)
            })
            
            return response
```

## Performance and Cost Optimization

### 1. Data Retention Policies
```python
# Example: Metric retention configuration
retention_policies = {
    'high_frequency_metrics': {
        'raw_data': '7 days',
        'downsampled_1m': '30 days',
        'downsampled_1h': '1 year'
    },
    'application_logs': {
        'debug_logs': '24 hours',
        'info_logs': '30 days',
        'error_logs': '1 year'
    },
    'traces': {
        'sampled_traces': '7 days',
        'error_traces': '30 days'
    }
}
```

### 2. Cardinality Management
Prevent metric explosion from high-cardinality labels.

```python
# Example: Cardinality limiting
class CardinalityLimiter:
    def __init__(self, max_cardinality=10000):
        self.max_cardinality = max_cardinality
        self.label_sets = set()
    
    def should_emit_metric(self, metric_name, labels):
        label_key = frozenset(labels.items()) if labels else frozenset()
        full_key = (metric_name, label_key)
        
        if full_key in self.label_sets:
            return True
        
        if len(self.label_sets) < self.max_cardinality:
            self.label_sets.add(full_key)
            return True
        
        # Drop metric to prevent cardinality explosion
        return False
```

## Real-World Scenarios

### 1. Incident Response
```python
# Example: Automated incident detection
class IncidentDetector:
    def __init__(self, alerting_system):
        self.alerting_system = alerting_system
        self.thresholds = {
            'error_rate': 0.05,  # 5%
            'response_time_p99': 2.0,  # 2 seconds
            'availability': 99.9  # 99.9%
        }
    
    def check_health(self, metrics):
        incidents = []
        
        for metric, threshold in self.thresholds.items():
            current_value = metrics.get(metric)
            if current_value and self.is_threshold_breached(metric, current_value, threshold):
                incident = {
                    'metric': metric,
                    'current_value': current_value,
                    'threshold': threshold,
                    'severity': self.calculate_severity(metric, current_value, threshold)
                }
                incidents.append(incident)
                self.alerting_system.trigger_alert(incident)
        
        return incidents
```

### 2. Capacity Planning
Use observability data to predict resource needs.

### 3. Performance Debugging
Leverage traces and metrics to identify bottlenecks.

## Interview Questions

### Conceptual Questions
1. **What's the difference between monitoring and observability?**
2. **Explain the three pillars of observability and their relationships.**
3. **How would you design an alerting strategy to minimize false positives?**

### System Design Questions
1. **Design a monitoring system for a microservices architecture**
2. **How would you implement distributed tracing across 50+ services?**
3. **Design an anomaly detection system for application metrics**

### Practical Questions
1. **How do you handle high-cardinality metrics?**
2. **What sampling strategies would you use for traces in a high-traffic system?**
3. **How would you correlate logs, metrics, and traces during an incident?**

## Conclusion

Observability is crucial for operating reliable, performant systems at scale. The key is to implement comprehensive instrumentation while managing costs and complexity. Focus on actionable insights rather than collecting data for its own sake, and ensure your observability strategy evolves with your system's growth and complexity.
