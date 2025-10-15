package com.ecommerce.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Product Domain Entity
 *
 * Represents a product in the e-commerce catalog with rich domain behavior.
 * Implements principles from "Designing Data-Intensive Applications":
 * - Data consistency through validation
 * - Immutable design for reliability
 * - Rich domain model
 */
@Deprecated // Prefer using persistence entity com.ecommerce.entity.Product in application layers
public class Product {
    private final String id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final ProductCategory category;
    private final ProductStatus status;
    private final int stockQuantity;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public enum ProductCategory {
        ELECTRONICS, CLOTHING, BOOKS, HOME, SPORTS, BEAUTY, OTHER
    }

    public enum ProductStatus {
        ACTIVE, INACTIVE, OUT_OF_STOCK, DISCONTINUED
    }

    // Constructor for new product creation
    public Product(String name, String description, BigDecimal price) {
        this.id = UUID.randomUUID().toString();
        this.name = Objects.requireNonNull(name, "Product name cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.category = ProductCategory.OTHER;
        this.status = ProductStatus.ACTIVE;
        this.stockQuantity = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        validatePrice(price);
    }

    // Constructor for existing product (from database)
    public Product(String id, String name, String description, BigDecimal price,
                   ProductCategory category, ProductStatus status, int stockQuantity,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Product name cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.stockQuantity = Math.max(0, stockQuantity);
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated date cannot be null");
        validatePrice(price);
    }

    private void validatePrice(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    // Immutable update methods
    public Product withPrice(BigDecimal newPrice) {
        validatePrice(newPrice);
        return new Product(this.id, this.name, this.description, newPrice,
                this.category, this.status, this.stockQuantity,
                this.createdAt, LocalDateTime.now());
    }

    public Product withStock(int newStock) {
        return new Product(this.id, this.name, this.description, this.price,
                this.category, this.status, Math.max(0, newStock),
                this.createdAt, LocalDateTime.now());
    }

    public Product withStatus(ProductStatus newStatus) {
        return new Product(this.id, this.name, this.description, this.price,
                this.category, newStatus, this.stockQuantity,
                this.createdAt, LocalDateTime.now());
    }

    public Product withCategory(ProductCategory newCategory) {
        return new Product(this.id, this.name, this.description, this.price,
                newCategory, this.status, this.stockQuantity,
                this.createdAt, LocalDateTime.now());
    }

    // Business logic methods
    public boolean isAvailable() {
        return status == ProductStatus.ACTIVE && stockQuantity > 0;
    }

    public boolean canFulfillOrder(int requestedQuantity) {
        return isAvailable() && stockQuantity >= requestedQuantity;
    }

    public Product reserveStock(int quantity) {
        if (!canFulfillOrder(quantity)) {
            throw new IllegalStateException("Insufficient stock available");
        }
        return withStock(stockQuantity - quantity);
    }

    public BigDecimal calculateTotalPrice(int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public ProductCategory getCategory() { return category; }
    public ProductStatus getStatus() { return status; }
    public int getStockQuantity() { return stockQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category=" + category +
                ", status=" + status +
                ", stockQuantity=" + stockQuantity +
                '}';
    }
}
