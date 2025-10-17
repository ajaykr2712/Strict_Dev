import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for ExemplarTracingExample
 * Tests linking metrics with trace exemplars for root cause analysis
 * in monitoring and observability scenarios.
 */
public class ExemplarTracingTest {

    /**
     * Mock classes for testing
     */
    static class HistogramBucket {
        final double upperBound;
        long count;
        String exemplarTraceId;
        Instant exemplarTime;

        HistogramBucket(double upperBound) {
            this.upperBound = upperBound;
        }
    }

    static class LatencyHistogram {
        final HistogramBucket[] buckets = {
            new HistogramBucket(50),
            new HistogramBucket(100),
            new HistogramBucket(250),
            new HistogramBucket(500),
            new HistogramBucket(1000),
            new HistogramBucket(Double.POSITIVE_INFINITY)
        };

        void observe(long millis, String currentTraceId) {
            for (var b : buckets) {
                if (millis <= b.upperBound) {
                    b.count++;
                    // Exemplar sampling: capture if no exemplar or count is power of two
                    if (b.exemplarTraceId == null || (b.count & (b.count - 1)) == 0) {
                        b.exemplarTraceId = currentTraceId;
                        b.exemplarTime = Instant.now();
                    }
                    break;
                }
            }
        }

        HistogramBucket getBucket(int index) {
            return buckets[index];
        }

        int getBucketCount() {
            return buckets.length;
        }
    }

    private LatencyHistogram histogram;

    @Before
    public void setUp() {
        histogram = new LatencyHistogram();
    }

    @Test
    public void testHistogramCreation() {
        // Assert
        assertNotNull("Histogram should be created", histogram);
        assertEquals("Should have 6 buckets", 6, histogram.getBucketCount());
    }

    @Test
    public void testBucketBoundaries() {
        // Arrange & Act
        HistogramBucket[] buckets = histogram.buckets;

        // Assert
        assertEquals("First bucket upper bound", 50, buckets[0].upperBound, 0.01);
        assertEquals("Second bucket upper bound", 100, buckets[1].upperBound, 0.01);
        assertEquals("Third bucket upper bound", 250, buckets[2].upperBound, 0.01);
        assertEquals("Fourth bucket upper bound", 500, buckets[3].upperBound, 0.01);
        assertEquals("Fifth bucket upper bound", 1000, buckets[4].upperBound, 0.01);
        assertTrue("Last bucket should be infinity", 
            Double.isInfinite(buckets[5].upperBound));
    }

    @Test
    public void testObserveInFirstBucket() {
        // Arrange
        String traceId = "trace-001";

        // Act
        histogram.observe(30, traceId);

        // Assert
        HistogramBucket bucket = histogram.getBucket(0);
        assertEquals("Bucket count should be 1", 1, bucket.count);
        assertEquals("Trace ID should be captured", traceId, bucket.exemplarTraceId);
        assertNotNull("Exemplar time should be set", bucket.exemplarTime);
    }

    @Test
    public void testObserveInMiddleBucket() {
        // Arrange
        String traceId = "trace-002";

        // Act
        histogram.observe(150, traceId);

        // Assert
        HistogramBucket bucket = histogram.getBucket(2);
        assertEquals("Bucket count should be 1", 1, bucket.count);
        assertEquals("Trace ID should be captured", traceId, bucket.exemplarTraceId);
    }

    @Test
    public void testObserveInInfinityBucket() {
        // Arrange
        String traceId = "trace-003";

        // Act
        histogram.observe(5000, traceId);

        // Assert
        HistogramBucket bucket = histogram.getBucket(5);
        assertEquals("Infinity bucket count should be 1", 1, bucket.count);
        assertEquals("Trace ID should be captured", traceId, bucket.exemplarTraceId);
    }

    @Test
    public void testBucketSelection() {
        // Arrange & Act
        histogram.observe(50, "trace-50");   // Goes to bucket 0
        histogram.observe(100, "trace-100"); // Goes to bucket 1
        histogram.observe(250, "trace-250"); // Goes to bucket 2

        // Assert
        assertEquals("Bucket 0 should have count 1", 1, histogram.getBucket(0).count);
        assertEquals("Bucket 1 should have count 1", 1, histogram.getBucket(1).count);
        assertEquals("Bucket 2 should have count 1", 1, histogram.getBucket(2).count);
    }

    @Test
    public void testExemplarSamplingFirstObservation() {
        // Arrange
        String traceId = "trace-first";

        // Act
        histogram.observe(75, traceId);

        // Assert
        HistogramBucket bucket = histogram.getBucket(1);
        assertEquals("First observation should capture exemplar", traceId, bucket.exemplarTraceId);
    }

    @Test
    public void testExemplarSamplingPowerOfTwo() {
        // Arrange
        histogram.observe(75, "trace-1");
        histogram.observe(75, "trace-2");
        histogram.observe(75, "trace-3");
        String powerOfTwoTrace = "trace-4";

        // Act
        histogram.observe(75, powerOfTwoTrace);

        // Assert
        HistogramBucket bucket = histogram.getBucket(1);
        assertEquals("Count should be 4 (power of 2)", 4, bucket.count);
        assertEquals("Exemplar should be updated at power of 2", 
            powerOfTwoTrace, bucket.exemplarTraceId);
    }

    @Test
    public void testExemplarNotUpdatedBetweenPowersOfTwo() {
        // Arrange
        histogram.observe(75, "trace-1");
        histogram.observe(75, "trace-2");
        String previousExemplar = histogram.getBucket(1).exemplarTraceId;

        // Act
        histogram.observe(75, "trace-3");

        // Assert
        HistogramBucket bucket = histogram.getBucket(1);
        assertEquals("Count should be 3", 3, bucket.count);
        assertEquals("Exemplar should not change at count 3", 
            previousExemplar, bucket.exemplarTraceId);
    }

