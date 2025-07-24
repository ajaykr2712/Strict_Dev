#!/usr/bin/env python3
"""
System Monitoring and Observability Framework
Comprehensive monitoring solution for distributed systems.
"""

import time
import psutil
import threading
import requests
import json
from typing import Dict, List, Any, Callable, Optional
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from collections import defaultdict, deque
import statistics
import logging
from contextlib import contextmanager


@dataclass
class Metric:
    name: str
    value: float
    timestamp: datetime
    tags: Dict[str, str] = field(default_factory=dict)
    metric_type: str = "gauge"  # gauge, counter, histogram


@dataclass
class Alert:
    id: str
    name: str
    message: str
    severity: str  # critical, warning, info
    timestamp: datetime
    tags: Dict[str, str] = field(default_factory=dict)
    resolved: bool = False


@dataclass
class HealthCheck:
    name: str
    status: str  # healthy, unhealthy, degraded
    timestamp: datetime
    response_time: float
    error_message: Optional[str] = None


class MetricsCollector:
    """Collects and stores system metrics"""
    
    def __init__(self):
        self.metrics: List[Metric] = []
        self.counters: Dict[str, float] = defaultdict(float)
        self.gauges: Dict[str, float] = {}
        self.histograms: Dict[str, List[float]] = defaultdict(list)
        self.lock = threading.Lock()
    
    def counter(self, name: str, value: float = 1, tags: Dict[str, str] = None):
        """Increment a counter metric"""
        with self.lock:
            key = self._make_key(name, tags or {})
            self.counters[key] += value
            self._add_metric(name, self.counters[key], "counter", tags)
    
    def gauge(self, name: str, value: float, tags: Dict[str, str] = None):
        """Set a gauge metric value"""
        with self.lock:
            key = self._make_key(name, tags or {})
            self.gauges[key] = value
            self._add_metric(name, value, "gauge", tags)
    
    def histogram(self, name: str, value: float, tags: Dict[str, str] = None):
        """Record a value in a histogram"""
        with self.lock:
            key = self._make_key(name, tags or {})
            self.histograms[key].append(value)
            # Keep only last 1000 values for memory efficiency
            if len(self.histograms[key]) > 1000:
                self.histograms[key] = self.histograms[key][-1000:]
            self._add_metric(name, value, "histogram", tags)
    
    def timing(self, name: str, duration: float, tags: Dict[str, str] = None):
        """Record a timing metric"""
        self.histogram(name, duration, tags)
    
    def _make_key(self, name: str, tags: Dict[str, str]) -> str:
        tag_str = ",".join(f"{k}={v}" for k, v in sorted(tags.items()))
        return f"{name}:{tag_str}" if tag_str else name
    
    def _add_metric(self, name: str, value: float, metric_type: str, tags: Dict[str, str]):
        metric = Metric(
            name=name,
            value=value,
            timestamp=datetime.now(),
            tags=tags or {},
            metric_type=metric_type
        )
        self.metrics.append(metric)
        
        # Keep only last 10000 metrics in memory
        if len(self.metrics) > 10000:
            self.metrics = self.metrics[-10000:]
    
    def get_counter_value(self, name: str, tags: Dict[str, str] = None) -> float:
        key = self._make_key(name, tags or {})
        return self.counters.get(key, 0)
    
    def get_gauge_value(self, name: str, tags: Dict[str, str] = None) -> float:
        key = self._make_key(name, tags or {})
        return self.gauges.get(key, 0)
    
    def get_histogram_stats(self, name: str, tags: Dict[str, str] = None) -> Dict[str, float]:
        key = self._make_key(name, tags or {})
        values = self.histograms.get(key, [])
        
        if not values:
            return {}
        
        return {
            "count": len(values),
            "sum": sum(values),
            "avg": statistics.mean(values),
            "min": min(values),
            "max": max(values),
            "p50": statistics.median(values),
            "p95": self._percentile(values, 0.95),
            "p99": self._percentile(values, 0.99)
        }
    
    def _percentile(self, values: List[float], percentile: float) -> float:
        sorted_values = sorted(values)
        index = int(len(sorted_values) * percentile)
        return sorted_values[min(index, len(sorted_values) - 1)]
    
    def get_recent_metrics(self, since: datetime) -> List[Metric]:
        return [m for m in self.metrics if m.timestamp >= since]


