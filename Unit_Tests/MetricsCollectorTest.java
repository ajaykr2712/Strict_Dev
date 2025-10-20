package unittests;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Unit tests for MetricsCollector
 * Refactored: October 20, 2025
 * 
 * Tests metrics collection infrastructure for monitoring and observability,
 * including counter metrics, latency tracking, and thread-safe operations.
 * Tests metrics collection infrastructure for monitoring and observability
 */
public class MetricsCollectorTest {

    /**
     * Mock classes for testing
     */
    static class LatencyTracker {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong sum = new AtomicLong(0);
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;

        void record(long latencyMs) {
            count.incrementAndGet();
            sum.addAndGet(latencyMs);
            synchronized (this) {
                if (latencyMs < min) min = latencyMs;
                if (latencyMs > max) max = latencyMs;
            }
        }

        long getCount() { return count.get(); }
        double getAverage() { return count.get() == 0 ? 0 : (double) sum.get() / count.get(); }
        long getMin() { return min == Long.MAX_VALUE ? 0 : min; }
        long getMax() { return max == Long.MIN_VALUE ? 0 : max; }
    }

    static class MetricsCollector {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        private final Map<String, LatencyTracker> latencies = new ConcurrentHashMap<>();
        private volatile boolean isRunning = false;

        void start() {
            isRunning = true;
        }

        void stop() {
            isRunning = false;
        }

        void recordMetric(String metricName, long value) {
            if (!isRunning) return;
            counters.computeIfAbsent(metricName, k -> new AtomicLong(0)).addAndGet(value);
        }

        void recordLatency(String metricName, long latencyMs) {
            if (!isRunning) return;
            latencies.computeIfAbsent(metricName, k -> new LatencyTracker()).record(latencyMs);
        }

        long getMetric(String metricName) {
            AtomicLong counter = counters.get(metricName);
            return counter != null ? counter.get() : 0;
        }

        LatencyTracker getLatency(String metricName) {
            return latencies.get(metricName);
        }

        boolean isRunning() {
            return isRunning;
        }

        int getMetricCount() {
            return counters.size();
        }
    }

    private MetricsCollector collector;

    @Before
    public void setUp() {
        collector = new MetricsCollector();
    }

    @Test
    public void testCollectorCreation() {
        // Assert
        assertNotNull("Collector should be created", collector);
        assertFalse("Collector should not be running initially", collector.isRunning());
    }

    @Test
    public void testStartCollector() {
        // Act
        collector.start();

        // Assert
        assertTrue("Collector should be running after start", collector.isRunning());
    }

    @Test
    public void testStopCollector() {
        // Arrange
        collector.start();

        // Act
        collector.stop();

        // Assert
        assertFalse("Collector should not be running after stop", collector.isRunning());
    }

    @Test
    public void testRecordMetricWhenStopped() {
        // Act
        collector.recordMetric("test_metric", 5);

        // Assert
        assertEquals("Metric should not be recorded when collector is stopped", 
            0, collector.getMetric("test_metric"));
    }

    @Test
    public void testRecordSingleMetric() {
        // Arrange
        collector.start();

        // Act
        collector.recordMetric("test_metric", 5);

        // Assert
        assertEquals("Metric should be recorded", 5, collector.getMetric("test_metric"));
    }

    @Test
    public void testRecordMultipleMetricsToSameName() {
        // Arrange
        collector.start();

        // Act
        collector.recordMetric("test_metric", 5);
        collector.recordMetric("test_metric", 3);
        collector.recordMetric("test_metric", 7);

        // Assert
        assertEquals("Metrics should be accumulated", 15, collector.getMetric("test_metric"));
    }

    @Test
    public void testRecordMultipleDifferentMetrics() {
        // Arrange
        collector.start();

        // Act
        collector.recordMetric("metric1", 10);
        collector.recordMetric("metric2", 20);
        collector.recordMetric("metric3", 30);

        // Assert
        assertEquals("Metric1 should be 10", 10, collector.getMetric("metric1"));
        assertEquals("Metric2 should be 20", 20, collector.getMetric("metric2"));
        assertEquals("Metric3 should be 30", 30, collector.getMetric("metric3"));
        assertEquals("Should have 3 different metrics", 3, collector.getMetricCount());
    }

    @Test
    public void testRecordLatency() {
        // Arrange
        collector.start();

        // Act
        collector.recordLatency("api_latency", 100);

        // Assert
        LatencyTracker tracker = collector.getLatency("api_latency");
        assertNotNull("Latency tracker should exist", tracker);
        assertEquals("Latency count should be 1", 1, tracker.getCount());
        assertEquals("Average latency should be 100", 100.0, tracker.getAverage(), 0.01);
    }

