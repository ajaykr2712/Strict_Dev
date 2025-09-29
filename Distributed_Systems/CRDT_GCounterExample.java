// CRDT_GCounterExample.java
// NOTE: File under Distributed_Systems/ with no package for consistency with other examples.
// Grow-only counter (G-Counter) CRDT merging from multiple replicas.

import java.util.*;

public class CRDT_GCounterExample {
    static class GCounter {
        private final Map<String, Long> counts = new HashMap<>();
        void increment(String nodeId, long delta) { counts.merge(nodeId, delta, Long::sum); }
        void merge(GCounter other) { other.counts.forEach((k,v) -> counts.merge(k, v, Math::max)); }
        long value() { return counts.values().stream().mapToLong(Long::longValue).sum(); }
        @Override public String toString(){ return counts.toString(); }
    }
    public static void main(String[] args) {
        var a = new GCounter();
        var b = new GCounter();
        a.increment("A", 3);
        b.increment("B", 5);
        a.merge(b);
        b.increment("B", 2);
        b.merge(a);
        System.out.println("A=" + a + " value=" + a.value());
        System.out.println("B=" + b + " value=" + b.value());
    }
}