class SystemMonitor:
    """Monitors system resources"""
    
    def __init__(self, metrics_collector: MetricsCollector):
        self.metrics = metrics_collector
        self.monitoring = False
        self.monitor_thread = None
    
    def start_monitoring(self, interval: float = 10.0):
        """Start system monitoring"""
        self.monitoring = True
        self.monitor_thread = threading.Thread(
            target=self._monitor_loop,
            args=(interval,),
            daemon=True
        )
        self.monitor_thread.start()
    
    def stop_monitoring(self):
        """Stop system monitoring"""
        self.monitoring = False
        if self.monitor_thread:
            self.monitor_thread.join()
    
    def _monitor_loop(self, interval: float):
        """Main monitoring loop"""
        while self.monitoring:
            try:
                self._collect_system_metrics()
                time.sleep(interval)
            except Exception as e:
                logging.error(f"Error collecting system metrics: {e}")
    
    def _collect_system_metrics(self):
        """Collect various system metrics"""
        # CPU metrics
        cpu_percent = psutil.cpu_percent(interval=1)
        self.metrics.gauge("system.cpu.usage_percent", cpu_percent)
        
        # Memory metrics
        memory = psutil.virtual_memory()
        self.metrics.gauge("system.memory.usage_percent", memory.percent)
        self.metrics.gauge("system.memory.available_bytes", memory.available)
        self.metrics.gauge("system.memory.used_bytes", memory.used)
        
        # Disk metrics
        disk_usage = psutil.disk_usage('/')
        self.metrics.gauge("system.disk.usage_percent", 
                          (disk_usage.used / disk_usage.total) * 100)
        self.metrics.gauge("system.disk.free_bytes", disk_usage.free)
        
        # Network metrics
        network = psutil.net_io_counters()
        self.metrics.counter("system.network.bytes_sent", network.bytes_sent)
        self.metrics.counter("system.network.bytes_recv", network.bytes_recv)
        
        # Process metrics
        process = psutil.Process()
        self.metrics.gauge("process.cpu.usage_percent", process.cpu_percent())
        self.metrics.gauge("process.memory.rss_bytes", process.memory_info().rss)
        self.metrics.gauge("process.memory.vms_bytes", process.memory_info().vms)
        self.metrics.gauge("process.threads.count", process.num_threads())


class ApplicationMonitor:
    """Monitors application-specific metrics"""
    
    def __init__(self, metrics_collector: MetricsCollector):
        self.metrics = metrics_collector
        self.request_times: Dict[str, deque] = defaultdict(lambda: deque(maxlen=1000))
        self.error_counts: Dict[str, int] = defaultdict(int)
    
    @contextmanager
    def monitor_request(self, endpoint: str, method: str = "GET"):
        """Context manager to monitor request duration and success"""
        start_time = time.time()
        tags = {"endpoint": endpoint, "method": method}
        
        try:
            self.metrics.counter("requests.started", tags=tags)
            yield
            
            # Success
            duration = time.time() - start_time
            self.metrics.timing("requests.duration", duration * 1000, tags=tags)  # ms
            self.metrics.counter("requests.success", tags=tags)
            
        except Exception as e:
            # Error
            duration = time.time() - start_time
            error_tags = {**tags, "error": type(e).__name__}
            self.metrics.timing("requests.duration", duration * 1000, tags=error_tags)
            self.metrics.counter("requests.error", tags=error_tags)
            raise
    
    def record_business_metric(self, name: str, value: float, tags: Dict[str, str] = None):
        """Record custom business metrics"""
        self.metrics.gauge(f"business.{name}", value, tags)
    
    def record_database_query(self, query_type: str, duration: float, success: bool = True):
        """Record database query metrics"""
        tags = {"query_type": query_type, "success": str(success)}
        self.metrics.timing("database.query.duration", duration * 1000, tags)
        self.metrics.counter("database.query.count", tags=tags)
    
    def record_cache_operation(self, operation: str, hit: bool):
        """Record cache operation metrics"""
        tags = {"operation": operation, "result": "hit" if hit else "miss"}
        self.metrics.counter("cache.operations", tags=tags)


