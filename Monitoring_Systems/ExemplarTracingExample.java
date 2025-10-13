// ExemplarTracingExample.java
// Demonstrates linking metrics data points with trace exemplars for high-cardinality root cause.
// This is a conceptual example (no real OTLP exporter) focusing on correlation mechanics.

import java.time.Instant;
import java.util.*;

public class ExemplarTracingExample {
    static class HistogramBucket {
        final double upperBound;
        long count;
        // store at most one exemplar trace id per bucket for simplicity
        String exemplarTraceId;
        Instant exemplarTime;
        HistogramBucket(double upperBound){this.upperBound=upperBound;}
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

        void observe(long millis, String currentTraceId){
            for (var b : buckets) {
                if (millis <= b.upperBound) {
                    b.count++;
                    // exemplar sampling rule: capture if bucket count is power of two or no exemplar yet
                    if (b.exemplarTraceId == null || (b.count & (b.count - 1)) == 0) {
                        b.exemplarTraceId = currentTraceId;
                        b.exemplarTime = Instant.now();
                    }
                    break;
                }
            }
        }

        void print() {
            System.out.println("Latency Histogram with Exemplars:");
            for (var b : buckets) {
                System.out.printf(" <= %-6s | count=%-4d exemplar=%s time=%s%n",
                        b.upperBound == Double.POSITIVE_INFINITY ? "+Inf" : (long)b.upperBound + "ms",
                        b.count,
                        b.exemplarTraceId,
                        b.exemplarTime);
            }
        }
    }

    // Trace span simulation
    static class Span implements AutoCloseable {
        final String traceId = UUID.randomUUID().toString();
        final String name;
        final long start = System.nanoTime();
        Span(String name){this.name=name;}
        long end(){return (System.nanoTime()-start)/1_000_000;}
        @Override public void close() { /* noop */ }
    }

    public static void main(String[] args) {
        var hist = new LatencyHistogram();
        Random r = new Random();
        for (int i=0;i<200;i++) {
            try (var span = new Span("operation")) {
                long simulated = (long)(Math.pow(r.nextDouble(), 2) * 900); // skew fast
                try { Thread.sleep(simulated/10); } catch (InterruptedException ignored) {}
                hist.observe(simulated, span.traceId);
            }
        }
        hist.print();
        System.out.println("Query exemplar trace id in backend tracing system for deep dive root cause.");
    }
}
