package com.ecommerce.controller;

import com.ecommerce.service.ProductService;
import java.math.BigDecimal;

/**
 * Product Controller
 * 
 * REST API endpoints for product management.
 * Simplified version for current project structure.
 */
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    /**
     * Create a new product
     */
    public String createProduct(String name, String description, BigDecimal price) {
        return productService.createProduct(name, description, price);
    }
    
    /**
     * Get product by ID
     */
    public String getProductById(String id) {
        return productService.findProductById(id);
    }
    
    /**
     * Update product stock
     */
    public void updateStock(String id, int stock) {
        productService.updateStock(id, stock);
    }
    
    /**
     * Search products
     */
    public void searchProducts(String searchTerm) {
        productService.searchProducts(searchTerm);
    }
}