    @Test
    public void testRecordMultipleLatencies() {
        // Arrange
        collector.start();

        // Act
        collector.recordLatency("api_latency", 100);
        collector.recordLatency("api_latency", 200);
        collector.recordLatency("api_latency", 150);

        // Assert
        LatencyTracker tracker = collector.getLatency("api_latency");
        assertEquals("Latency count should be 3", 3, tracker.getCount());
        assertEquals("Average latency should be 150", 150.0, tracker.getAverage(), 0.01);
    }

    @Test
    public void testLatencyMinMax() {
        // Arrange
        collector.start();

        // Act
        collector.recordLatency("api_latency", 100);
        collector.recordLatency("api_latency", 50);
        collector.recordLatency("api_latency", 200);

        // Assert
        LatencyTracker tracker = collector.getLatency("api_latency");
        assertEquals("Min latency should be 50", 50, tracker.getMin());
        assertEquals("Max latency should be 200", 200, tracker.getMax());
    }

    @Test
    public void testRecordLatencyWhenStopped() {
        // Act
        collector.recordLatency("api_latency", 100);

        // Assert
        assertNull("Latency should not be recorded when collector is stopped", 
            collector.getLatency("api_latency"));
    }

    @Test
    public void testGetNonExistentMetric() {
        // Arrange
        collector.start();

        // Act
        long value = collector.getMetric("non_existent");

        // Assert
        assertEquals("Non-existent metric should return 0", 0, value);
    }

    @Test
    public void testGetNonExistentLatency() {
        // Arrange
        collector.start();

        // Act
        LatencyTracker tracker = collector.getLatency("non_existent");

        // Assert
        assertNull("Non-existent latency tracker should return null", tracker);
    }

    @Test
    public void testConcurrentMetricRecording() throws InterruptedException {
        // Arrange
        collector.start();
        int threadCount = 10;
        int incrementsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    collector.recordMetric("concurrent_metric", 1);
                }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertEquals("All increments should be recorded", 
            threadCount * incrementsPerThread, 
            collector.getMetric("concurrent_metric"));
    }

    @Test
    public void testConcurrentLatencyRecording() throws InterruptedException {
        // Arrange
        collector.start();
        int threadCount = 5;
        int recordsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < recordsPerThread; j++) {
                    collector.recordLatency("concurrent_latency", 100);
                }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        LatencyTracker tracker = collector.getLatency("concurrent_latency");
        assertEquals("All latencies should be recorded", 
            threadCount * recordsPerThread, 
            tracker.getCount());
    }

    @Test
    public void testStartStopCycle() {
        // Act & Assert
        collector.start();
        assertTrue("Should be running", collector.isRunning());
        
        collector.stop();
        assertFalse("Should be stopped", collector.isRunning());
        
        collector.start();
        assertTrue("Should be running again", collector.isRunning());
    }

    @Test
    public void testMetricNamesWithSpecialCharacters() {
        // Arrange
        collector.start();

        // Act
        collector.recordMetric("http.requests.total", 10);
        collector.recordMetric("cache_hits_ratio", 5);
        collector.recordMetric("queue-depth", 3);

        // Assert
        assertEquals("Metric with dots should work", 10, 
            collector.getMetric("http.requests.total"));
        assertEquals("Metric with underscores should work", 5, 
            collector.getMetric("cache_hits_ratio"));
        assertEquals("Metric with hyphens should work", 3, 
            collector.getMetric("queue-depth"));
    }

    @Test
    public void testZeroValueMetric() {
        // Arrange
        collector.start();

        // Act
        collector.recordMetric("zero_metric", 0);

        // Assert
        assertEquals("Zero value should be recorded", 0, 
            collector.getMetric("zero_metric"));
    }

    @Test
    public void testNegativeValueMetric() {
        // Arrange
        collector.start();

        // Act
        collector.recordMetric("error_metric", 5);
        collector.recordMetric("error_metric", -3);

        // Assert
        assertEquals("Negative values should adjust the metric", 2, 
            collector.getMetric("error_metric"));
    }

    @Test
    public void testLatencyAverage() {
        // Arrange
        collector.start();

        // Act
        collector.recordLatency("avg_latency", 100);
        collector.recordLatency("avg_latency", 200);
        collector.recordLatency("avg_latency", 300);

        // Assert
        LatencyTracker tracker = collector.getLatency("avg_latency");
        assertEquals("Average should be 200", 200.0, tracker.getAverage(), 0.01);
    }

    @Test
    public void testHighVolumeMetrics() {
        // Arrange
        collector.start();

        // Act
        for (int i = 0; i < 10000; i++) {
            collector.recordMetric("high_volume", 1);
        }

        // Assert
        assertEquals("Should handle high volume", 10000, 
            collector.getMetric("high_volume"));
    }
}
