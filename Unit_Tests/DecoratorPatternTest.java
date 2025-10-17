import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Decorator Pattern
 * Tests component decoration and feature enhancement
 */
public class DecoratorPatternTest {

    @Test
    public void testSimpleCoffee_GetCost() {
        // Arrange
        Coffee coffee = new SimpleCoffee();

        // Act
        double cost = coffee.getCost();

        // Assert
        assertEquals(2.0, cost, 0.01);
    }

    @Test
    public void testSimpleCoffee_GetDescription() {
        // Arrange
        Coffee coffee = new SimpleCoffee();

        // Act
        String description = coffee.getDescription();

        // Assert
        assertEquals("Simple Coffee", description);
    }

    @Test
    public void testMilkDecorator_AddsCost() {
        // Arrange
        Coffee coffee = new SimpleCoffee();
        coffee = new MilkDecorator(coffee);

        // Act
        double cost = coffee.getCost();

        // Assert
        assertEquals(2.5, cost, 0.01);
    }

    @Test
    public void testMilkDecorator_UpdatesDescription() {
        // Arrange
        Coffee coffee = new SimpleCoffee();
        coffee = new MilkDecorator(coffee);

        // Act
        String description = coffee.getDescription();

        // Assert
        assertEquals("Simple Coffee, Milk", description);
    }

    @Test
    public void testMultipleDecorators() {
        // Arrange
        Coffee coffee = new SimpleCoffee();
        coffee = new MilkDecorator(coffee);
        coffee = new SugarDecorator(coffee);

        // Act
        double cost = coffee.getCost();
        String description = coffee.getDescription();

        // Assert
        assertEquals(3.0, cost, 0.01);
        assertEquals("Simple Coffee, Milk, Sugar", description);
    }

    @Test
    public void testAllDecorators() {
        // Arrange
        Coffee coffee = new SimpleCoffee();
        coffee = new MilkDecorator(coffee);
        coffee = new SugarDecorator(coffee);
        coffee = new WhipDecorator(coffee);

        // Act
        double cost = coffee.getCost();
        String description = coffee.getDescription();

        // Assert
        assertEquals(4.0, cost, 0.01);
        assertTrue(description.contains("Milk"));
        assertTrue(description.contains("Sugar"));
        assertTrue(description.contains("Whip"));
    }

    @Test
    public void testSameDecoratorMultipleTimes() {
        // Arrange
        Coffee coffee = new SimpleCoffee();
        coffee = new SugarDecorator(coffee);
        coffee = new SugarDecorator(coffee);

        // Act
        double cost = coffee.getCost();

        // Assert
        assertEquals(3.0, cost, 0.01); // 2.0 + 0.5 + 0.5
    }

    @Test
    public void testDecoratorOrder_DoesNotMatterForCost() {
        // Arrange
        Coffee coffee1 = new SimpleCoffee();
        coffee1 = new MilkDecorator(coffee1);
        coffee1 = new SugarDecorator(coffee1);

        Coffee coffee2 = new SimpleCoffee();
        coffee2 = new SugarDecorator(coffee2);
        coffee2 = new MilkDecorator(coffee2);

        // Act & Assert
        assertEquals(coffee1.getCost(), coffee2.getCost(), 0.01);
    }

    // Decorator pattern implementation
    interface Coffee {
        double getCost();
        String getDescription();
    }

    static class SimpleCoffee implements Coffee {
        @Override
        public double getCost() {
            return 2.0;
        }

        @Override
        public String getDescription() {
            return "Simple Coffee";
        }
    }

    static abstract class CoffeeDecorator implements Coffee {
        protected Coffee decoratedCoffee;

        public CoffeeDecorator(Coffee coffee) {
            this.decoratedCoffee = coffee;
        }

        @Override
        public double getCost() {
            return decoratedCoffee.getCost();
        }

        @Override
        public String getDescription() {
            return decoratedCoffee.getDescription();
        }
    }

    static class MilkDecorator extends CoffeeDecorator {
        public MilkDecorator(Coffee coffee) {
            super(coffee);
        }

        @Override
        public double getCost() {
            return super.getCost() + 0.5;
        }

        @Override
        public String getDescription() {
            return super.getDescription() + ", Milk";
        }
    }

    static class SugarDecorator extends CoffeeDecorator {
        public SugarDecorator(Coffee coffee) {
            super(coffee);
        }

        @Override
        public double getCost() {
            return super.getCost() + 0.5;
        }

        @Override
        public String getDescription() {
            return super.getDescription() + ", Sugar";
        }
    }

    static class WhipDecorator extends CoffeeDecorator {
        public WhipDecorator(Coffee coffee) {
            super(coffee);
        }

        @Override
        public double getCost() {
            return super.getCost() + 1.0;
        }

        @Override
        public String getDescription() {
            return super.getDescription() + ", Whip";
        }
    }
}
