/**
 * Factory Pattern Implementation
 * 
 * The Factory pattern creates objects without specifying their exact classes.
 * It provides an interface for creating objects in a superclass, but allows
 * subclasses to alter the type of objects that will be created.
 * 
 * Real-world examples:
 * - Database connection factories
 * - UI component factories
 * - Payment processor factories
 * - Vehicle manufacturing
 */

import java.util.*;

// Product interface
interface Vehicle {
    void start();
    void stop();
    String getType();
    double getPrice();
}

// Concrete Products
class Car implements Vehicle {
    private String model;
    private double price;
    
    public Car(String model, double price) {
        this.model = model;
        this.price = price;
    }
    
    @Override
    public void start() {
        System.out.println("Car " + model + " engine started");
    }
    
    @Override
    public void stop() {
        System.out.println("Car " + model + " engine stopped");
    }
    
    @Override
    public String getType() {
        return "Car - " + model;
    }
    
    @Override
    public double getPrice() {
        return price;
    }
}

class Motorcycle implements Vehicle {
    private String model;
    private double price;
    
    public Motorcycle(String model, double price) {
        this.model = model;
        this.price = price;
    }
    
    @Override
    public void start() {
        System.out.println("Motorcycle " + model + " engine started");
    }
    
    @Override
    public void stop() {
        System.out.println("Motorcycle " + model + " engine stopped");
    }
    
    @Override
    public String getType() {
        return "Motorcycle - " + model;
    }
    
    @Override
    public double getPrice() {
        return price;
    }
}

class Truck implements Vehicle {
    private String model;
    private double price;
    
    public Truck(String model, double price) {
        this.model = model;
        this.price = price;
    }
    
    @Override
    public void start() {
        System.out.println("Truck " + model + " engine started");
    }
    
    @Override
    public void stop() {
        System.out.println("Truck " + model + " engine stopped");
    }
    
    @Override
    public String getType() {
        return "Truck - " + model;
    }
    
    @Override
    public double getPrice() {
        return price;
    }
}

// Simple Factory (Not a design pattern, but a programming idiom)
class SimpleVehicleFactory {
    public static Vehicle createVehicle(String type) {
        switch (type.toLowerCase()) {
            case "car":
                return new Car("Toyota Camry", 25000);
            case "motorcycle":
                return new Motorcycle("Harley Davidson", 15000);
            case "truck":
                return new Truck("Ford F-150", 35000);
            default:
                throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}

// Factory Method Pattern
abstract class VehicleFactory {
    // Factory method - to be implemented by subclasses
    public abstract Vehicle createVehicle(String model);
    
    // Template method that uses the factory method
    public Vehicle orderVehicle(String model) {
        Vehicle vehicle = createVehicle(model);
        
        // Common processing steps
        prepareVehicle(vehicle);
        inspectVehicle(vehicle);
        packageVehicle(vehicle);
        
        return vehicle;
    }
    
    private void prepareVehicle(Vehicle vehicle) {
        System.out.println("Preparing " + vehicle.getType());
    }
    
    private void inspectVehicle(Vehicle vehicle) {
        System.out.println("Inspecting " + vehicle.getType());
    }
    
    private void packageVehicle(Vehicle vehicle) {
        System.out.println("Packaging " + vehicle.getType());
    }
}

// Concrete Factories
class CarFactory extends VehicleFactory {
    @Override
    public Vehicle createVehicle(String model) {
        switch (model.toLowerCase()) {
            case "sedan":
                return new Car("Toyota Camry", 25000);
            case "suv":
                return new Car("Honda CR-V", 30000);
            case "luxury":
                return new Car("BMW 5 Series", 50000);
            default:
                throw new IllegalArgumentException("Unknown car model: " + model);
        }
    }
}

class MotorcycleFactory extends VehicleFactory {
    @Override
    public Vehicle createVehicle(String model) {
        switch (model.toLowerCase()) {
            case "cruiser":
                return new Motorcycle("Harley Davidson", 15000);
            case "sport":
                return new Motorcycle("Yamaha R1", 12000);
            case "touring":
                return new Motorcycle("BMW GS", 18000);
            default:
                throw new IllegalArgumentException("Unknown motorcycle model: " + model);
        }
    }
}

// Abstract Factory Pattern for Payment Processing
interface PaymentProcessor {
    void processPayment(double amount);
    String getPaymentMethod();
}

interface PaymentValidator {
    boolean validate(String paymentDetails);
}

// Payment implementations
class CreditCardProcessor implements PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing $" + amount + " via Credit Card");
    }
    
    @Override
    public String getPaymentMethod() {
        return "Credit Card";
    }
}

class PayPalProcessor implements PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing $" + amount + " via PayPal");
    }
    
    @Override
    public String getPaymentMethod() {
        return "PayPal";
    }
}

class CreditCardValidator implements PaymentValidator {
    @Override
    public boolean validate(String paymentDetails) {
        System.out.println("Validating credit card: " + paymentDetails);
        return paymentDetails.length() == 16; // Simple validation
    }
}

class PayPalValidator implements PaymentValidator {
    @Override
    public boolean validate(String paymentDetails) {
        System.out.println("Validating PayPal account: " + paymentDetails);
        return paymentDetails.contains("@"); // Simple email validation
    }
}

