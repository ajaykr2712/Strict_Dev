package com.ecommerce;

import com.ecommerce.config.DatabaseConfig;
import com.ecommerce.config.CacheConfig;
import com.ecommerce.config.MessageQueueConfig;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.UserService;
import com.ecommerce.service.PaymentService;
import com.ecommerce.infrastructure.monitoring.MetricsCollector;
import com.ecommerce.infrastructure.circuitbreaker.CircuitBreakerRegistry;

/**
 * Main Application Class for Data-Intensive E-Commerce Platform
 * 
 * This application demonstrates high-level system design principles from
 * "Designing Data-Intensive Applications" including:
 * 
 * 1. Reliability: Circuit breakers, retries, graceful degradation
 * 2. Scalability: Horizontal scaling, caching, async processing
 * 3. Maintainability: Clean architecture, dependency injection, monitoring
 * 
 * Architecture Overview:
 * - Domain-Driven Design (DDD) with clear bounded contexts
 * - CQRS (Command Query Responsibility Segregation) for read/write separation
 * - Event Sourcing for audit trails and state reconstruction
 * - Microservices-ready modular design
 * - Distributed caching and database sharding support
 * 
 * @author System Design Team
 * @version 1.0
 */
public class ECommerceApplication {
    
    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final MetricsCollector metricsCollector;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    /**
     * Constructor with dependency injection
     */
    public ECommerceApplication(
            ProductService productService,
            OrderService orderService,
            UserService userService,
            PaymentService paymentService,
            MetricsCollector metricsCollector,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        
        this.productService = productService;
        this.orderService = orderService;
        this.userService = userService;
        this.paymentService = paymentService;
        this.metricsCollector = metricsCollector;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }
    
    /**
     * Application startup with proper initialization sequence
     */
    public void start() {
        System.out.println("Starting E-Commerce Platform...");
        
        try {
            // Initialize infrastructure components
            initializeInfrastructure();
            
            // Initialize core services
            initializeServices();
            
            // Start monitoring
            startMonitoring();
            
            System.out.println("E-Commerce Platform started successfully!");
            System.out.println("System is ready to handle requests...");
            
            // Demonstrate key operations
            demonstrateSystemCapabilities();
            
        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            gracefulShutdown();
        }
    }
    
    /**
     * Initialize infrastructure components (databases, caches, message queues)
     */
    private void initializeInfrastructure() {
        System.out.println("Initializing infrastructure...");
        
        // Initialize database connections with connection pooling
        DatabaseConfig.initialize();
        
        // Initialize distributed cache (Redis cluster)
        CacheConfig.initialize();
        
        // Initialize message queue (Kafka/RabbitMQ)
        MessageQueueConfig.initialize();
        
        // Initialize circuit breakers
        circuitBreakerRegistry.initialize();
        
        System.out.println("Infrastructure initialized successfully");
    }
    
    /**
     * Initialize core business services
     */
    private void initializeServices() {
        System.out.println("Initializing core services...");
        
        userService.initialize();
        productService.initialize();
        orderService.initialize();
        paymentService.initialize();
        
        System.out.println("Core services initialized successfully");
    }
    
    /**
     * Start monitoring and metrics collection
     */
    private void startMonitoring() {
        System.out.println("Starting monitoring systems...");
        metricsCollector.start();
        System.out.println("Monitoring systems started");
    }
    
    /**
     * Demonstrate key system capabilities
     */
    private void demonstrateSystemCapabilities() {
        System.out.println("\n=== System Capabilities Demonstration ===");
        
        try {
            // User registration and authentication
            System.out.println("\n1. User Management:");
            var user = userService.registerUser("john.doe@example.com", "John Doe");
            System.out.println("User registered: " + user.getEmail());
            
            // Product catalog operations
            System.out.println("\n2. Product Catalog:");
            var product = productService.createProduct("MacBook Pro", "High-performance laptop", 2499.99);
            System.out.println("Product created: " + product.getName());
            
            var searchResults = productService.searchProducts("MacBook");
            System.out.println("Search results: " + searchResults.size() + " products found");
            
            // Order processing
            System.out.println("\n3. Order Processing:");
            var order = orderService.createOrder(user.getId(), product.getId(), 1);
            System.out.println("Order created: " + order.getId());
            
            // Payment processing
            System.out.println("\n4. Payment Processing:");
            var payment = paymentService.processPayment(order.getId(), order.getTotalAmount());
            System.out.println("Payment processed: " + payment.getStatus());
            
            // Event sourcing demonstration
            System.out.println("\n5. Event Sourcing:");
            orderService.publishOrderEvent(order, "ORDER_COMPLETED");
            System.out.println("Order event published for audit trail");
            
            // Metrics collection
            System.out.println("\n6. Metrics Collection:");
            metricsCollector.recordMetric("orders.created", 1);
            metricsCollector.recordMetric("payments.processed", 1);
            System.out.println("Metrics recorded for monitoring");
            
        } catch (Exception e) {
            System.err.println("Error during demonstration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Graceful shutdown with proper resource cleanup
     */
    public void gracefulShutdown() {
        System.out.println("Initiating graceful shutdown...");
        
        try {
            // Stop accepting new requests
            System.out.println("Stopping request acceptance...");
            
            // Complete pending operations
            System.out.println("Completing pending operations...");
            
            // Close database connections
            DatabaseConfig.shutdown();
            
            // Close cache connections
            CacheConfig.shutdown();
            
            // Close message queue connections
            MessageQueueConfig.shutdown();
            
            // Stop monitoring
            metricsCollector.stop();
            
            System.out.println("Graceful shutdown completed");
            
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Main method - Application entry point
     */
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("E-Commerce Platform - High Level Design");
        System.out.println("Based on 'Designing Data-Intensive Applications'");
        System.out.println("========================================\n");
        
        // Create application instance with dependency injection
        // In a real application, this would be handled by a DI container (Spring, Guice, etc.)
        ECommerceApplication app = createApplication();
        
        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(app::gracefulShutdown));
        
        // Start the application
        app.start();
        
        // Keep application running
        try {
            Thread.sleep(5000); // Simulate running application
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Factory method to create application with dependencies
     * In production, this would be replaced by a proper DI framework
     */
    private static ECommerceApplication createApplication() {
        // Create infrastructure components
        var metricsCollector = new MetricsCollector();
        var circuitBreakerRegistry = new CircuitBreakerRegistry();
        
        // Create services (simplified dependency injection)
        var userService = new UserService(metricsCollector);
        var productService = new ProductService(metricsCollector);
        var paymentService = new PaymentService(circuitBreakerRegistry, metricsCollector);
        var orderService = new OrderService(productService, paymentService, metricsCollector);
        
        return new ECommerceApplication(
            productService,
            orderService,
            userService,
            paymentService,
            metricsCollector,
            circuitBreakerRegistry
        );
    }
}
