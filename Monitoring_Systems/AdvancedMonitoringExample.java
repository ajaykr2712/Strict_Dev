package com.systemdesign.monitoring;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Instant;
import java.time.Duration;

/**
 * Advanced Monitoring and Observability System
 * 
 * Implements comprehensive monitoring patterns used by Netflix, Uber, and WhatsApp
 * for distributed systems observability including metrics, logging, tracing,
 * alerting, and health checks.
 * 
 * Features:
 * - Multi-dimensional metrics collection
 * - Distributed tracing
 * - Health check aggregation
 * - Alerting with escalation
 * - SLA monitoring
 * - Custom dashboards
 */

// Metric types and data structures
enum MetricType {
    COUNTER,    // Monotonically increasing values
    GAUGE,      // Point-in-time values
    HISTOGRAM,  // Distribution of values
    TIMER,      // Duration measurements
    SUMMARY     // Quantile summaries
}

class Metric {
    private final String name;
    private final MetricType type;
    private final Map<String, String> tags;
    private final AtomicLong value;
    private final List<Double> histogram;
    private final long timestamp;
    
    public Metric(String name, MetricType type, Map<String, String> tags, double value) {
        this.name = name;
        this.type = type;
        this.tags = new HashMap<>(tags != null ? tags : Collections.emptyMap());
        this.value = new AtomicLong(Double.doubleToLongBits(value));
        this.histogram = type == MetricType.HISTOGRAM ? new CopyOnWriteArrayList<>() : null;
        this.timestamp = System.currentTimeMillis();
    }
    
    public void increment() {
        if (type == MetricType.COUNTER) {
            value.incrementAndGet();
        }
    }
    
    public void increment(double amount) {
        if (type == MetricType.COUNTER) {
            long current, updated;
            do {
                current = value.get();
                updated = Double.doubleToLongBits(Double.longBitsToDouble(current) + amount);
            } while (!value.compareAndSet(current, updated));
        }
    }
    
    public void set(double newValue) {
        if (type == MetricType.GAUGE) {
            value.set(Double.doubleToLongBits(newValue));
        }
    }
    
    public void recordValue(double val) {
        if (type == MetricType.HISTOGRAM && histogram != null) {
            histogram.add(val);
        }
    }
    
    public double getValue() {
        return Double.longBitsToDouble(value.get());
    }
    
    // Getters
    public String getName() { return name; }
    public MetricType getType() { return type; }
    public Map<String, String> getTags() { return new HashMap<>(tags); }
    public long getTimestamp() { return timestamp; }
    public List<Double> getHistogram() { 
        return histogram != null ? new ArrayList<>(histogram) : Collections.emptyList(); 
    }
    
    @Override
    public String toString() {
        return String.format("Metric{name='%s', type=%s, value=%.2f, tags=%s}",
                           name, type, getValue(), tags);
    }
}

// Distributed trace span
class TraceSpan {
    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String operationName;
    private final String serviceName;
    private final long startTime;
    private volatile long endTime;
    private final Map<String, String> tags;
    private final List<LogEntry> logs;
    private volatile SpanStatus status;
    
    public TraceSpan(String traceId, String spanId, String parentSpanId, 
                    String operationName, String serviceName) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.operationName = operationName;
        this.serviceName = serviceName;
        this.startTime = System.currentTimeMillis();
        this.endTime = -1;
        this.tags = new ConcurrentHashMap<>();
        this.logs = new CopyOnWriteArrayList<>();
        this.status = SpanStatus.ACTIVE;
    }
    
    public void addTag(String key, String value) {
        tags.put(key, value);
    }
    
    public void log(String message) {
        logs.add(new LogEntry(System.currentTimeMillis(), message));
    }
    
    public void finish() {
        this.endTime = System.currentTimeMillis();
        this.status = SpanStatus.FINISHED;
    }
    
    public void setError(String errorMessage) {
        this.status = SpanStatus.ERROR;
        addTag("error", "true");
        log("ERROR: " + errorMessage);
    }
    
    public long getDuration() {
        return endTime > 0 ? endTime - startTime : -1;
    }
    
    // Getters
    public String getTraceId() { return traceId; }
    public String getSpanId() { return spanId; }
    public String getParentSpanId() { return parentSpanId; }
    public String getOperationName() { return operationName; }
    public String getServiceName() { return serviceName; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public Map<String, String> getTags() { return new HashMap<>(tags); }
    public List<LogEntry> getLogs() { return new ArrayList<>(logs); }
    public SpanStatus getStatus() { return status; }
}

