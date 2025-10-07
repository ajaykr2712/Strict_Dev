package com.ecommerce.dto;

import java.math.BigDecimal;

/**
 * Product Creation Request DTO
 * 
 * Data Transfer Object for creating new products.
 */
public class ProductCreateRequest {
    
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stockQuantity;
    
    // Default constructor
    public ProductCreateRequest() {}
    
    // Constructor
    public ProductCreateRequest(String name, String description, BigDecimal price, 
                               String category, Integer stockQuantity) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
}
