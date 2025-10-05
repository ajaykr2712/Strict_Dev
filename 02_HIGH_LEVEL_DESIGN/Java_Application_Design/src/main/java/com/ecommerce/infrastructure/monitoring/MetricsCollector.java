package com.ecommerce.infrastructure.monitoring;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Metrics Collector - Infrastructure Component
 * 
 * Collects and aggregates application metrics for monitoring and observability.
 * Implements patterns from "Designing Data-Intensive Applications" for telemetry.
 * 
 * Features:
 * - Counter metrics for events and errors
 * - Latency tracking for performance monitoring
 * - Thread-safe concurrent collection
 * - In-memory aggregation (in production would use Prometheus/Grafana)
 */
public class MetricsCollector {
    private final Map<String, AtomicLong> counters;
    private final Map<String, LatencyTracker> latencies;
    private volatile boolean isRunning;
    
    public MetricsCollector() {
        this.counters = new ConcurrentHashMap<>();
        this.latencies = new ConcurrentHashMap<>();
        this.isRunning = false;
    }
    
    /**
     * Start the metrics collection system
     */
    public void start() {
        this.isRunning = true;
        System.out.println("MetricsCollector started at " + LocalDateTime.now());
        
        // In production, this would start background threads for:
        // - Periodic metric export to monitoring systems
        // - Health checks
        // - Alerting based on thresholds
    }
    
    /**
     * Stop the metrics collection system
     */
    public void stop() {
        this.isRunning = false;
        System.out.println("MetricsCollector stopped at " + LocalDateTime.now());
        printSummary();
    }
    
    /**
     * Record a counter metric (increment by value)
     */
    public void recordMetric(String metricName, long value) {
        if (!isRunning) {
            return;
        }
        
        counters.computeIfAbsent(metricName, k -> new AtomicLong(0))
                .addAndGet(value);
        
        // In production, this would:
        // - Send to time-series database (InfluxDB, Prometheus)
        // - Apply sampling for high-volume metrics
        // - Add labels/tags for dimensional analysis
    }
    
    /**
     * Record latency metric in milliseconds
     */
    public void recordLatency(String metricName, long latencyMs) {
        if (!isRunning) {
            return;
        }
        
        latencies.computeIfAbsent(metricName, k -> new LatencyTracker())
                .record(latencyMs);
    }
    
    /**
     * Get current value of a counter metric
     */
    public long getCounterValue(String metricName) {
        AtomicLong counter = counters.get(metricName);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * Get latency statistics for a metric
     */
    public LatencyStats getLatencyStats(String metricName) {
        LatencyTracker tracker = latencies.get(metricName);
        return tracker != null ? tracker.getStats() : new LatencyStats(0, 0, 0, 0);
    }
    
    /**
     * Print metrics summary (for demonstration)
     */
    public void printSummary() {
        System.out.println("\n=== Metrics Summary ===");
        
        System.out.println("Counter Metrics:");
        counters.forEach((name, value) -> 
            System.out.println("  " + name + ": " + value.get()));
        
        System.out.println("Latency Metrics:");
        latencies.forEach((name, tracker) -> {
            LatencyStats stats = tracker.getStats();
            System.out.println("  " + name + ": avg=" + stats.average + "ms, " +
                             "min=" + stats.min + "ms, max=" + stats.max + "ms, " +
                             "count=" + stats.count);
        });
    }
    
    /**
     * Inner class for tracking latency statistics
     */
    private static class LatencyTracker {
        private volatile long sum = 0;
        private volatile long count = 0;
        private volatile long min = Long.MAX_VALUE;
        private volatile long max = Long.MIN_VALUE;
        
        public synchronized void record(long latency) {
            sum += latency;
            count++;
            min = Math.min(min, latency);
            max = Math.max(max, latency);
        }
        
        public synchronized LatencyStats getStats() {
            if (count == 0) {
                return new LatencyStats(0, 0, 0, 0);
            }
            return new LatencyStats(sum / count, min, max, count);
        }
    }
    
    /**
     * Immutable latency statistics
     */
    public static class LatencyStats {
        public final long average;
        public final long min;
        public final long max;
        public final long count;
        
        public LatencyStats(long average, long min, long max, long count) {
            this.average = average;
            this.min = min;
            this.max = max;
            this.count = count;
        }
        
        @Override
        public String toString() {
            return "LatencyStats{" +
                    "average=" + average + "ms" +
                    ", min=" + min + "ms" +
                    ", max=" + max + "ms" +
                    ", count=" + count +
                    '}';
        }
    }
}