    @Test
    public void testMultipleBucketsIndependent() {
        // Arrange & Act
        histogram.observe(30, "trace-bucket0");
        histogram.observe(150, "trace-bucket2");
        histogram.observe(600, "trace-bucket4");

        // Assert
        assertEquals("Bucket 0 trace ID", "trace-bucket0", 
            histogram.getBucket(0).exemplarTraceId);
        assertEquals("Bucket 2 trace ID", "trace-bucket2", 
            histogram.getBucket(2).exemplarTraceId);
        assertEquals("Bucket 4 trace ID", "trace-bucket4", 
            histogram.getBucket(4).exemplarTraceId);
    }

    @Test
    public void testBucketCountIncrement() {
        // Arrange & Act
        for (int i = 0; i < 10; i++) {
            histogram.observe(30, "trace-" + i);
        }

        // Assert
        assertEquals("Bucket 0 count should be 10", 10, histogram.getBucket(0).count);
        assertEquals("Other buckets should be empty", 0, histogram.getBucket(1).count);
    }

    @Test
    public void testExemplarTimeRecorded() throws InterruptedException {
        // Arrange
        Instant before = Instant.now();
        Thread.sleep(10);

        // Act
        histogram.observe(50, "trace-timed");
        Thread.sleep(10);
        Instant after = Instant.now();

        // Assert
        HistogramBucket bucket = histogram.getBucket(0);
        assertNotNull("Exemplar time should be recorded", bucket.exemplarTime);
        assertTrue("Exemplar time should be after start", 
            bucket.exemplarTime.isAfter(before) || bucket.exemplarTime.equals(before));
        assertTrue("Exemplar time should be before end", 
            bucket.exemplarTime.isBefore(after) || bucket.exemplarTime.equals(after));
    }

    @Test
    public void testBoundaryValueExactMatch() {
        // Arrange & Act
        histogram.observe(100, "trace-boundary");

        // Assert - Value equal to boundary goes to that bucket
        HistogramBucket bucket = histogram.getBucket(1);
        assertEquals("Boundary value should go to bucket 1", 1, bucket.count);
    }

    @Test
    public void testZeroLatency() {
        // Arrange & Act
        histogram.observe(0, "trace-zero");

        // Assert
        HistogramBucket bucket = histogram.getBucket(0);
        assertEquals("Zero latency should go to first bucket", 1, bucket.count);
        assertEquals("Trace ID should be captured", "trace-zero", bucket.exemplarTraceId);
    }

    @Test
    public void testHighLatencyDistribution() {
        // Arrange & Act
        for (int i = 0; i < 50; i++) {
            long latency = (long) (Math.random() * 2000);
            histogram.observe(latency, "trace-" + i);
        }

        // Assert
        long totalCount = 0;
        for (HistogramBucket bucket : histogram.buckets) {
            totalCount += bucket.count;
        }
        assertEquals("Total count should equal observations", 50, totalCount);
    }

    @Test
    public void testExemplarSamplingAt16() {
        // Arrange
        for (int i = 1; i <= 15; i++) {
            histogram.observe(75, "trace-" + i);
        }
        String trace16 = "trace-16";

        // Act
        histogram.observe(75, trace16);

        // Assert
        HistogramBucket bucket = histogram.getBucket(1);
        assertEquals("Count should be 16 (power of 2)", 16, bucket.count);
        assertEquals("Exemplar should be updated at count 16", trace16, bucket.exemplarTraceId);
    }

    @Test
    public void testIsPowerOfTwo() {
        // Helper test for power of two logic
        assertTrue("1 is power of two", isPowerOfTwo(1));
        assertTrue("2 is power of two", isPowerOfTwo(2));
        assertTrue("4 is power of two", isPowerOfTwo(4));
        assertTrue("8 is power of two", isPowerOfTwo(8));
        assertTrue("16 is power of two", isPowerOfTwo(16));
        assertFalse("3 is not power of two", isPowerOfTwo(3));
        assertFalse("5 is not power of two", isPowerOfTwo(5));
        assertFalse("7 is not power of two", isPowerOfTwo(7));
    }

    private boolean isPowerOfTwo(long n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    @Test
    public void testBucketInitialState() {
        // Arrange
        HistogramBucket bucket = new HistogramBucket(100);

        // Assert
        assertEquals("Initial count should be 0", 0, bucket.count);
        assertNull("Initial trace ID should be null", bucket.exemplarTraceId);
        assertNull("Initial time should be null", bucket.exemplarTime);
        assertEquals("Upper bound should match", 100, bucket.upperBound, 0.01);
    }

    @Test
    public void testConcurrentObservations() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        int observationsPerThread = 100;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < observationsPerThread; j++) {
                    histogram.observe(75 + threadId, "trace-" + threadId + "-" + j);
                }
                latch.countDown();
            });
        }

        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        long totalCount = 0;
        for (HistogramBucket bucket : histogram.buckets) {
            totalCount += bucket.count;
        }
        assertEquals("Total observations should match", 
            threadCount * observationsPerThread, totalCount);
    }

    @Test
    public void testTraceIdFormat() {
        // Arrange
        String traceId = UUID.randomUUID().toString();

        // Act
        histogram.observe(50, traceId);

        // Assert
        HistogramBucket bucket = histogram.getBucket(0);
        assertEquals("Trace ID should be preserved", traceId, bucket.exemplarTraceId);
        assertTrue("Trace ID should be valid UUID format", 
            bucket.exemplarTraceId.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
    }
}
