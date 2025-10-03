// SOLID Principles - Object Oriented Design Implementation

/**
 * SOLID Principles Implementation
 * 
 * Real-world Use Case: E-commerce Order Processing System
 * - Demonstrates all five SOLID principles in action
 * - Shows how to design maintainable, extensible, and testable code
 * - Includes examples of violations and their fixes
 * 
 * SOLID Principles:
 * S - Single Responsibility Principle
 * O - Open/Closed Principle  
 * L - Liskov Substitution Principle
 * I - Interface Segregation Principle
 * D - Dependency Inversion Principle
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// =============================================================================
// 1. SINGLE RESPONSIBILITY PRINCIPLE (SRP)
// =============================================================================

// VIOLATION: This class has multiple responsibilities
class OrderProcessorBad {
    public void processOrder(Order order) {
        // Validate order
        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order cannot be empty");
        }
        
        // Calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        order.setTotal(total);
        
        // Save to database
        System.out.println("Saving order to database...");
        
        // Send email
        System.out.println("Sending confirmation email...");
        
        // Update inventory
        System.out.println("Updating inventory...");
    }
}

// CORRECT: Each class has a single responsibility
class Order {
    private String orderId;
    private String customerId;
    private List<OrderItem> items;
    private BigDecimal total;
    private LocalDateTime orderDate;
    private OrderStatus status;
    
    public Order(String orderId, String customerId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return new ArrayList<>(items); }
    public BigDecimal getTotal() { return total; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public OrderStatus getStatus() { return status; }
    
    public void setTotal(BigDecimal total) { this.total = total; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public void addItem(OrderItem item) {
        this.items.add(item);
    }
}

class OrderItem {
    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal price;
    
    public OrderItem(String productId, String productName, int quantity, BigDecimal price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }
    
    // Getters
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
}

enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

// Single responsibility: Only validates orders
class OrderValidator {
    public void validate(Order order) {
        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order cannot be empty");
        }
        
        for (OrderItem item : order.getItems()) {
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be positive");
            }
            if (item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Item price must be positive");
            }
        }
    }
}

// Single responsibility: Only calculates order totals
class OrderCalculator {
    private TaxCalculator taxCalculator;
    private ShippingCalculator shippingCalculator;
    
    public OrderCalculator(TaxCalculator taxCalculator, ShippingCalculator shippingCalculator) {
        this.taxCalculator = taxCalculator;
        this.shippingCalculator = shippingCalculator;
    }
    
    public BigDecimal calculateTotal(Order order) {
        BigDecimal subtotal = order.getItems().stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal tax = taxCalculator.calculateTax(subtotal);
        BigDecimal shipping = shippingCalculator.calculateShipping(order);
        
        return subtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP);
    }
}

class TaxCalculator {
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08"); // 8%
    
    public BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}

class ShippingCalculator {
    private static final BigDecimal BASE_SHIPPING = new BigDecimal("5.99");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00");
    
    public BigDecimal calculateShipping(Order order) {
        BigDecimal subtotal = order.getItems().stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? 
            BigDecimal.ZERO : BASE_SHIPPING;
    }
}

// =============================================================================
// 2. OPEN/CLOSED PRINCIPLE (OCP)
// =============================================================================

// Abstract base class for payment processing
abstract class PaymentProcessor {
    public abstract boolean processPayment(BigDecimal amount, Map<String, String> paymentDetails);
    public abstract String getPaymentMethodName();
    
    // Template method - open for extension, closed for modification
    public final PaymentResult process(Order order, Map<String, String> paymentDetails) {
        try {
            boolean success = processPayment(order.getTotal(), paymentDetails);
            if (success) {
                return new PaymentResult(true, "Payment processed successfully via " + getPaymentMethodName());
            } else {
                return new PaymentResult(false, "Payment failed via " + getPaymentMethodName());
            }
        } catch (Exception e) {
            return new PaymentResult(false, "Payment error: " + e.getMessage());
        }
    }
}

class PaymentResult {
    private boolean success;
    private String message;
    
    public PaymentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}

// Concrete implementations - extending without modifying base class
class CreditCardPaymentProcessor extends PaymentProcessor {
    @Override
    public boolean processPayment(BigDecimal amount, Map<String, String> paymentDetails) {
        String cardNumber = paymentDetails.get("cardNumber");
        String expiryDate = paymentDetails.get("expiryDate");
        String cvv = paymentDetails.get("cvv");
        
        // Simulate credit card processing
        System.out.println("Processing credit card payment of $" + amount);
        System.out.println("Card: ****" + cardNumber.substring(cardNumber.length() - 4));
        
        // Simulate success/failure based on card number
        return !cardNumber.endsWith("0000");
    }
    
    @Override
    public String getPaymentMethodName() {
        return "Credit Card";
    }
}

class PayPalPaymentProcessor extends PaymentProcessor {
    @Override
    public boolean processPayment(BigDecimal amount, Map<String, String> paymentDetails) {
        String email = paymentDetails.get("email");
        String password = paymentDetails.get("password");
        
        System.out.println("Processing PayPal payment of $" + amount);
        System.out.println("PayPal account: " + email);
        
        // Simulate PayPal processing
        return email.contains("@");
    }
    
    @Override
    public String getPaymentMethodName() {
        return "PayPal";
    }
}

// New payment method can be added without modifying existing code
class CryptoPaymentProcessor extends PaymentProcessor {
    @Override
    public boolean processPayment(BigDecimal amount, Map<String, String> paymentDetails) {
        String walletAddress = paymentDetails.get("walletAddress");
        String cryptoType = paymentDetails.get("cryptoType");
        
        System.out.println("Processing " + cryptoType + " payment of $" + amount);
        System.out.println("Wallet: " + walletAddress);
        
        return walletAddress.length() > 10;
    }
    
    @Override
    public String getPaymentMethodName() {
        return "Cryptocurrency";
    }
}

// =============================================================================
// 3. LISKOV SUBSTITUTION PRINCIPLE (LSP)
// =============================================================================

// Base class defines contract
abstract class NotificationService {
    public abstract void sendNotification(String recipient, String subject, String message);
    
    // Precondition: recipient must not be null or empty
    // Postcondition: notification must be sent or exception thrown
    public final void notify(String recipient, String subject, String message) {
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient cannot be null or empty");
        }
        
        sendNotification(recipient, subject, message);
        logNotification(recipient, subject);
    }
    
    protected void logNotification(String recipient, String subject) {
        System.out.println("Notification logged: " + subject + " to " + recipient);
    }
}

// Correct implementations that honor the contract
class EmailNotificationService extends NotificationService {
    @Override
    public void sendNotification(String recipient, String subject, String message) {
        // Email-specific validation (stronger precondition is allowed)
        if (!recipient.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        
        System.out.println("📧 Email sent to: " + recipient);
        System.out.println("Subject: " + subject);
        System.out.println("Message: " + message);
    }
}

class SMSNotificationService extends NotificationService {
    @Override
    public void sendNotification(String recipient, String subject, String message) {
        // SMS-specific validation
        if (!recipient.matches("\\\\+?\\\\d{10,15}")) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        
        System.out.println("📱 SMS sent to: " + recipient);
        System.out.println("Message: " + subject + " - " + message);
    }
}

class PushNotificationService extends NotificationService {
    @Override
    public void sendNotification(String recipient, String subject, String message) {
        System.out.println("🔔 Push notification sent to device: " + recipient);
        System.out.println("Title: " + subject);
        System.out.println("Body: " + message);
    }
}

// =============================================================================
// 4. INTERFACE SEGREGATION PRINCIPLE (ISP)
// =============================================================================

// VIOLATION: Fat interface forces clients to depend on methods they don't use
interface WorkerBad {
    void work();
    void eat();
    void sleep();
    void code();
    void attendMeeting();
}

// CORRECT: Segregated interfaces
interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

interface Sleepable {
    void sleep();
}

interface Programmable {
    void code();
}

interface MeetingAttendable {
    void attendMeeting();
}

// Classes implement only the interfaces they need
class Developer implements Workable, Eatable, Sleepable, Programmable, MeetingAttendable {
    private String name;
    
    public Developer(String name) {
        this.name = name;
    }
    
    @Override
    public void work() {
        System.out.println(name + " is working on development tasks");
    }
    
    @Override
    public void eat() {
        System.out.println(name + " is eating lunch");
    }
    
    @Override
    public void sleep() {
        System.out.println(name + " is sleeping");
    }
    
    @Override
    public void code() {
        System.out.println(name + " is writing code");
    }
    
    @Override
    public void attendMeeting() {
        System.out.println(name + " is attending a meeting");
    }
}

class Robot implements Workable, Programmable {
    private String model;
    
    public Robot(String model) {
        this.model = model;
    }
    
    @Override
    public void work() {
        System.out.println("Robot " + model + " is performing automated tasks");
    }
    
    @Override
    public void code() {
        System.out.println("Robot " + model + " is generating code");
    }
    
    // Robot doesn't need to implement eat() or sleep()
}

// =============================================================================
// 5. DEPENDENCY INVERSION PRINCIPLE (DIP)
// =============================================================================

// High-level abstraction
interface OrderRepository {
    void save(Order order);
    Order findById(String orderId);
    List<Order> findByCustomerId(String customerId);
}

interface NotificationSender {
    void sendOrderConfirmation(Order order);
}

interface InventoryManager {
    void updateStock(String productId, int quantity);
    boolean checkAvailability(String productId, int quantity);
}

// Low-level implementations
class DatabaseOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) {
        System.out.println("💾 Saving order to database: " + order.getOrderId());
    }
    
    @Override
    public Order findById(String orderId) {
        System.out.println("🔍 Finding order in database: " + orderId);
        return null; // Simplified for demo
    }
    
    @Override
    public List<Order> findByCustomerId(String customerId) {
        System.out.println("🔍 Finding orders for customer: " + customerId);
        return new ArrayList<>(); // Simplified for demo
    }
}

class EmailNotificationSender implements NotificationSender {
    @Override
    public void sendOrderConfirmation(Order order) {
        System.out.println("📧 Sending email confirmation for order: " + order.getOrderId());
    }
}

class DatabaseInventoryManager implements InventoryManager {
    @Override
    public void updateStock(String productId, int quantity) {
        System.out.println("📦 Updating inventory for product " + productId + 
                          " by " + quantity + " units");
    }
    
    @Override
    public boolean checkAvailability(String productId, int quantity) {
        System.out.println("✅ Checking availability for product " + productId + 
                          ": " + quantity + " units");
        return true; // Simplified for demo
    }
}

// High-level module depends on abstractions, not concretions
class OrderService {
    private final OrderValidator validator;
    private final OrderCalculator calculator;
    private final OrderRepository repository;
    private final NotificationSender notificationSender;
    private final InventoryManager inventoryManager;
    private final PaymentProcessor paymentProcessor;
    
    // Dependency injection through constructor
    public OrderService(OrderValidator validator, 
                       OrderCalculator calculator,
                       OrderRepository repository,
                       NotificationSender notificationSender,
                       InventoryManager inventoryManager,
                       PaymentProcessor paymentProcessor) {
        this.validator = validator;
        this.calculator = calculator;
        this.repository = repository;
        this.notificationSender = notificationSender;
        this.inventoryManager = inventoryManager;
        this.paymentProcessor = paymentProcessor;
    }
    
    public OrderResult processOrder(Order order, Map<String, String> paymentDetails) {
        try {
            // Validate order (SRP)
            validator.validate(order);
            
            // Check inventory availability
            for (OrderItem item : order.getItems()) {
                if (!inventoryManager.checkAvailability(item.getProductId(), item.getQuantity())) {
                    return new OrderResult(false, "Insufficient inventory for " + item.getProductName());
                }
            }
            
            // Calculate total (SRP)
            BigDecimal total = calculator.calculateTotal(order);
            order.setTotal(total);
            
            // Process payment (OCP - can extend with new payment methods)
            PaymentResult paymentResult = paymentProcessor.process(order, paymentDetails);
            if (!paymentResult.isSuccess()) {
                return new OrderResult(false, "Payment failed: " + paymentResult.getMessage());
            }
            
            // Update order status
            order.setStatus(OrderStatus.CONFIRMED);
            
            // Save order (DIP)
            repository.save(order);
            
            // Update inventory
            for (OrderItem item : order.getItems()) {
                inventoryManager.updateStock(item.getProductId(), -item.getQuantity());
            }
            
            // Send notification (LSP - any notification service can be used)
            notificationSender.sendOrderConfirmation(order);
            
            return new OrderResult(true, "Order processed successfully: " + order.getOrderId());
            
        } catch (Exception e) {
            return new OrderResult(false, "Order processing failed: " + e.getMessage());
        }
    }
}

class OrderResult {
    private boolean success;
    private String message;
    
    public OrderResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}

// Factory for creating order service with proper dependencies (DIP)
class OrderServiceFactory {
    public static OrderService createOrderService() {
        // Create dependencies
        TaxCalculator taxCalculator = new TaxCalculator();
        ShippingCalculator shippingCalculator = new ShippingCalculator();
        OrderValidator validator = new OrderValidator();
        OrderCalculator calculator = new OrderCalculator(taxCalculator, shippingCalculator);
        OrderRepository repository = new DatabaseOrderRepository();
        NotificationSender notificationSender = new EmailNotificationSender();
        InventoryManager inventoryManager = new DatabaseInventoryManager();
        PaymentProcessor paymentProcessor = new CreditCardPaymentProcessor();
        
        // Inject dependencies
        return new OrderService(validator, calculator, repository, 
                              notificationSender, inventoryManager, paymentProcessor);
    }
    
    public static OrderService createOrderServiceWithPayPal() {
        TaxCalculator taxCalculator = new TaxCalculator();
        ShippingCalculator shippingCalculator = new ShippingCalculator();
        OrderValidator validator = new OrderValidator();
        OrderCalculator calculator = new OrderCalculator(taxCalculator, shippingCalculator);
        OrderRepository repository = new DatabaseOrderRepository();
        NotificationSender notificationSender = new EmailNotificationSender();
        InventoryManager inventoryManager = new DatabaseInventoryManager();
        PaymentProcessor paymentProcessor = new PayPalPaymentProcessor(); // Different implementation
        
        return new OrderService(validator, calculator, repository, 
                              notificationSender, inventoryManager, paymentProcessor);
    }
}

// Main demonstration class
public class SOLIDPrinciplesLLD {
    public static void main(String[] args) {
        System.out.println("🏛️  SOLID PRINCIPLES - Object Oriented Design Demo");
        System.out.println("==================================================\\n");
        
        // Test Single Responsibility Principle
        System.out.println("1️⃣  SINGLE RESPONSIBILITY PRINCIPLE");
        System.out.println("===================================");
        
        Order order = new Order("ORD-001", "CUST-123");
        order.addItem(new OrderItem("PROD-1", "Laptop", 1, new BigDecimal("999.99")));
        order.addItem(new OrderItem("PROD-2", "Mouse", 2, new BigDecimal("25.50")));
        
        OrderValidator validator = new OrderValidator();
        TaxCalculator taxCalculator = new TaxCalculator();
        ShippingCalculator shippingCalculator = new ShippingCalculator();
        OrderCalculator calculator = new OrderCalculator(taxCalculator, shippingCalculator);
        
        try {
            validator.validate(order);
            System.out.println("✅ Order validation passed");
            
            BigDecimal total = calculator.calculateTotal(order);
            order.setTotal(total);
            System.out.println("💰 Order total calculated: $" + total);
        } catch (Exception e) {
            System.out.println("❌ Order validation failed: " + e.getMessage());
        }
        
        // Test Open/Closed Principle
        System.out.println("\\n2️⃣  OPEN/CLOSED PRINCIPLE");
        System.out.println("=========================");
        
        List<PaymentProcessor> processors = Arrays.asList(
            new CreditCardPaymentProcessor(),
            new PayPalPaymentProcessor(),
            new CryptoPaymentProcessor()
        );
        
        for (PaymentProcessor processor : processors) {
            Map<String, String> paymentDetails = new HashMap<>();
            
            if (processor instanceof CreditCardPaymentProcessor) {
                paymentDetails.put("cardNumber", "1234567812345678");
                paymentDetails.put("expiryDate", "12/25");
                paymentDetails.put("cvv", "123");
            } else if (processor instanceof PayPalPaymentProcessor) {
                paymentDetails.put("email", "user@example.com");
                paymentDetails.put("password", "password123");
            } else if (processor instanceof CryptoPaymentProcessor) {
                paymentDetails.put("walletAddress", "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa");
                paymentDetails.put("cryptoType", "Bitcoin");
            }
            
            PaymentResult result = processor.process(order, paymentDetails);
            System.out.println("💳 " + result.getMessage());
        }
        
        // Test Liskov Substitution Principle
        System.out.println("\\n3️⃣  LISKOV SUBSTITUTION PRINCIPLE");
        System.out.println("=================================");
        
        List<NotificationService> notificationServices = Arrays.asList(
            new EmailNotificationService(),
            new SMSNotificationService(),
            new PushNotificationService()
        );
        
        String[] recipients = {"user@example.com", "+1234567890", "device-token-123"};
        
        for (int i = 0; i < notificationServices.size(); i++) {
            NotificationService service = notificationServices.get(i);
            try {
                service.notify(recipients[i], "Order Confirmation", 
                             "Your order " + order.getOrderId() + " has been confirmed!");
            } catch (Exception e) {
                System.out.println("❌ Notification failed: " + e.getMessage());
            }
            System.out.println();
        }
        
        // Test Interface Segregation Principle
        System.out.println("4️⃣  INTERFACE SEGREGATION PRINCIPLE");
        System.out.println("===================================");
        
        Developer developer = new Developer("Alice");
        Robot robot = new Robot("R2D2");
        
        System.out.println("--- Developer Activities ---");
        developer.work();
        developer.code();
        developer.attendMeeting();
        developer.eat();
        
        System.out.println("\\n--- Robot Activities ---");
        robot.work();
        robot.code();
        // Robot doesn't need to eat() or sleep()
        
        // Test Dependency Inversion Principle
        System.out.println("\\n5️⃣  DEPENDENCY INVERSION PRINCIPLE");
        System.out.println("==================================");
        
        System.out.println("\\n--- Processing order with Credit Card ---");
        OrderService orderService = OrderServiceFactory.createOrderService();
        
        Map<String, String> ccPaymentDetails = new HashMap<>();
        ccPaymentDetails.put("cardNumber", "1234567812345678");
        ccPaymentDetails.put("expiryDate", "12/25");
        ccPaymentDetails.put("cvv", "123");
        
        OrderResult result1 = orderService.processOrder(order, ccPaymentDetails);
        System.out.println("📋 " + result1.getMessage());
        
        System.out.println("\\n--- Processing order with PayPal ---");
        OrderService paypalOrderService = OrderServiceFactory.createOrderServiceWithPayPal();
        
        Order order2 = new Order("ORD-002", "CUST-456");
        order2.addItem(new OrderItem("PROD-3", "Keyboard", 1, new BigDecimal("79.99")));
        
        Map<String, String> paypalDetails = new HashMap<>();
        paypalDetails.put("email", "customer@example.com");
        paypalDetails.put("password", "paypal123");
        
        OrderResult result2 = paypalOrderService.processOrder(order2, paypalDetails);
        System.out.println("📋 " + result2.getMessage());
        
        System.out.println("\\n✅ SOLID Principles Demo Complete!");
        
        System.out.println("\\n📚 SOLID PRINCIPLES SUMMARY:");
        System.out.println("==============================================");
        System.out.println("🔸 Single Responsibility: Each class has one reason to change");
        System.out.println("🔸 Open/Closed: Open for extension, closed for modification");
        System.out.println("🔸 Liskov Substitution: Subtypes must be substitutable for base types");
        System.out.println("🔸 Interface Segregation: Clients shouldn't depend on unused interfaces");
        System.out.println("🔸 Dependency Inversion: Depend on abstractions, not concretions");
        
        System.out.println("\\n🎯 BENEFITS ACHIEVED:");
        System.out.println("• Higher code maintainability and readability");
        System.out.println("• Better testability through dependency injection");
        System.out.println("• Easier extension with new features");
        System.out.println("• Reduced coupling between components");
        System.out.println("• Improved code reusability and flexibility");
    }
}
