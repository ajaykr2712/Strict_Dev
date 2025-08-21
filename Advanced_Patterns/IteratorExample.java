// Iterator Design Pattern Example
import java.util.*;

public class IteratorExample {
    static class NameRepository implements Iterable<String> {
        private final List<String> names = new ArrayList<>();
        public void add(String name) { names.add(name); }
        @Override public Iterator<String> iterator() { return new NameIterator(); }
        private class NameIterator implements Iterator<String> {
            int index;
            @Override public boolean hasNext() { return index < names.size(); }
            @Override public String next() { return names.get(index++); }
        }
    }

    public static void main(String[] args) {
        NameRepository repo = new NameRepository();
        repo.add("Alice"); repo.add("Bob"); repo.add("Charlie");
        for (String n : repo) System.out.println(n);
    }
}