enum SpanStatus {
    ACTIVE, FINISHED, ERROR
}

class LogEntry {
    private final long timestamp;
    private final String message;
    
    public LogEntry(long timestamp, String message) {
        this.timestamp = timestamp;
        this.message = message;
    }
    
    public long getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
}

// Health check system
interface HealthCheck {
    String getName();
    HealthCheckResult check();
}

class HealthCheckResult {
    private final boolean healthy;
    private final String message;
    private final Map<String, Object> details;
    private final long timestamp;
    
    public HealthCheckResult(boolean healthy, String message, Map<String, Object> details) {
        this.healthy = healthy;
        this.message = message;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public boolean isHealthy() { return healthy; }
    public String getMessage() { return message; }
    public Map<String, Object> getDetails() { return new HashMap<>(details); }
    public long getTimestamp() { return timestamp; }
}

// Database health check example
class DatabaseHealthCheck implements HealthCheck {
    private final String databaseName;
    private final AtomicLong connectionPool;
    private final AtomicLong activeConnections;
    
    public DatabaseHealthCheck(String databaseName) {
        this.databaseName = databaseName;
        this.connectionPool = new AtomicLong(20); // Max 20 connections
        this.activeConnections = new AtomicLong(0);
    }
    
    @Override
    public String getName() {
        return "database_" + databaseName;
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            // Simulate connection check
            long active = activeConnections.get();
            long total = connectionPool.get();
            
            boolean healthy = active < total * 0.9; // Consider unhealthy if >90% utilized
            
            Map<String, Object> details = Map.of(
                "activeConnections", active,
                "totalConnections", total,
                "utilizationPercentage", (double) active / total * 100
            );
            
            String message = healthy ? 
                "Database connection pool healthy" : 
                "Database connection pool nearly exhausted";
            
            return new HealthCheckResult(healthy, message, details);
            
        } catch (Exception e) {
            return new HealthCheckResult(false, "Database health check failed: " + e.getMessage(), null);
        }
    }
    
    // Simulate connection usage
    public void simulateConnection() {
        activeConnections.incrementAndGet();
        CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS)
            .execute(() -> activeConnections.decrementAndGet());
    }
}

// External service health check
class ExternalServiceHealthCheck implements HealthCheck {
    private final String serviceName;
    private final String endpoint;
    private volatile long lastSuccessfulCheck;
    private final AtomicLong consecutiveFailures;
    
    public ExternalServiceHealthCheck(String serviceName, String endpoint) {
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.lastSuccessfulCheck = System.currentTimeMillis();
        this.consecutiveFailures = new AtomicLong(0);
    }
    
    @Override
    public String getName() {
        return "external_service_" + serviceName;
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            // Simulate HTTP health check
            boolean isHealthy = performHealthCheck();
            
            if (isHealthy) {
                lastSuccessfulCheck = System.currentTimeMillis();
                consecutiveFailures.set(0);
                
                Map<String, Object> details = Map.of(
                    "endpoint", endpoint,
                    "lastSuccessfulCheck", lastSuccessfulCheck
                );
                
                return new HealthCheckResult(true, "Service is healthy", details);
            } else {
                long failures = consecutiveFailures.incrementAndGet();
                long timeSinceSuccess = System.currentTimeMillis() - lastSuccessfulCheck;
                
                Map<String, Object> details = Map.of(
                    "endpoint", endpoint,
                    "consecutiveFailures", failures,
                    "timeSinceLastSuccess", timeSinceSuccess
                );
                
                return new HealthCheckResult(false, 
                    "Service health check failed " + failures + " times", details);
            }
            
        } catch (Exception e) {
            consecutiveFailures.incrementAndGet();
            return new HealthCheckResult(false, "Health check exception: " + e.getMessage(), null);
        }
    }
    
    private boolean performHealthCheck() {
        // Simulate health check with 85% success rate
        return Math.random() > 0.15;
    }
}

