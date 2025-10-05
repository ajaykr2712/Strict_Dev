package com.ecommerce.service;

import com.ecommerce.domain.Order;
import com.ecommerce.domain.Product;
import com.ecommerce.domain.Payment;
import com.ecommerce.infrastructure.monitoring.MetricsCollector;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Order Service - Application Service Layer
 * 
 * Orchestrates order processing workflow with event sourcing and CQRS patterns.
 * Coordinates between product inventory, payment processing, and order fulfillment.
 */
public class OrderService {
    private final ProductService productService;
    private final PaymentService paymentService;
    private final MetricsCollector metricsCollector;
    private final Map<String, Order> orderDatabase;
    
    public OrderService(ProductService productService, PaymentService paymentService, 
                       MetricsCollector metricsCollector) {
        this.productService = productService;
        this.paymentService = paymentService;
        this.metricsCollector = metricsCollector;
        this.orderDatabase = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        System.out.println("OrderService initialized");
    }
    
    /**
     * Create a new order (simplified for demonstration)
     */
    public Order createOrder(String userId, String productId, int quantity) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate product availability
            Product product = productService.findProductById(productId);
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + productId);
            }
            
            if (!product.canFulfillOrder(quantity)) {
                throw new IllegalStateException("Insufficient stock for product: " + productId);
            }
            
            // Create order items (simplified - single item for demo)
            List<Object> orderItems = new ArrayList<>();
            // Note: In a real implementation, OrderItem would be properly imported
            // orderItems.add(new OrderItem(productId, product.getName(), product.getPrice(), quantity));
            
            // Create order with dummy shipping address
            Order order = new Order(userId, orderItems, "123 Main St, City, State");
            
            // Save order
            orderDatabase.put(order.getId(), order);
            
            // Reserve product stock
            productService.updateStock(productId, product.getStockQuantity() - quantity);
            
            // Record metrics
            metricsCollector.recordMetric("order.created", 1);
            metricsCollector.recordLatency("order.creation.latency", 
                                         System.currentTimeMillis() - startTime);
            
            System.out.println("Order created: " + order.getId() + 
                             " for user: " + userId + 
                             " (Product: " + product.getName() + ", Qty: " + quantity + ")");
            
            return order;
            
        } catch (Exception e) {
            metricsCollector.recordMetric("order.creation.error", 1);
            throw e;
        }
    }
    
    /**
     * Find order by ID
     */
    public Order findOrderById(String orderId) {
        return orderDatabase.get(orderId);
    }
    
    /**
     * Process order payment
     */
    public Order processOrderPayment(String orderId) {
        Order order = findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        
        if (!order.canBePaid()) {
            throw new IllegalStateException("Order cannot be paid: " + orderId);
        }
        
        // Process payment
        Payment payment = paymentService.processPayment(orderId, order.getTotalAmount());
        
        if (payment.isSuccessful()) {
            // Update order status
            Order paidOrder = order.markAsPaid();
            orderDatabase.put(orderId, paidOrder);
            
            metricsCollector.recordMetric("order.paid", 1);
            System.out.println("Order payment processed: " + orderId);
            
            return paidOrder;
        } else {
            throw new RuntimeException("Payment failed for order: " + orderId);
        }
    }
    
    /**
     * Confirm order
     */
    public Order confirmOrder(String orderId) {
        Order order = findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        
        Order confirmedOrder = order.confirm();
        orderDatabase.put(orderId, confirmedOrder);
        
        metricsCollector.recordMetric("order.confirmed", 1);
        System.out.println("Order confirmed: " + orderId);
        
        return confirmedOrder;
    }
    
    /**
     * Cancel order
     */
    public Order cancelOrder(String orderId) {
        Order order = findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        
        if (!order.canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled: " + orderId);
        }
        
        Order cancelledOrder = order.cancel();
        orderDatabase.put(orderId, cancelledOrder);
        
        metricsCollector.recordMetric("order.cancelled", 1);
        System.out.println("Order cancelled: " + orderId);
        
        return cancelledOrder;
    }
    
    /**
     * Publish order event for event sourcing
     */
    public void publishOrderEvent(Order order, String eventType) {
        // In a real implementation, this would publish to a message queue
        System.out.println("Order event published: " + eventType + 
                          " for order " + order.getId() + 
                          " at " + java.time.LocalDateTime.now());
        
        metricsCollector.recordMetric("order.event.published", 1);
        
        // Event would contain:
        // - Order ID
        // - Event type (ORDER_CREATED, ORDER_PAID, ORDER_SHIPPED, etc.)
        // - Timestamp
        // - Order state snapshot
        // - User ID
        // - Correlation ID for tracing
    }
}
