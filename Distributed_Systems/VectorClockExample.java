// VectorClockExample.java
// NOTE: Placed under Distributed_Systems/ without package for consistency with other examples lacking explicit packages.
// Demonstrates vector clock merge logic for partial ordering in distributed systems.

import java.util.*;

public class VectorClockExample {
    static class VectorClock {
        private final Map<String, Integer> versions = new HashMap<>();

        void tick(String node) {
            versions.merge(node, 1, Integer::sum);
        }

        void merge(VectorClock other) {
            other.versions.forEach((k,v) -> versions.merge(k, v, Math::max));
        }

        Relation compare(VectorClock other) {
            boolean thisGreater = false;
            boolean otherGreater = false;
            Set<String> keys = new HashSet<>(versions.keySet());
            keys.addAll(other.versions.keySet());
            for (String k : keys) {
                int a = versions.getOrDefault(k, 0);
                int b = other.versions.getOrDefault(k, 0);
                if (a < b) otherGreater = true; else if (a > b) thisGreater = true;
                if (thisGreater && otherGreater) return Relation.CONCURRENT;
            }
            if (!thisGreater && !otherGreater) return Relation.EQUAL;
            return thisGreater ? Relation.AFTER : Relation.BEFORE;
        }

        @Override public String toString(){ return versions.toString(); }
    }

    enum Relation { BEFORE, AFTER, CONCURRENT, EQUAL }

    public static void main(String[] args) {
        var a = new VectorClock();
        var b = new VectorClock();
        a.tick("A");
        a.tick("A");
        b.tick("B");
        a.merge(b); // propagate B's version
        b.tick("B");
        var rel1 = a.compare(b); // concurrent?
        System.out.println("a=" + a + " b=" + b + " relation=" + rel1);

        var c = new VectorClock();
        c.merge(a); // copy state
        c.tick("A"); // advances beyond a
        System.out.println("c vs a => " + c.compare(a));
    }
}
