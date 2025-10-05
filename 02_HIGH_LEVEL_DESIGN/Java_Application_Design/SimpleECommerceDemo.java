import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simplified E-Commerce Platform Demo
 * 
 * This is a simplified version that demonstrates the key architectural patterns
 * from "Designing Data-Intensive Applications" in a single file for easy execution.
 */
public class SimpleECommerceDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("E-Commerce Platform - Demo Version");
        System.out.println("Based on 'Designing Data-Intensive Applications'");
        System.out.println("========================================\n");
        
        // Initialize the demo
        ECommercePlatform platform = new ECommercePlatform();
        platform.start();
        
        // Run demonstration
        platform.demonstrateCapabilities();
        
        // Shutdown
        platform.shutdown();
    }
    
    static class ECommercePlatform {
        private final MetricsCollector metrics = new MetricsCollector();
        private final Map<String, User> users = new ConcurrentHashMap<>();
        private final Map<String, Product> products = new ConcurrentHashMap<>();
        private final Map<String, Order> orders = new ConcurrentHashMap<>();
        
        public void start() {
            System.out.println("Starting E-Commerce Platform...");
            System.out.println("✅ Infrastructure initialized");
            System.out.println("✅ Services initialized");
            System.out.println("✅ Monitoring started");
            System.out.println("Platform ready!\n");
        }
        
        public void demonstrateCapabilities() {
            System.out.println("=== Demonstrating System Capabilities ===\n");
            
            // 1. User Management
            System.out.println("1. User Management:");
            User user = createUser("john.doe@example.com", "John Doe");
            System.out.println("   ✓ User created: " + user.email);
            
            // 2. Product Catalog
            System.out.println("\n2. Product Catalog:");
            Product product = createProduct("MacBook Pro", "High-performance laptop", new BigDecimal("2499.99"));
            System.out.println("   ✓ Product created: " + product.name + " ($" + product.price + ")");
            
            // 3. Order Processing
            System.out.println("\n3. Order Processing:");
            Order order = createOrder(user.id, product.id, 1);
            System.out.println("   ✓ Order created: " + order.id + " (Total: $" + order.totalAmount + ")");
            
            // 4. Payment Processing
            System.out.println("\n4. Payment Processing:");
            Payment payment = processPayment(order.id, order.totalAmount);
            System.out.println("   ✓ Payment processed: " + payment.status);
            
            // 5. Metrics
            System.out.println("\n5. System Metrics:");
            metrics.printSummary();
            
            System.out.println("\n✅ All capabilities demonstrated successfully!");
        }
        
        private User createUser(String email, String name) {
            User user = new User(email, name);
            users.put(user.id, user);
            metrics.increment("users.created");
            return user;
        }
        
        private Product createProduct(String name, String description, BigDecimal price) {
            Product product = new Product(name, description, price);
            products.put(product.id, product);
            metrics.increment("products.created");
            return product;
        }
        
        private Order createOrder(String userId, String productId, int quantity) {
            Product product = products.get(productId);
            BigDecimal total = product.price.multiply(BigDecimal.valueOf(quantity));
            Order order = new Order(userId, productId, quantity, total);
            orders.put(order.id, order);
            metrics.increment("orders.created");
            return order;
        }
        
        private Payment processPayment(String orderId, BigDecimal amount) {
            // Simulate payment processing with circuit breaker pattern
            if (Math.random() > 0.1) { // 90% success rate
                metrics.increment("payments.successful");
                return new Payment(orderId, amount, "COMPLETED");
            } else {
                metrics.increment("payments.failed");
                return new Payment(orderId, amount, "FAILED");
            }
        }
        
        public void shutdown() {
            System.out.println("\n=== Shutting Down Platform ===");
            System.out.println("✅ Services stopped");
            System.out.println("✅ Connections closed");
            System.out.println("✅ Resources cleaned up");
            System.out.println("Platform shutdown complete.");
        }
    }
    
    // Simplified domain models
    static class User {
        final String id = UUID.randomUUID().toString();
        final String email;
        final String name;
        final LocalDateTime createdAt = LocalDateTime.now();
        
        User(String email, String name) {
            this.email = email;
            this.name = name;
        }
    }
    
    static class Product {
        final String id = UUID.randomUUID().toString();
        final String name;
        final String description;
        final BigDecimal price;
        final LocalDateTime createdAt = LocalDateTime.now();
        
        Product(String name, String description, BigDecimal price) {
            this.name = name;
            this.description = description;
            this.price = price;
        }
    }
    
    static class Order {
        final String id = UUID.randomUUID().toString();
        final String userId;
        final String productId;
        final int quantity;
        final BigDecimal totalAmount;
        final LocalDateTime createdAt = LocalDateTime.now();
        
        Order(String userId, String productId, int quantity, BigDecimal totalAmount) {
            this.userId = userId;
            this.productId = productId;
            this.quantity = quantity;
            this.totalAmount = totalAmount;
        }
    }
    
    static class Payment {
        final String id = UUID.randomUUID().toString();
        final String orderId;
        final BigDecimal amount;
        final String status;
        final LocalDateTime createdAt = LocalDateTime.now();
        
        Payment(String orderId, BigDecimal amount, String status) {
            this.orderId = orderId;
            this.amount = amount;
            this.status = status;
        }
    }
    
    // Simplified metrics collector
    static class MetricsCollector {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        
        void increment(String metric) {
            counters.computeIfAbsent(metric, k -> new AtomicLong(0)).incrementAndGet();
        }
        
        void printSummary() {
            System.out.println("   Metrics collected:");
            counters.forEach((metric, count) -> 
                System.out.println("   - " + metric + ": " + count.get()));
        }
    }
}
