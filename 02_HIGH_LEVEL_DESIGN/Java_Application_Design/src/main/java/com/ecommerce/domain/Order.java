package com.ecommerce.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * Order Domain Entity - Aggregate Root
 * 
 * Represents an order in the e-commerce system following DDD principles.
 * This is an aggregate root that maintains consistency across order items.
 * 
 * Key Features:
 * - Event sourcing ready with state transitions
 * - Rich domain behavior for order processing
 * - Immutable design for data consistency
 */
public class Order {
    private final String id;
    private final String userId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final OrderStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String shippingAddress;
    
    public enum OrderStatus {
        PENDING, CONFIRMED, PAID, SHIPPED, DELIVERED, CANCELLED, REFUNDED
    }
    
    // Constructor for new order
    public Order(String userId, List<OrderItem> items, String shippingAddress) {
        this.id = UUID.randomUUID().toString();
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "Items cannot be null"));
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "Shipping address cannot be null");
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.totalAmount = calculateTotalAmount(items);
        
        validateOrder();
    }
    
    // Constructor for existing order (from database)
    public Order(String id, String userId, List<OrderItem> items, BigDecimal totalAmount,
                 OrderStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                 String shippingAddress) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "Items cannot be null"));
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated date cannot be null");
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "Shipping address cannot be null");
    }
    
    private void validateOrder() {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
    }
    
    private BigDecimal calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Immutable state transition methods
    public Order confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        return new Order(this.id, this.userId, this.items, this.totalAmount,
                        OrderStatus.CONFIRMED, this.createdAt, LocalDateTime.now(),
                        this.shippingAddress);
    }
    
    public Order markAsPaid() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be marked as paid");
        }
        return new Order(this.id, this.userId, this.items, this.totalAmount,
                        OrderStatus.PAID, this.createdAt, LocalDateTime.now(),
                        this.shippingAddress);
    }
    
    public Order ship() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("Only paid orders can be shipped");
        }
        return new Order(this.id, this.userId, this.items, this.totalAmount,
                        OrderStatus.SHIPPED, this.createdAt, LocalDateTime.now(),
                        this.shippingAddress);
    }
    
    public Order deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Only shipped orders can be delivered");
        }
        return new Order(this.id, this.userId, this.items, this.totalAmount,
                        OrderStatus.DELIVERED, this.createdAt, LocalDateTime.now(),
                        this.shippingAddress);
    }
    
    public Order cancel() {
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel delivered or already cancelled orders");
        }
        return new Order(this.id, this.userId, this.items, this.totalAmount,
                        OrderStatus.CANCELLED, this.createdAt, LocalDateTime.now(),
                        this.shippingAddress);
    }
    
    // Business logic methods
    public boolean canBePaid() {
        return status == OrderStatus.CONFIRMED;
    }
    
    public boolean canBeCancelled() {
        return status != OrderStatus.DELIVERED && status != OrderStatus.CANCELLED;
    }
    
    public boolean isCompleted() {
        return status == OrderStatus.DELIVERED;
    }
    
    public int getTotalItemCount() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
    
    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public List<OrderItem> getItems() { return new ArrayList<>(items); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getShippingAddress() { return shippingAddress; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                ", itemCount=" + items.size() +
                ", createdAt=" + createdAt +
                '}';
    }
}

/**
 * Order Item Value Object
 * 
 * Represents an item within an order with immutable design
 */
class OrderItem {
    private final String productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal totalPrice;
    
    public OrderItem(String productId, String productName, BigDecimal unitPrice, int quantity) {
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.productName = Objects.requireNonNull(productName, "Product name cannot be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price cannot be null");
        this.quantity = quantity;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
    }
    
    // Getters
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return quantity == orderItem.quantity &&
                Objects.equals(productId, orderItem.productId) &&
                Objects.equals(unitPrice, orderItem.unitPrice);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(productId, unitPrice, quantity);
    }
    
    @Override
    public String toString() {
        return "OrderItem{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", unitPrice=" + unitPrice +
                ", quantity=" + quantity +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
