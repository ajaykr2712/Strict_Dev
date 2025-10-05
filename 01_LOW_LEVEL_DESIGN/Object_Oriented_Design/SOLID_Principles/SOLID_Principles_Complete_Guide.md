# SOLID Principles - Complete Guide

The SOLID principles are five design principles that make software designs more understandable, flexible, and maintainable. They were introduced by Robert C. Martin (Uncle Bob) and form the foundation of clean, object-oriented design.

## Overview

**SOLID** is an acronym for:
- **S** - Single Responsibility Principle (SRP)
- **O** - Open/Closed Principle (OCP)
- **L** - Liskov Substitution Principle (LSP)
- **I** - Interface Segregation Principle (ISP)
- **D** - Dependency Inversion Principle (DIP)

## 1. Single Responsibility Principle (SRP)

> "A class should have only one reason to change."

### Definition
Each class should have only one responsibility and only one reason to change. This principle helps to create more focused, cohesive classes that are easier to understand, test, and maintain.

### ❌ Violation Example
```java
// BAD: Multiple responsibilities in one class
class Employee {
    private String name;
    private double salary;
    
    // Responsibility 1: Employee data management
    public void setName(String name) { this.name = name; }
    public void setSalary(double salary) { this.salary = salary; }
    
    // Responsibility 2: Database operations
    public void saveToDatabase() {
        // Save employee to database
    }
    
    // Responsibility 3: Report generation
    public void generatePayrollReport() {
        // Generate payroll report
    }
    
    // Responsibility 4: Tax calculations
    public double calculateTax() {
        return salary * 0.2;
    }
}
```

### ✅ Correct Implementation
```java
// GOOD: Single responsibility per class
class Employee {
    private String name;
    private double salary;
    
    // Only responsibility: Employee data management
    public void setName(String name) { this.name = name; }
    public void setSalary(double salary) { this.salary = salary; }
    public String getName() { return name; }
    public double getSalary() { return salary; }
}

class EmployeeRepository {
    // Only responsibility: Database operations
    public void save(Employee employee) {
        // Save employee to database
    }
    
    public Employee findById(int id) {
        // Find employee by id
        return new Employee();
    }
}

class PayrollReportGenerator {
    // Only responsibility: Report generation
    public void generateReport(List<Employee> employees) {
        // Generate payroll report
    }
}

class TaxCalculator {
    // Only responsibility: Tax calculations
    public double calculateTax(Employee employee) {
        return employee.getSalary() * 0.2;
    }
}
```

### Benefits
- **Easier to understand**: Each class has a clear, single purpose
- **Easier to test**: Focused functionality is simpler to test
- **Easier to maintain**: Changes in one area don't affect others
- **Reduced coupling**: Classes depend on fewer things

## 2. Open/Closed Principle (OCP)

> "Software entities should be open for extension but closed for modification."

### Definition
You should be able to extend a class's behavior without modifying its existing code. This is typically achieved through inheritance, composition, and polymorphism.

### ❌ Violation Example
```java
// BAD: Need to modify existing code for new shapes
class AreaCalculator {
    public double calculateArea(Object shape) {
        if (shape instanceof Rectangle) {
            Rectangle rectangle = (Rectangle) shape;
            return rectangle.getWidth() * rectangle.getHeight();
        } else if (shape instanceof Circle) {
            Circle circle = (Circle) shape;
            return Math.PI * circle.getRadius() * circle.getRadius();
        }
        // Adding new shape requires modifying this method
        return 0;
    }
}
```

### ✅ Correct Implementation
```java
// GOOD: Open for extension, closed for modification
interface Shape {
    double calculateArea();
}

class Rectangle implements Shape {
    private double width;
    private double height;
    
    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }
    
    @Override
    public double calculateArea() {
        return width * height;
    }
}

class Circle implements Shape {
    private double radius;
    
    public Circle(double radius) {
        this.radius = radius;
    }
    
    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}

class Triangle implements Shape {
    private double base;
    private double height;
    
    public Triangle(double base, double height) {
        this.base = base;
        this.height = height;
    }
    
    @Override
    public double calculateArea() {
        return 0.5 * base * height;
    }
}

class AreaCalculator {
    public double calculateArea(Shape shape) {
        return shape.calculateArea(); // No modification needed for new shapes
    }
    
    public double calculateTotalArea(List<Shape> shapes) {
        return shapes.stream()
                    .mapToDouble(Shape::calculateArea)
                    .sum();
    }
}
```

### Benefits
- **Extensible**: New functionality can be added without changing existing code
- **Stable**: Existing code remains unchanged and tested
- **Maintainable**: Reduces the risk of introducing bugs
- **Flexible**: Supports multiple implementations

## 3. Liskov Substitution Principle (LSP)

> "Objects of a superclass should be replaceable with objects of a subclass without breaking the application."

