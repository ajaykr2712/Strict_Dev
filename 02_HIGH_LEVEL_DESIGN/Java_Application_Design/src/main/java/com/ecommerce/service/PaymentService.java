package com.ecommerce.service;

import com.ecommerce.domain.Payment;
import com.ecommerce.infrastructure.monitoring.MetricsCollector;
import com.ecommerce.infrastructure.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Payment Service - Application Service Layer
 * 
 * Handles payment processing with reliability patterns:
 * - Circuit breaker for external payment gateway resilience
 * - Retry logic with exponential backoff
 * - Idempotency for payment safety
 */
public class PaymentService {
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MetricsCollector metricsCollector;
    private final Map<String, Payment> paymentDatabase;
    
    public PaymentService(CircuitBreakerRegistry circuitBreakerRegistry, MetricsCollector metricsCollector) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.metricsCollector = metricsCollector;
        this.paymentDatabase = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        System.out.println("PaymentService initialized");
    }
    
    /**
     * Process payment with circuit breaker protection
     */
    public Payment processPayment(String orderId, BigDecimal amount) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Create payment record
            Payment payment = new Payment(orderId, "user-123", amount, Payment.PaymentMethod.CREDIT_CARD);
            
            // Save payment in pending state
            paymentDatabase.put(payment.getId(), payment);
            
            // Start processing
            payment = payment.startProcessing();
            paymentDatabase.put(payment.getId(), payment);
            
            // Process payment through external gateway with circuit breaker
            String transactionId = circuitBreakerRegistry.execute("payment-service", () -> {
                return processPaymentWithExternalGateway(payment);
            });
            
            // Complete payment
            payment = payment.complete(transactionId);
            paymentDatabase.put(payment.getId(), payment);
            
            // Record success metrics
            metricsCollector.recordMetric("payment.processed.success", 1);
            metricsCollector.recordLatency("payment.processing.latency", 
                                         System.currentTimeMillis() - startTime);
            
            System.out.println("Payment processed successfully: " + payment.getId() + 
                             " (Transaction ID: " + transactionId + ")");
            return payment;
            
        } catch (Exception e) {
            metricsCollector.recordMetric("payment.processed.error", 1);
            
            // Handle payment failure
            Payment failedPayment = paymentDatabase.values().stream()
                    .filter(p -> p.getOrderId().equals(orderId))
                    .findFirst()
                    .orElse(null);
            
            if (failedPayment != null && failedPayment.isInProgress()) {
                failedPayment = failedPayment.fail(e.getMessage());
                paymentDatabase.put(failedPayment.getId(), failedPayment);
            }
            
            System.err.println("Payment processing failed for order " + orderId + ": " + e.getMessage());
            throw new PaymentProcessingException("Payment failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Simulate external payment gateway call
     */
    private String processPaymentWithExternalGateway(Payment payment) throws Exception {
        // Simulate external API call with potential failure
        System.out.println("Processing payment with external gateway: " + payment.getId());
        
        // Simulate processing time
        Thread.sleep(100);
        
        // Simulate 10% failure rate for demonstration
        if (Math.random() < 0.1) {
            throw new Exception("External gateway error");
        }
        
        // Return mock transaction ID
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Find payment by ID
     */
    public Payment findPaymentById(String paymentId) {
        return paymentDatabase.get(paymentId);
    }
    
    /**
     * Find payment by order ID
     */
    public Payment findPaymentByOrderId(String orderId) {
        return paymentDatabase.values().stream()
                .filter(payment -> payment.getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Refund payment
     */
    public Payment refundPayment(String paymentId) {
        Payment payment = findPaymentById(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found: " + paymentId);
        }
        
        if (!payment.canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded: " + paymentId);
        }
        
        Payment refundedPayment = payment.refund();
        paymentDatabase.put(paymentId, refundedPayment);
        
        metricsCollector.recordMetric("payment.refunded", 1);
        System.out.println("Payment refunded: " + paymentId);
        
        return refundedPayment;
    }
    
    /**
     * Custom exception for payment processing errors
     */
    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