class HealthCheckManager:
    """Manages health checks for various system components"""
    
    def __init__(self, metrics_collector: MetricsCollector):
        self.metrics = metrics_collector
        self.health_checks: Dict[str, Callable[[], HealthCheck]] = {}
        self.check_results: Dict[str, HealthCheck] = {}
        self.checking = False
        self.check_thread = None
    
    def register_health_check(self, name: str, check_func: Callable[[], bool],
                            timeout: float = 5.0):
        """Register a health check function"""
        def wrapped_check():
            start_time = time.time()
            try:
                healthy = check_func()
                duration = time.time() - start_time
                
                return HealthCheck(
                    name=name,
                    status="healthy" if healthy else "unhealthy",
                    timestamp=datetime.now(),
                    response_time=duration
                )
            except Exception as e:
                duration = time.time() - start_time
                return HealthCheck(
                    name=name,
                    status="unhealthy",
                    timestamp=datetime.now(),
                    response_time=duration,
                    error_message=str(e)
                )
        
        self.health_checks[name] = wrapped_check
    
    def start_health_checks(self, interval: float = 30.0):
        """Start periodic health checks"""
        self.checking = True
        self.check_thread = threading.Thread(
            target=self._check_loop,
            args=(interval,),
            daemon=True
        )
        self.check_thread.start()
    
    def stop_health_checks(self):
        """Stop health checks"""
        self.checking = False
        if self.check_thread:
            self.check_thread.join()
    
    def _check_loop(self, interval: float):
        """Main health check loop"""
        while self.checking:
            try:
                self._run_all_checks()
                time.sleep(interval)
            except Exception as e:
                logging.error(f"Error running health checks: {e}")
    
    def _run_all_checks(self):
        """Run all registered health checks"""
        for name, check_func in self.health_checks.items():
            try:
                result = check_func()
                self.check_results[name] = result
                
                # Record metrics
                health_value = 1 if result.status == "healthy" else 0
                self.metrics.gauge("health_check.status", health_value, 
                                 {"check_name": name})
                self.metrics.timing("health_check.response_time", 
                                  result.response_time * 1000,
                                  {"check_name": name})
                
            except Exception as e:
                logging.error(f"Health check {name} failed: {e}")
    
    def get_health_status(self) -> Dict[str, Any]:
        """Get overall health status"""
        if not self.check_results:
            return {"status": "unknown", "checks": {}}
        
        overall_healthy = all(
            check.status == "healthy" 
            for check in self.check_results.values()
        )
        
        return {
            "status": "healthy" if overall_healthy else "unhealthy",
            "timestamp": datetime.now().isoformat(),
            "checks": {
                name: {
                    "status": check.status,
                    "response_time": check.response_time,
                    "error": check.error_message
                }
                for name, check in self.check_results.items()
            }
        }