### Definition
Subtypes must be substitutable for their base types. This means that derived classes must be completely substitutable for their base classes.

### ❌ Violation Example
```java
// BAD: LSP violation - Square changes Rectangle behavior
class Rectangle {
    protected double width;
    protected double height;
    
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
    public double getArea() { return width * height; }
}

class Square extends Rectangle {
    @Override
    public void setWidth(double width) {
        this.width = width;
        this.height = width; // Violates LSP - unexpected behavior
    }
    
    @Override
    public void setHeight(double height) {
        this.width = height;
        this.height = height; // Violates LSP - unexpected behavior
    }
}

// This breaks when using Square instead of Rectangle
void processRectangle(Rectangle rectangle) {
    rectangle.setWidth(5);
    rectangle.setHeight(4);
    // Expected area: 20, but Square will give 16
    assert rectangle.getArea() == 20; // This fails for Square
}
```

### ✅ Correct Implementation
```java
// GOOD: LSP compliant design
abstract class Shape {
    public abstract double getArea();
    public abstract double getPerimeter();
}

class Rectangle extends Shape {
    private final double width;
    private final double height;
    
    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }
    
    @Override
    public double getArea() {
        return width * height;
    }
    
    @Override
    public double getPerimeter() {
        return 2 * (width + height);
    }
    
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}

class Square extends Shape {
    private final double side;
    
    public Square(double side) {
        this.side = side;
    }
    
    @Override
    public double getArea() {
        return side * side;
    }
    
    @Override
    public double getPerimeter() {
        return 4 * side;
    }
    
    public double getSide() { return side; }
}

// Both Rectangle and Square can be used interchangeably as Shape
void processShape(Shape shape) {
    System.out.println("Area: " + shape.getArea());
    System.out.println("Perimeter: " + shape.getPerimeter());
}
```

### Benefits
- **Reliability**: Substitution doesn't break existing functionality
- **Polymorphism**: True polymorphic behavior is achieved
- **Testability**: Base class tests should pass for all derived classes
- **Maintainability**: Code using base classes works with all derived classes

## 4. Interface Segregation Principle (ISP)

> "Clients should not be forced to depend on interfaces they do not use."

### Definition
Many specific interfaces are better than one general-purpose interface. Classes should not be forced to implement methods they don't need.

### ❌ Violation Example
```java
// BAD: Fat interface forces unnecessary implementations
interface Worker {
    void work();
    void eat();
    void sleep();
    void attendMeetings();
    void writeReports();
    void operateMachinery();
    void manageTeam();
}

class Developer implements Worker {
    @Override
    public void work() { /* Write code */ }
    @Override
    public void eat() { /* Eat lunch */ }
    @Override
    public void sleep() { /* Not applicable during work */ }
    @Override
    public void attendMeetings() { /* Attend meetings */ }
    @Override
    public void writeReports() { /* Write technical reports */ }
    @Override
    public void operateMachinery() { /* Not applicable - forced to implement */ }
    @Override
    public void manageTeam() { /* Not applicable - forced to implement */ }
}

class Robot implements Worker {
    @Override
    public void work() { /* Perform tasks */ }
    @Override
    public void eat() { /* Not applicable - forced to implement */ }
    @Override
    public void sleep() { /* Not applicable - forced to implement */ }
    @Override
    public void attendMeetings() { /* Not applicable - forced to implement */ }
    @Override
    public void writeReports() { /* Not applicable - forced to implement */ }
    @Override
    public void operateMachinery() { /* Operate machinery */ }
    @Override
    public void manageTeam() { /* Not applicable - forced to implement */ }
}
```

### ✅ Correct Implementation
```java
// GOOD: Segregated interfaces
interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

interface Sleepable {
    void sleep();
}

interface MeetingAttendable {
    void attendMeetings();
}

interface ReportWritable {
    void writeReports();
}

interface MachineOperable {
    void operateMachinery();
}

interface TeamManageable {
    void manageTeam();
}

// Implement only needed interfaces
class Developer implements Workable, Eatable, Sleepable, MeetingAttendable, ReportWritable {
    @Override
    public void work() { /* Write code */ }
    @Override
    public void eat() { /* Eat lunch */ }
    @Override
    public void sleep() { /* Sleep at home */ }
    @Override
    public void attendMeetings() { /* Attend meetings */ }
    @Override
    public void writeReports() { /* Write technical reports */ }
}

class Robot implements Workable, MachineOperable {
    @Override
    public void work() { /* Perform tasks */ }
    @Override
    public void operateMachinery() { /* Operate machinery */ }
}

class Manager implements Workable, Eatable, Sleepable, MeetingAttendable, ReportWritable, TeamManageable {
    @Override
    public void work() { /* Manage projects */ }
    @Override
    public void eat() { /* Business lunches */ }
    @Override
    public void sleep() { /* Sleep at home */ }
    @Override
    public void attendMeetings() { /* Lead meetings */ }
    @Override
    public void writeReports() { /* Write management reports */ }
    @Override
    public void manageTeam() { /* Manage team members */ }
}
```

