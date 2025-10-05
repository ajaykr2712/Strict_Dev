package com.ecommerce.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment Domain Entity
 * 
 * Represents a payment transaction with strong consistency guarantees.
 * Implements financial domain patterns for reliability and auditability.
 */
public class Payment {
    private final String id;
    private final String orderId;
    private final String userId;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private final PaymentStatus status;
    private final String transactionId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String failureReason;
    
    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, DIGITAL_WALLET
    }
    
    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED
    }
    
    // Constructor for new payment
    public Payment(String orderId, String userId, BigDecimal amount, PaymentMethod method) {
        this.id = UUID.randomUUID().toString();
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.method = Objects.requireNonNull(method, "Payment method cannot be null");
        this.status = PaymentStatus.PENDING;
        this.transactionId = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.failureReason = null;
        
        validateAmount(amount);
    }
    
    // Constructor for existing payment (from database)
    public Payment(String id, String orderId, String userId, BigDecimal amount,
                   PaymentMethod method, PaymentStatus status, String transactionId,
                   LocalDateTime createdAt, LocalDateTime updatedAt, String failureReason) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.method = Objects.requireNonNull(method, "Payment method cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.transactionId = transactionId;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated date cannot be null");
        this.failureReason = failureReason;
        
        validateAmount(amount);
    }
    
    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
    }
    
    // Immutable state transition methods
    public Payment startProcessing() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can start processing");
        }
        return new Payment(this.id, this.orderId, this.userId, this.amount,
                          this.method, PaymentStatus.PROCESSING, this.transactionId,
                          this.createdAt, LocalDateTime.now(), this.failureReason);
    }
    
    public Payment complete(String transactionId) {
        if (status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Only processing payments can be completed");
        }
        return new Payment(this.id, this.orderId, this.userId, this.amount,
                          this.method, PaymentStatus.COMPLETED, transactionId,
                          this.createdAt, LocalDateTime.now(), null);
    }
    
    public Payment fail(String reason) {
        if (status == PaymentStatus.COMPLETED || status == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Cannot fail completed or refunded payments");
        }
        return new Payment(this.id, this.orderId, this.userId, this.amount,
                          this.method, PaymentStatus.FAILED, this.transactionId,
                          this.createdAt, LocalDateTime.now(), reason);
    }
    
    public Payment refund() {
        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only completed payments can be refunded");
        }
        return new Payment(this.id, this.orderId, this.userId, this.amount,
                          this.method, PaymentStatus.REFUNDED, this.transactionId,
                          this.createdAt, LocalDateTime.now(), this.failureReason);
    }
    
    // Business logic methods
    public boolean isSuccessful() {
        return status == PaymentStatus.COMPLETED;
    }
    
    public boolean canBeRefunded() {
        return status == PaymentStatus.COMPLETED;
    }
    
    public boolean isInProgress() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }
    
    // Getters
    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionId() { return transactionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getFailureReason() { return failureReason; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Payment{" +
                "id='" + id + '\'' +
                ", orderId='" + orderId + '\'' +
                ", amount=" + amount +
                ", method=" + method +
                ", status=" + status +
                ", transactionId='" + transactionId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