// Alert system
enum AlertSeverity {
    INFO, WARNING, CRITICAL
}

class Alert {
    private final String id;
    private final String name;
    private final AlertSeverity severity;
    private final String message;
    private final Map<String, String> labels;
    private final long timestamp;
    private volatile boolean acknowledged;
    private volatile boolean resolved;
    
    public Alert(String name, AlertSeverity severity, String message, Map<String, String> labels) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.severity = severity;
        this.message = message;
        this.labels = new HashMap<>(labels != null ? labels : Collections.emptyMap());
        this.timestamp = System.currentTimeMillis();
        this.acknowledged = false;
        this.resolved = false;
    }
    
    public void acknowledge() {
        this.acknowledged = true;
    }
    
    public void resolve() {
        this.resolved = true;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public AlertSeverity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public Map<String, String> getLabels() { return new HashMap<>(labels); }
    public long getTimestamp() { return timestamp; }
    public boolean isAcknowledged() { return acknowledged; }
    public boolean isResolved() { return resolved; }
    
    @Override
    public String toString() {
        return String.format("Alert{name='%s', severity=%s, message='%s', resolved=%s}",
                           name, severity, message, resolved);
    }
}

// Alert rule
class AlertRule {
    private final String name;
    private final String metricName;
    private final String condition; // e.g., "> 0.8", "< 100"
    private final AlertSeverity severity;
    private final Duration evaluationInterval;
    private final Duration forDuration; // How long condition must be true
    private volatile long conditionStartTime;
    
    public AlertRule(String name, String metricName, String condition, 
                    AlertSeverity severity, Duration evaluationInterval, Duration forDuration) {
        this.name = name;
        this.metricName = metricName;
        this.condition = condition;
        this.severity = severity;
        this.evaluationInterval = evaluationInterval;
        this.forDuration = forDuration;
        this.conditionStartTime = -1;
    }
    
    public Optional<Alert> evaluate(Metric metric) {
        if (!metric.getName().equals(metricName)) {
            return Optional.empty();
        }
        
        boolean conditionMet = evaluateCondition(metric.getValue());
        long currentTime = System.currentTimeMillis();
        
        if (conditionMet) {
            if (conditionStartTime == -1) {
                conditionStartTime = currentTime;
            } else if (currentTime - conditionStartTime >= forDuration.toMillis()) {
                // Condition has been true for required duration
                String message = String.format("Metric %s %s (current value: %.2f)", 
                                              metricName, condition, metric.getValue());
                
                Map<String, String> labels = new HashMap<>(metric.getTags());
                labels.put("metric", metricName);
                labels.put("condition", condition);
                
                conditionStartTime = -1; // Reset to avoid duplicate alerts
                return Optional.of(new Alert(name, severity, message, labels));
            }
        } else {
            conditionStartTime = -1; // Reset condition timer
        }
        
        return Optional.empty();
    }
    
    private boolean evaluateCondition(double value) {
        // Simple condition evaluation (in real implementation, use expression parser)
        if (condition.startsWith(">")) {
            double threshold = Double.parseDouble(condition.substring(1).trim());
            return value > threshold;
        } else if (condition.startsWith("<")) {
            double threshold = Double.parseDouble(condition.substring(1).trim());
            return value < threshold;
        } else if (condition.startsWith("==")) {
            double threshold = Double.parseDouble(condition.substring(2).trim());
            return Math.abs(value - threshold) < 0.001;
        }
        
        return false;
    }
    