class AlertManager:
    """Manages alerts based on metric thresholds"""
    
    def __init__(self, metrics_collector: MetricsCollector):
        self.metrics = metrics_collector
        self.alerts: List[Alert] = []
        self.alert_rules: List[Dict] = []
        self.notification_handlers: List[Callable[[Alert], None]] = []
        self.checking = False
        self.check_thread = None
    
    def add_alert_rule(self, name: str, metric_name: str, threshold: float,
                      operator: str = "greater_than", severity: str = "warning"):
        """Add an alert rule"""
        rule = {
            "name": name,
            "metric_name": metric_name,
            "threshold": threshold,
            "operator": operator,
            "severity": severity
        }
        self.alert_rules.append(rule)
    
    def add_notification_handler(self, handler: Callable[[Alert], None]):
        """Add a notification handler for alerts"""
        self.notification_handlers.append(handler)
    
    def start_alert_checking(self, interval: float = 60.0):
        """Start periodic alert checking"""
        self.checking = True
        self.check_thread = threading.Thread(
            target=self._alert_loop,
            args=(interval,),
            daemon=True
        )
        self.check_thread.start()
    
    def stop_alert_checking(self):
        """Stop alert checking"""
        self.checking = False
        if self.check_thread:
            self.check_thread.join()
    
    def _alert_loop(self, interval: float):
        """Main alert checking loop"""
        while self.checking:
            try:
                self._check_alert_rules()
                time.sleep(interval)
            except Exception as e:
                logging.error(f"Error checking alerts: {e}")
    
    def _check_alert_rules(self):
        """Check all alert rules"""
        for rule in self.alert_rules:
            try:
                self._evaluate_rule(rule)
            except Exception as e:
                logging.error(f"Error evaluating rule {rule['name']}: {e}")
    
    def _evaluate_rule(self, rule: Dict):
        """Evaluate a single alert rule"""
        metric_name = rule["metric_name"]
        
        # Get current metric value
        current_value = self.metrics.get_gauge_value(metric_name)
        
        # Check if threshold is breached
        threshold_breached = self._check_threshold(
            current_value, rule["threshold"], rule["operator"]
        )
        
        if threshold_breached:
            # Create alert
            alert = Alert(
                id=f"{rule['name']}_{int(time.time())}",
                name=rule["name"],
                message=f"{metric_name} is {current_value}, threshold: {rule['threshold']}",
                severity=rule["severity"],
                timestamp=datetime.now()
            )
            
            self.alerts.append(alert)
            
            # Notify handlers
            for handler in self.notification_handlers:
                try:
                    handler(alert)
                except Exception as e:
                    logging.error(f"Notification handler failed: {e}")
    
    def _check_threshold(self, value: float, threshold: float, operator: str) -> bool:
        """Check if value breaches threshold"""
        if operator == "greater_than":
            return value > threshold
        elif operator == "less_than":
            return value < threshold
        elif operator == "equals":
            return value == threshold
        elif operator == "not_equals":
            return value != threshold
        else:
            return False


def email_notification_handler(alert: Alert):
    """Example email notification handler"""
    print(f"EMAIL ALERT: {alert.name} - {alert.message} ({alert.severity})")


def slack_notification_handler(webhook_url: str):
    """Create a Slack notification handler"""
    def handler(alert: Alert):
        color = {
            "critical": "#ff0000",
            "warning": "#ffaa00",
            "info": "#00ff00"
        }.get(alert.severity, "#cccccc")
        
        payload = {
            "attachments": [{
                "color": color,
                "title": f"Alert: {alert.name}",
                "text": alert.message,
                "fields": [
                    {"title": "Severity", "value": alert.severity, "short": True},
                    {"title": "Time", "value": alert.timestamp.isoformat(), "short": True}
                ]
            }]
        }
        
        try:
            requests.post(webhook_url, json=payload, timeout=10)
        except Exception as e:
            logging.error(f"Failed to send Slack notification: {e}")
    
    return handler


class MonitoringDashboard:
    """Simple web-based monitoring dashboard"""
    
    def __init__(self, metrics_collector: MetricsCollector, 
                 health_manager: HealthCheckManager,
                 alert_manager: AlertManager):
        self.metrics = metrics_collector
        self.health = health_manager
        self.alerts = alert_manager
    
    def generate_dashboard_data(self) -> Dict[str, Any]:
        """Generate data for dashboard"""
        now = datetime.now()
        one_hour_ago = now - timedelta(hours=1)
        
        return {
            "timestamp": now.isoformat(),
            "health": self.health.get_health_status(),
            "alerts": {
                "active": len([a for a in self.alerts.alerts if not a.resolved]),
                "recent": [
                    {
                        "name": a.name,
                        "message": a.message,
                        "severity": a.severity,
                        "timestamp": a.timestamp.isoformat()
                    }
                    for a in self.alerts.alerts[-10:]  # Last 10 alerts
                ]
            },
            "metrics": {
                "system": {
                    "cpu_usage": self.metrics.get_gauge_value("system.cpu.usage_percent"),
                    "memory_usage": self.metrics.get_gauge_value("system.memory.usage_percent"),
                    "disk_usage": self.metrics.get_gauge_value("system.disk.usage_percent")
                },
                "application": {
                    "request_rate": len(self.metrics.get_recent_metrics(one_hour_ago)),
                    "error_rate": self.metrics.get_counter_value("requests.error"),
                    "avg_response_time": self.metrics.get_histogram_stats("requests.duration").get("avg", 0)
                }
            }
        }


