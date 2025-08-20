// Composite Design Pattern Example
import java.util.*;

public class CompositeExample {
    interface Graphic { void draw(); }

    static class Dot implements Graphic {
        private final int x, y; public Dot(int x, int y) { this.x = x; this.y = y; }
        @Override public void draw() { System.out.println("Draw Dot at (" + x + "," + y + ")"); }
    }

    static class Circle implements Graphic {
        private final int x, y, r; public Circle(int x, int y, int r) { this.x = x; this.y = y; this.r = r; }
        @Override public void draw() { System.out.println("Draw Circle at (" + x + "," + y + ") r=" + r); }
    }

    static class CompoundGraphic implements Graphic {
        private final List<Graphic> children = new ArrayList<>();
        public void add(Graphic child) { children.add(child); }
        public void remove(Graphic child) { children.remove(child); }
        @Override public void draw() { children.forEach(Graphic::draw); }
    }

    public static void main(String[] args) {
        CompoundGraphic root = new CompoundGraphic();
        root.add(new Dot(1, 2));
        root.add(new Circle(5, 5, 3));
        CompoundGraphic group = new CompoundGraphic();
        group.add(new Dot(10, 10));
        group.add(new Circle(8, 8, 2));
        root.add(group);
        root.draw();
    }
}