    // Getters
    public String getName() { return name; }
    public String getMetricName() { return metricName; }
    public Duration getEvaluationInterval() { return evaluationInterval; }
}

// Main monitoring system
class AdvancedMonitoringSystem {
    private final Map<String, Metric> metrics;
    private final Map<String, TraceSpan> activeSpans;
    private final Map<String, HealthCheck> healthChecks;
    private final List<AlertRule> alertRules;
    private final List<Alert> activeAlerts;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService alertExecutor;
    
    // Trace context for current thread
    private final ThreadLocal<String> currentTraceId = new ThreadLocal<>();
    private final ThreadLocal<TraceSpan> currentSpan = new ThreadLocal<>();
    
    public AdvancedMonitoringSystem() {
        this.metrics = new ConcurrentHashMap<>();
        this.activeSpans = new ConcurrentHashMap<>();
        this.healthChecks = new ConcurrentHashMap<>();
        this.alertRules = new CopyOnWriteArrayList<>();
        this.activeAlerts = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.alertExecutor = Executors.newCachedThreadPool();
        
        startMonitoringTasks();
    }
    
    // Metrics methods
    public void recordCounter(String name, Map<String, String> tags) {
        recordCounter(name, tags, 1.0);
    }
    
    public void recordCounter(String name, Map<String, String> tags, double increment) {
        String key = buildMetricKey(name, tags);
        metrics.computeIfAbsent(key, k -> new Metric(name, MetricType.COUNTER, tags, 0))
               .increment(increment);
    }
    
    public void recordGauge(String name, Map<String, String> tags, double value) {
        String key = buildMetricKey(name, tags);
        metrics.computeIfAbsent(key, k -> new Metric(name, MetricType.GAUGE, tags, value))
               .set(value);
    }
    
    public void recordTimer(String name, Map<String, String> tags, long durationMs) {
        String key = buildMetricKey(name, tags);
        metrics.computeIfAbsent(key, k -> new Metric(name, MetricType.TIMER, tags, 0))
               .recordValue(durationMs);
    }
    
    // Tracing methods
    public TraceSpan startSpan(String operationName, String serviceName) {
        String traceId = currentTraceId.get();
        if (traceId == null) {
            traceId = generateTraceId();
            currentTraceId.set(traceId);
        }
        
        String spanId = generateSpanId();
        String parentSpanId = currentSpan.get() != null ? currentSpan.get().getSpanId() : null;
        
        TraceSpan span = new TraceSpan(traceId, spanId, parentSpanId, operationName, serviceName);
        activeSpans.put(spanId, span);
        currentSpan.set(span);
        
        return span;
    }
    
    public void finishSpan(TraceSpan span) {
        span.finish();
        currentSpan.remove();
        
        // Record span duration as metric
        recordTimer("span.duration", 
                   Map.of("operation", span.getOperationName(), "service", span.getServiceName()),
                   span.getDuration());
        
        // Clean up after some time
        scheduler.schedule(() -> activeSpans.remove(span.getSpanId()), 
                         5, TimeUnit.MINUTES);
    }
    
    // Health check methods
    public void registerHealthCheck(HealthCheck healthCheck) {
        healthChecks.put(healthCheck.getName(), healthCheck);
    }
    
    public Map<String, HealthCheckResult> runHealthChecks() {
        Map<String, HealthCheckResult> results = new HashMap<>();
        
        healthChecks.forEach((name, healthCheck) -> {
            try {
                HealthCheckResult result = healthCheck.check();
                results.put(name, result);
                
                // Record health check result as metric
                recordGauge("health.check", 
                           Map.of("check_name", name),
                           result.isHealthy() ? 1.0 : 0.0);
                
            } catch (Exception e) {
                results.put(name, new HealthCheckResult(false, 
                    "Health check threw exception: " + e.getMessage(), null));
            }
        });
        
        return results;
    }
    
    // Alert methods
    public void addAlertRule(AlertRule rule) {
        alertRules.add(rule);
    }
    
