import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Unit tests for CRDT_GCounterExample (Grow-only Counter)
 * Tests the conflict-free replicated data type implementation
 * for distributed counter scenarios.
 */
public class GCounterCRDTTest {

    /**
     * Mock GCounter implementation for testing
     */
    static class GCounter {
        private final Map<String, Long> counts = new HashMap<>();

        void increment(String nodeId, long delta) {
            counts.merge(nodeId, delta, Long::sum);
        }

        void merge(GCounter other) {
            other.counts.forEach((k, v) -> counts.merge(k, v, Math::max));
        }

        long value() {
            return counts.values().stream().mapToLong(Long::longValue).sum();
        }

        Map<String, Long> getCounts() {
            return new HashMap<>(counts);
        }

        @Override
        public String toString() {
            return counts.toString();
        }
    }

    private GCounter counter;

    @Before
    public void setUp() {
        counter = new GCounter();
    }

    @Test
    public void testInitialCounterValue() {
        // Arrange & Act & Assert
        assertEquals("Initial counter value should be 0", 0, counter.value());
        assertTrue("Initial counts map should be empty", counter.getCounts().isEmpty());
    }

    @Test
    public void testSingleNodeIncrement() {
        // Arrange & Act
        counter.increment("node1", 5);

        // Assert
        assertEquals("Counter value should be 5", 5, counter.value());
        assertEquals("Node1 count should be 5", Long.valueOf(5), counter.getCounts().get("node1"));
    }

    @Test
    public void testMultipleIncrementsOnSameNode() {
        // Arrange & Act
        counter.increment("node1", 3);
        counter.increment("node1", 2);
        counter.increment("node1", 5);

        // Assert
        assertEquals("Counter value should be 10 (3+2+5)", 10, counter.value());
        assertEquals("Node1 count should be 10", Long.valueOf(10), counter.getCounts().get("node1"));
    }

    @Test
    public void testMultipleNodesIncrement() {
        // Arrange & Act
        counter.increment("node1", 5);
        counter.increment("node2", 3);
        counter.increment("node3", 7);

        // Assert
        assertEquals("Counter value should be 15 (5+3+7)", 15, counter.value());
        assertEquals("Node1 count should be 5", Long.valueOf(5), counter.getCounts().get("node1"));
        assertEquals("Node2 count should be 3", Long.valueOf(3), counter.getCounts().get("node2"));
        assertEquals("Node3 count should be 7", Long.valueOf(7), counter.getCounts().get("node3"));
    }

    @Test
    public void testMergeTwoCounters() {
        // Arrange
        GCounter counter1 = new GCounter();
        GCounter counter2 = new GCounter();
        counter1.increment("node1", 5);
        counter2.increment("node2", 3);

        // Act
        counter1.merge(counter2);

        // Assert
        assertEquals("Counter1 value should be 8 after merge", 8, counter1.value());
        assertEquals("Counter1 should have node1 count", Long.valueOf(5), counter1.getCounts().get("node1"));
        assertEquals("Counter1 should have node2 count", Long.valueOf(3), counter1.getCounts().get("node2"));
    }

    @Test
    public void testMergeWithOverlappingNodes() {
        // Arrange
        GCounter counter1 = new GCounter();
        GCounter counter2 = new GCounter();
        counter1.increment("node1", 5);
        counter1.increment("node2", 3);
        counter2.increment("node2", 7);
        counter2.increment("node3", 2);

        // Act
        counter1.merge(counter2);

        // Assert
        assertEquals("Counter1 value should use max for node2", 14, counter1.value()); // 5 + max(3,7) + 2
        assertEquals("Node1 count should be 5", Long.valueOf(5), counter1.getCounts().get("node1"));
        assertEquals("Node2 count should be max(3,7)=7", Long.valueOf(7), counter1.getCounts().get("node2"));
        assertEquals("Node3 count should be 2", Long.valueOf(2), counter1.getCounts().get("node3"));
    }

    @Test
    public void testMergeIdempotency() {
        // Arrange
        GCounter counter1 = new GCounter();
        GCounter counter2 = new GCounter();
        counter1.increment("node1", 5);
        counter2.increment("node2", 3);

        // Act - Merge multiple times
        counter1.merge(counter2);
        long valueAfterFirstMerge = counter1.value();
        counter1.merge(counter2);
        long valueAfterSecondMerge = counter1.value();

        // Assert
        assertEquals("Multiple merges of same state should be idempotent", 
            valueAfterFirstMerge, valueAfterSecondMerge);
    }

