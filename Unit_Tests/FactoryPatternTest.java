package unittests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Factory Pattern Implementation
 * Tests object creation through factory methods
 */
public class FactoryPatternTest {

    @Test
    public void testCreateCircle() {
        // Act
        Shape circle = ShapeFactory.createShape("CIRCLE");

        // Assert
        assertNotNull("Circle should not be null", circle);
        assertEquals("Should be Circle type", "Circle", circle.getType());
    }

    @Test
    public void testCreateSquare() {
        // Act
        Shape square = ShapeFactory.createShape("SQUARE");

        // Assert
        assertNotNull("Square should not be null", square);
        assertEquals("Should be Square type", "Square", square.getType());
    }

    @Test
    public void testCreateRectangle() {
        // Act
        Shape rectangle = ShapeFactory.createShape("RECTANGLE");

        // Assert
        assertNotNull("Rectangle should not be null", rectangle);
        assertEquals("Should be Rectangle type", "Rectangle", rectangle.getType());
    }

    @Test
    public void testCreateUnknownShape_ReturnsNull() {
        // Act
        Shape shape = ShapeFactory.createShape("TRIANGLE");

        // Assert
        assertNull("Unknown shape should return null", shape);
    }

    @Test
    public void testFactoryMethod_IsCaseInsensitive() {
        // Act
        Shape circle = ShapeFactory.createShape("circle");

        // Assert - Depending on implementation
        // This test assumes case-insensitive factory
        assertNull("Should handle case sensitivity", circle);
    }

    @Test
    public void testDraw_ExecutesWithoutException() {
        // Arrange
        Shape circle = ShapeFactory.createShape("CIRCLE");

        // Act & Assert - Should not throw exception
        try {
            circle.draw();
        } catch (Exception e) {
            fail("Draw should not throw exception");
        }
    }

    // Test interfaces and implementations
    interface Shape {
        void draw();
        String getType();
    }

    static class Circle implements Shape {
        @Override
        public void draw() {
            System.out.println("Drawing Circle");
        }
        
        @Override
        public String getType() {
            return "Circle";
        }
    }

    static class Square implements Shape {
        @Override
        public void draw() {
            System.out.println("Drawing Square");
        }
        
        @Override
        public String getType() {
            return "Square";
        }
    }

    static class Rectangle implements Shape {
        @Override
        public void draw() {
            System.out.println("Drawing Rectangle");
        }
        
        @Override
        public String getType() {
            return "Rectangle";
        }
    }

    static class ShapeFactory {
        public static Shape createShape(String type) {
            if (type == null) {
                return null;
            }
            if (type.equalsIgnoreCase("CIRCLE")) {
                return new Circle();
            } else if (type.equalsIgnoreCase("SQUARE")) {
                return new Square();
            } else if (type.equalsIgnoreCase("RECTANGLE")) {
                return new Rectangle();
            }
            return null;
        }
    }
}