// Abstract Factory
abstract class PaymentFactory {
    public abstract PaymentProcessor createProcessor();
    public abstract PaymentValidator createValidator();
}

// Concrete Factories
class CreditCardFactory extends PaymentFactory {
    @Override
    public PaymentProcessor createProcessor() {
        return new CreditCardProcessor();
    }
    
    @Override
    public PaymentValidator createValidator() {
        return new CreditCardValidator();
    }
}

class PayPalFactory extends PaymentFactory {
    @Override
    public PaymentProcessor createProcessor() {
        return new PayPalProcessor();
    }
    
    @Override
    public PaymentValidator createValidator() {
        return new PayPalValidator();
    }
}

// Factory Registry for dynamic factory selection
class FactoryRegistry {
    private static final Map<String, PaymentFactory> factories = new HashMap<>();
    
    static {
        register("creditcard", new CreditCardFactory());
        register("paypal", new PayPalFactory());
    }
    
    public static void register(String type, PaymentFactory factory) {
        factories.put(type.toLowerCase(), factory);
    }
    
    public static PaymentFactory getFactory(String type) {
        PaymentFactory factory = factories.get(type.toLowerCase());
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for type: " + type);
        }
        return factory;
    }
    
    public static Set<String> getAvailableTypes() {
        return factories.keySet();
    }
}

// Demo class
class FactoryPatternDemo {
    public static void main(String[] args) {
        System.out.println("=== Factory Pattern Demo ===\n");
        
        // 1. Simple Factory Demo
        System.out.println("1. Simple Factory Pattern:");
        demonstrateSimpleFactory();
        
        // 2. Factory Method Demo
        System.out.println("\n2. Factory Method Pattern:");
        demonstrateFactoryMethod();
        
        // 3. Abstract Factory Demo
        System.out.println("\n3. Abstract Factory Pattern:");
        demonstrateAbstractFactory();
        
        // 4. Factory Registry Demo
        System.out.println("\n4. Factory Registry Pattern:");
        demonstrateFactoryRegistry();
    }
    
    private static void demonstrateSimpleFactory() {
        String[] vehicleTypes = {"car", "motorcycle", "truck"};
        
        for (String type : vehicleTypes) {
            try {
                Vehicle vehicle = SimpleVehicleFactory.createVehicle(type);
                System.out.println("Created: " + vehicle.getType() + " - $" + vehicle.getPrice());
                vehicle.start();
                vehicle.stop();
                System.out.println();
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
    
    private static void demonstrateFactoryMethod() {
        VehicleFactory carFactory = new CarFactory();
        VehicleFactory motorcycleFactory = new MotorcycleFactory();
        
        // Order different car types
        Vehicle sedan = carFactory.orderVehicle("sedan");
        System.out.println("Ordered: " + sedan.getType() + "\n");
        
        Vehicle sportBike = motorcycleFactory.orderVehicle("sport");
        System.out.println("Ordered: " + sportBike.getType() + "\n");
    }
    
    private static void demonstrateAbstractFactory() {
        // Process payment with Credit Card
        PaymentFactory creditCardFactory = new CreditCardFactory();
        processPayment(creditCardFactory, "1234567890123456", 100.0);
        
        System.out.println();
        
        // Process payment with PayPal
        PaymentFactory paypalFactory = new PayPalFactory();
        processPayment(paypalFactory, "user@example.com", 75.0);
    }
    
    private static void processPayment(PaymentFactory factory, String paymentDetails, double amount) {
        PaymentProcessor processor = factory.createProcessor();
        PaymentValidator validator = factory.createValidator();
        
        System.out.println("Processing payment with " + processor.getPaymentMethod());
        
        if (validator.validate(paymentDetails)) {
            processor.processPayment(amount);
            System.out.println("Payment successful!");
        } else {
            System.out.println("Payment validation failed!");
        }
    }
    
    private static void demonstrateFactoryRegistry() {
        System.out.println("Available payment methods: " + FactoryRegistry.getAvailableTypes());
        
        // Dynamic factory selection
        String[] paymentMethods = {"creditcard", "paypal"};
        String[] paymentDetails = {"1234567890123456", "john@example.com"};
        double[] amounts = {120.0, 80.0};
        
        for (int i = 0; i < paymentMethods.length; i++) {
            try {
                PaymentFactory factory = FactoryRegistry.getFactory(paymentMethods[i]);
                processPayment(factory, paymentDetails[i], amounts[i]);
                System.out.println();
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}

/*
 * Factory Pattern Benefits:
 * 
 * ✅ Advantages:
 * - Loose coupling between client and concrete classes
 * - Single Responsibility Principle
 * - Open/Closed Principle
 * - Easy to extend with new product types
 * - Centralized object creation logic
 * 
 * ❌ Disadvantages:
 * - Can make code more complex
 * - Requires many interfaces and classes
 * - May be overkill for simple scenarios
 * 
 * When to use:
 * - When you don't know beforehand the exact types of objects your code should work with
 * - When you want to provide users with a way to extend internal components
 * - When you want to save system resources by reusing existing objects
 * - When you need to separate the process of creating objects from using them
 */