### Benefits
- **Flexibility**: Classes implement only what they need
- **Maintainability**: Changes to unused methods don't affect classes
- **Testability**: Smaller interfaces are easier to mock and test
- **Cohesion**: Interfaces have focused responsibilities

## 5. Dependency Inversion Principle (DIP)

> "High-level modules should not depend on low-level modules. Both should depend on abstractions."

### Definition
- High-level modules should not depend on low-level modules
- Both should depend on abstractions (interfaces)
- Abstractions should not depend on details
- Details should depend on abstractions

### ❌ Violation Example
```java
// BAD: High-level class depends on low-level classes
class MySQLDatabase {
    public void save(String data) {
        System.out.println("Saving to MySQL: " + data);
    }
}

class EmailService {
    public void sendEmail(String message) {
        System.out.println("Sending email: " + message);
    }
}

class UserService {
    private MySQLDatabase database; // Direct dependency on concrete class
    private EmailService emailService; // Direct dependency on concrete class
    
    public UserService() {
        this.database = new MySQLDatabase(); // Tight coupling
        this.emailService = new EmailService(); // Tight coupling
    }
    
    public void createUser(String userData) {
        database.save(userData);
        emailService.sendEmail("Welcome new user!");
    }
}
```

### ✅ Correct Implementation
```java
// GOOD: Depend on abstractions
interface Database {
    void save(String data);
    String findById(String id);
}

interface NotificationService {
    void sendNotification(String message);
}

// Low-level modules implement abstractions
class MySQLDatabase implements Database {
    @Override
    public void save(String data) {
        System.out.println("Saving to MySQL: " + data);
    }
    
    @Override
    public String findById(String id) {
        return "User data from MySQL for ID: " + id;
    }
}

class PostgreSQLDatabase implements Database {
    @Override
    public void save(String data) {
        System.out.println("Saving to PostgreSQL: " + data);
    }
    
    @Override
    public String findById(String id) {
        return "User data from PostgreSQL for ID: " + id;
    }
}

class EmailService implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("Sending email: " + message);
    }
}

class SMSService implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("Sending SMS: " + message);
    }
}

// High-level module depends on abstractions
class UserService {
    private final Database database;
    private final NotificationService notificationService;
    
    // Dependency injection through constructor
    public UserService(Database database, NotificationService notificationService) {
        this.database = database;
        this.notificationService = notificationService;
    }
    
    public void createUser(String userData) {
        database.save(userData);
        notificationService.sendNotification("Welcome new user!");
    }
    
    public String getUser(String id) {
        return database.findById(id);
    }
}

// Usage with dependency injection
class Application {
    public static void main(String[] args) {
        // Can easily switch implementations
        Database database = new MySQLDatabase();
        NotificationService notificationService = new EmailService();
        
        UserService userService = new UserService(database, notificationService);
        userService.createUser("John Doe");
        
        // Easy to change implementations
        Database postgresDB = new PostgreSQLDatabase();
        NotificationService smsService = new SMSService();
        
        UserService userService2 = new UserService(postgresDB, smsService);
        userService2.createUser("Jane Smith");
    }
}
```

### Benefits
- **Flexibility**: Easy to swap implementations
- **Testability**: Easy to mock dependencies for testing
- **Maintainability**: Changes in low-level modules don't affect high-level modules
- **Reusability**: High-level modules can work with different implementations

## Summary

### Why SOLID Principles Matter

1. **Maintainability**: Code is easier to modify and extend
2. **Testability**: Smaller, focused classes are easier to test
3. **Flexibility**: Systems can adapt to changing requirements
4. **Reusability**: Well-designed components can be reused
5. **Understandability**: Code is easier to read and comprehend

### Key Takeaways

- **SRP**: One class, one responsibility
- **OCP**: Extend behavior without modifying existing code
- **LSP**: Subtypes must be substitutable for their base types
- **ISP**: Many specific interfaces > one general interface
- **DIP**: Depend on abstractions, not concretions

### Best Practices

1. **Start Simple**: Don't over-engineer from the beginning
2. **Refactor Gradually**: Apply SOLID principles during refactoring
3. **Use Dependency Injection**: Helps with DIP and testability
4. **Think in Terms of Contracts**: Focus on interfaces and abstractions
5. **Regular Code Reviews**: Ensure principles are being followed
6. **Automated Testing**: SOLID code is easier to test

Remember: SOLID principles are guidelines, not rigid rules. Apply them judiciously based on the complexity and requirements of your system.
