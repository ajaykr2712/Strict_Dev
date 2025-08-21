// Visitor Design Pattern Example
import java.util.*;

public class VisitorExample {
    interface Shape { void accept(Visitor v); }
    static class Dot implements Shape { final int x,y; Dot(int x,int y){this.x=x;this.y=y;} @Override public void accept(Visitor v){ v.visit(this);} }
    static class Circle implements Shape { final int x,y,r; Circle(int x,int y,int r){this.x=x;this.y=y;this.r=r;} @Override public void accept(Visitor v){ v.visit(this);} }
    static class Rectangle implements Shape { final int x,y,w,h; Rectangle(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;} @Override public void accept(Visitor v){ v.visit(this);} }

    interface Visitor { void visit(Dot d); void visit(Circle c); void visit(Rectangle r); }

    static class AreaCalculator implements Visitor {
        double total = 0;
        @Override public void visit(Dot d) { /* area ~ 0 */ }
        @Override public void visit(Circle c) { total += Math.PI * c.r * c.r; }
        @Override public void visit(Rectangle r) { total += r.w * r.h; }
    }

    public static void main(String[] args) {
        List<Shape> shapes = Arrays.asList(new Dot(1,2), new Circle(0,0,3), new Rectangle(0,0,2,4));
        AreaCalculator calc = new AreaCalculator();
        shapes.forEach(s -> s.accept(calc));
        System.out.println("Total area: " + calc.total);
    }
}