    @Test
    public void testMergeCommutative() {
        // Arrange
        GCounter counter1 = new GCounter();
        GCounter counter2 = new GCounter();
        GCounter counter3 = new GCounter();
        GCounter counter4 = new GCounter();

        counter1.increment("node1", 5);
        counter2.increment("node1", 5);
        counter3.increment("node2", 3);
        counter4.increment("node2", 3);

        // Act - Merge in different orders
        counter1.merge(counter3);
        counter4.merge(counter2);

        // Assert
        assertEquals("Merge should be commutative", counter1.value(), counter4.value());
        assertEquals("Node states should be identical", counter1.getCounts(), counter4.getCounts());
    }

    @Test
    public void testMergeAssociative() {
        // Arrange
        GCounter a = new GCounter();
        GCounter b = new GCounter();
        GCounter c = new GCounter();
        GCounter x = new GCounter();
        GCounter y = new GCounter();
        GCounter z = new GCounter();

        a.increment("node1", 2);
        b.increment("node1", 2);
        x.increment("node1", 2);
        y.increment("node1", 2);

        b.increment("node2", 3);
        y.increment("node2", 3);

        c.increment("node3", 5);
        z.increment("node3", 5);

        // Act - (a merge b) merge c
        a.merge(b);
        a.merge(c);

        // a merge (b merge c)
        y.merge(z);
        x.merge(y);

        // Assert
        assertEquals("Merge should be associative", a.value(), x.value());
        assertEquals("Node states should be identical", a.getCounts(), x.getCounts());
    }

    @Test
    public void testMergeEmptyCounter() {
        // Arrange
        GCounter counter1 = new GCounter();
        GCounter counter2 = new GCounter();
        counter1.increment("node1", 5);

        // Act
        counter1.merge(counter2);

        // Assert
        assertEquals("Merging with empty counter should not change value", 5, counter1.value());
    }

    @Test
    public void testEmptyCounterMergeWithNonEmpty() {
        // Arrange
        GCounter counter1 = new GCounter();
        GCounter counter2 = new GCounter();
        counter2.increment("node1", 5);

        // Act
        counter1.merge(counter2);

        // Assert
        assertEquals("Empty counter should adopt values from non-empty", 5, counter1.value());
    }

    @Test
    public void testIncrementWithZeroDelta() {
        // Arrange & Act
        counter.increment("node1", 0);

        // Assert
        assertEquals("Counter value should be 0", 0, counter.value());
        assertEquals("Node1 should have count of 0", Long.valueOf(0), counter.getCounts().get("node1"));
    }

    @Test
    public void testIncrementWithLargeValues() {
        // Arrange & Act
        counter.increment("node1", Long.MAX_VALUE / 3);
        counter.increment("node2", Long.MAX_VALUE / 3);
        counter.increment("node3", Long.MAX_VALUE / 3);

        // Assert
        long expectedValue = (Long.MAX_VALUE / 3) * 3;
        assertEquals("Counter should handle large values", expectedValue, counter.value());
    }

    @Test
    public void testConcurrentIncrements() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        int incrementsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            final String nodeId = "node" + i;
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.increment(nodeId, 1);
                }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertEquals("Counter value should be correct", 
            threadCount * incrementsPerThread, counter.value());
    }

    @Test
    public void testDistributedScenario() {
        // Arrange - Simulate 3 replicas
        GCounter replica1 = new GCounter();
        GCounter replica2 = new GCounter();
        GCounter replica3 = new GCounter();

        // Act - Each replica receives different updates
        replica1.increment("nodeA", 10);
        replica2.increment("nodeB", 20);
        replica3.increment("nodeC", 30);

        // Simulate gossip protocol - replicas exchange state
        replica1.merge(replica2);
        replica1.merge(replica3);

        replica2.merge(replica1);
        replica2.merge(replica3);

        replica3.merge(replica1);
        replica3.merge(replica2);

        // Assert - All replicas converge to same value
        assertEquals("Replica1 should converge", 60, replica1.value());
        assertEquals("Replica2 should converge", 60, replica2.value());
        assertEquals("Replica3 should converge", 60, replica3.value());
        assertEquals("All replicas should have same state", replica1.getCounts(), replica2.getCounts());
        assertEquals("All replicas should have same state", replica2.getCounts(), replica3.getCounts());
    }

    @Test
    public void testToString() {
        // Arrange
        counter.increment("node1", 5);
        counter.increment("node2", 3);

        // Act
        String str = counter.toString();

        // Assert
        assertNotNull("toString should not return null", str);
        assertTrue("toString should contain node1", str.contains("node1"));
        assertTrue("toString should contain node2", str.contains("node2"));
    }

    @Test
    public void testPartialReplication() {
        // Arrange
        GCounter source = new GCounter();
        GCounter replica = new GCounter();

        source.increment("node1", 5);
        source.increment("node2", 10);

        replica.increment("node2", 7);
        replica.increment("node3", 3);

        // Act
        replica.merge(source);

        // Assert
        assertEquals("Replica should have max value for overlapping nodes", 
            5 + 10 + 3, replica.value()); // node1(5) + node2(max(10,7)) + node3(3)
    }
}
