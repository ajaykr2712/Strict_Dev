// Interpreter Design Pattern Example
import java.util.*;

public class InterpreterExample {
    interface Expression { int interpret(Map<String, Integer> ctx); }

    static class Number implements Expression {
        private final int number; Number(int number) { this.number = number; }
        @Override
        public int interpret(Map<String, Integer> ctx) { return number; }
    }

    static class Variable implements Expression {
        private final String name; Variable(String name) { this.name = name; }
        @Override
        public int interpret(Map<String, Integer> ctx) { return ctx.get(name); }
    }

    static class Add implements Expression {
        private final Expression left, right; Add(Expression l, Expression r) { left = l; right = r; }
        @Override
        public int interpret(Map<String, Integer> ctx) { return left.interpret(ctx) + right.interpret(ctx); }
    }

    static class Subtract implements Expression {
        private final Expression left, right; Subtract(Expression l, Expression r) { left = l; right = r; }
        @Override
        public int interpret(Map<String, Integer> ctx) { return left.interpret(ctx) - right.interpret(ctx); }
    }

    public static void main(String[] args) {
        // Interpret: x + 5 - y where x=10, y=3
        Expression expr = new Subtract(new Add(new Variable("x"), new Number(5)), new Variable("y"));
        Map<String, Integer> ctx = new HashMap<>();
        ctx.put("x", 10);
        ctx.put("y", 3);
        System.out.println("Result: " + expr.interpret(ctx));
    }
}