    private void evaluateAlerts() {
        for (AlertRule rule : alertRules) {
            metrics.values().stream()
                .filter(metric -> metric.getName().equals(rule.getMetricName()))
                .forEach(metric -> {
                    Optional<Alert> alert = rule.evaluate(metric);
                    if (alert.isPresent()) {
                        Alert newAlert = alert.get();
                        activeAlerts.add(newAlert);
                        
                        // Send alert notification
                        alertExecutor.submit(() -> sendAlertNotification(newAlert));
                    }
                });
        }
    }
    
    private void sendAlertNotification(Alert alert) {
        System.out.println("🚨 ALERT: " + alert);
        
        // In real implementation: send to Slack, PagerDuty, email, etc.
        if (alert.getSeverity() == AlertSeverity.CRITICAL) {
            System.out.println("📞 Paging on-call engineer for critical alert: " + alert.getName());
        }
    }
    
    // Monitoring tasks
    private void startMonitoringTasks() {
        // Health check monitoring every 30 seconds
        scheduler.scheduleAtFixedRate(this::runHealthChecks, 0, 30, TimeUnit.SECONDS);
        
        // Alert evaluation every 15 seconds
        scheduler.scheduleAtFixedRate(this::evaluateAlerts, 0, 15, TimeUnit.SECONDS);
        
        // Metrics cleanup every 5 minutes
        scheduler.scheduleAtFixedRate(this::cleanupOldMetrics, 5, 5, TimeUnit.MINUTES);
        
        // System metrics collection every 10 seconds
        scheduler.scheduleAtFixedRate(this::collectSystemMetrics, 0, 10, TimeUnit.SECONDS);
    }
    
    private void collectSystemMetrics() {
        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        recordGauge("jvm.memory.used", Collections.emptyMap(), usedMemory);
        recordGauge("jvm.memory.total", Collections.emptyMap(), totalMemory);
        recordGauge("jvm.memory.utilization", Collections.emptyMap(), 
                   (double) usedMemory / totalMemory);
        
        // System metrics
        recordGauge("system.active_threads", Collections.emptyMap(), 
                   Thread.activeCount());
        recordGauge("system.cpu_cores", Collections.emptyMap(), 
                   Runtime.getRuntime().availableProcessors());
    }
    
    private void cleanupOldMetrics() {
        long cutoffTime = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis();
        
        metrics.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp() < cutoffTime);
        
        // Clean up resolved alerts older than 1 hour
        long alertCutoff = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        activeAlerts.removeIf(alert -> 
            alert.isResolved() && alert.getTimestamp() < alertCutoff);
    }
    
    // Utility methods
    private String buildMetricKey(String name, Map<String, String> tags) {
        StringBuilder key = new StringBuilder(name);
        if (tags != null && !tags.isEmpty()) {
            key.append("{");
            tags.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> key.append(entry.getKey()).append("=").append(entry.getValue()).append(","));
            key.setLength(key.length() - 1); // Remove trailing comma
            key.append("}");
        }
        return key.toString();
    }
    
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    // Statistics and reporting
    public void printSystemOverview() {
        System.out.println("\n=== Monitoring System Overview ===");
        System.out.println("Total metrics: " + metrics.size());
        System.out.println("Active spans: " + activeSpans.size());
        System.out.println("Health checks: " + healthChecks.size());
        System.out.println("Alert rules: " + alertRules.size());
        System.out.println("Active alerts: " + activeAlerts.stream().filter(a -> !a.isResolved()).count());
        
        // System health summary
        Map<String, HealthCheckResult> healthResults = runHealthChecks();
        long healthyChecks = healthResults.values().stream()
            .mapToLong(result -> result.isHealthy() ? 1 : 0).sum();
        
        System.out.println(String.format("System health: %d/%d checks passing", 
                                        healthyChecks, healthResults.size()));
        
        // Recent alerts
        System.out.println("\nRecent alerts:");
        activeAlerts.stream()
            .filter(alert -> !alert.isResolved())
            .limit(5)
            .forEach(alert -> System.out.println("  " + alert));
    }
    
    public void shutdown() {
        scheduler.shutdown();
        alertExecutor.shutdown();
    }
    
    // Getters for testing/inspection
    public Map<String, Metric> getMetrics() { return new HashMap<>(metrics); }
    public List<Alert> getActiveAlerts() { return new ArrayList<>(activeAlerts); }
    public Map<String, HealthCheck> getHealthChecks() { return new HashMap<>(healthChecks); }
}