def demo_monitoring_system():
    """Demonstrate the monitoring system"""
    print("Monitoring System Demo")
    print("=" * 50)
    
    # Initialize components
    metrics = MetricsCollector()
    system_monitor = SystemMonitor(metrics)
    app_monitor = ApplicationMonitor(metrics)
    health_manager = HealthCheckManager(metrics)
    alert_manager = AlertManager(metrics)
    dashboard = MonitoringDashboard(metrics, health_manager, alert_manager)
    
    # Start monitoring
    system_monitor.start_monitoring(interval=5)
    
    # Register health checks
    def database_health_check():
        # Simulate database check
        time.sleep(0.1)
        return True
    
    def redis_health_check():
        # Simulate Redis check
        time.sleep(0.05)
        return True
    
    health_manager.register_health_check("database", database_health_check)
    health_manager.register_health_check("redis", redis_health_check)
    health_manager.start_health_checks(interval=10)
    
    # Setup alerts
    alert_manager.add_alert_rule(
        "High CPU Usage",
        "system.cpu.usage_percent",
        threshold=80.0,
        severity="warning"
    )
    
    alert_manager.add_alert_rule(
        "High Memory Usage",
        "system.memory.usage_percent",
        threshold=90.0,
        severity="critical"
    )
    
    alert_manager.add_notification_handler(email_notification_handler)
    alert_manager.start_alert_checking(interval=30)
    
    print("Monitoring started. Collecting metrics...")
    
    # Simulate application activity
    for i in range(10):
        # Simulate API requests
        with app_monitor.monitor_request("/api/users", "GET"):
            time.sleep(0.1)  # Simulate request processing
        
        # Simulate database operations
        app_monitor.record_database_query("SELECT", 0.05)
        app_monitor.record_cache_operation("get", hit=True)
        
        # Record business metrics
        app_monitor.record_business_metric("active_users", 1000 + i * 10)
        
        time.sleep(1)
    
    # Display results
    print("\nSystem Metrics:")
    print(f"CPU Usage: {metrics.get_gauge_value('system.cpu.usage_percent'):.1f}%")
    print(f"Memory Usage: {metrics.get_gauge_value('system.memory.usage_percent'):.1f}%")
    
    print("\nApplication Metrics:")
    request_stats = metrics.get_histogram_stats("requests.duration")
    if request_stats:
        print(f"Request Count: {request_stats['count']}")
        print(f"Avg Response Time: {request_stats['avg']:.2f}ms")
        print(f"P95 Response Time: {request_stats['p95']:.2f}ms")
    
    print("\nHealth Status:")
    health_status = health_manager.get_health_status()
    print(f"Overall Status: {health_status['status']}")
    for check_name, check_data in health_status['checks'].items():
        print(f"  {check_name}: {check_data['status']} ({check_data['response_time']:.3f}s)")
    
    print("\nDashboard Data:")
    dashboard_data = dashboard.generate_dashboard_data()
    print(json.dumps(dashboard_data, indent=2, default=str))
    
    # Cleanup
    system_monitor.stop_monitoring()
    health_manager.stop_health_checks()
    alert_manager.stop_alert_checking()
    
    print("\nMonitoring demo completed!")


if __name__ == "__main__":
    demo_monitoring_system()