public class AdvancedMonitoringExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Advanced Monitoring & Observability Demo ===\n");
        
        AdvancedMonitoringSystem monitoring = new AdvancedMonitoringSystem();
        
        // Setup health checks
        setupHealthChecks(monitoring);
        
        // Setup alert rules
        setupAlertRules(monitoring);
        
        // Simulate application activity
        simulateApplicationActivity(monitoring);
        
        // Let monitoring run for a while
        Thread.sleep(10000);
        
        // Print system overview
        monitoring.printSystemOverview();
        
        monitoring.shutdown();
        System.out.println("\nDemo completed!");
    }
    
    private static void setupHealthChecks(AdvancedMonitoringSystem monitoring) {
        System.out.println("Setting up health checks...");
        
        monitoring.registerHealthCheck(new DatabaseHealthCheck("user_db"));
        monitoring.registerHealthCheck(new DatabaseHealthCheck("content_db"));
        monitoring.registerHealthCheck(new ExternalServiceHealthCheck("payment_service", "http://payment.api/health"));
        monitoring.registerHealthCheck(new ExternalServiceHealthCheck("notification_service", "http://notification.api/health"));
    }
    
    private static void setupAlertRules(AdvancedMonitoringSystem monitoring) {
        System.out.println("Setting up alert rules...");
        
        // High memory utilization alert
        monitoring.addAlertRule(new AlertRule(
            "High Memory Usage",
            "jvm.memory.utilization",
            "> 0.8",
            AlertSeverity.WARNING,
            Duration.ofSeconds(15),
            Duration.ofMinutes(1)
        ));
        
        // Service health alert
        monitoring.addAlertRule(new AlertRule(
            "Service Health Check Failed",
            "health.check",
            "< 1",
            AlertSeverity.CRITICAL,
            Duration.ofSeconds(15),
            Duration.ofSeconds(30)
        ));
        
        // High error rate alert
        monitoring.addAlertRule(new AlertRule(
            "High Error Rate",
            "http.requests.errors",
            "> 10",
            AlertSeverity.CRITICAL,
            Duration.ofSeconds(15),
            Duration.ofMinutes(2)
        ));
    }
    
    private static void simulateApplicationActivity(AdvancedMonitoringSystem monitoring) {
        System.out.println("Simulating application activity...\n");
        
        Random random = new Random();
        
        // Simulate HTTP requests
        for (int i = 0; i < 50; i++) {
            CompletableFuture.runAsync(() -> {
                TraceSpan span = monitoring.startSpan("http_request", "web_service");
                span.addTag("method", "GET");
                span.addTag("endpoint", "/api/recommendations");
                
                try {
                    // Simulate request processing
                    Thread.sleep(random.nextInt(200) + 50);
                    
                    // Record metrics
                    monitoring.recordCounter("http.requests.total", 
                        Map.of("method", "GET", "status", "200"));
                    
                    if (random.nextDouble() < 0.1) { // 10% error rate
                        span.setError("Simulated error");
                        monitoring.recordCounter("http.requests.errors", 
                            Map.of("method", "GET"));
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    monitoring.finishSpan(span);
                }
            });
            
            try {
                Thread.sleep(200); // Space out requests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Simulate some database activity
        DatabaseHealthCheck dbHealth = new DatabaseHealthCheck("user_db");
        for (int i = 0; i < 15; i++) {
            dbHealth.simulateConnection();
        }
    }
}
