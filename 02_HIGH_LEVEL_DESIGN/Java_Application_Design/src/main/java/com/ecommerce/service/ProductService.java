package com.ecommerce.service;

import com.ecommerce.domain.Product;
import com.ecommerce.infrastructure.monitoring.MetricsCollector;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Product Service - Application Service Layer
 * 
 * Manages product catalog operations with search and caching capabilities.
 * Implements catalog management patterns for e-commerce systems.
 */
public class ProductService {
    private final MetricsCollector metricsCollector;
    private final Map<String, Product> productDatabase;
    private final Map<String, Product> productCache;
    
    public ProductService(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.productDatabase = new ConcurrentHashMap<>();
        this.productCache = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        System.out.println("ProductService initialized");
        
        // Pre-populate with some sample products
        createSampleProducts();
    }
    
    private void createSampleProducts() {
        // Create sample products for demonstration
        createProduct("iPhone 15", "Latest smartphone from Apple", new BigDecimal("999.99"));
        createProduct("MacBook Pro", "High-performance laptop", new BigDecimal("2499.99"));
        createProduct("AirPods Pro", "Wireless noise-cancelling earbuds", new BigDecimal("249.99"));
    }
    
    /**
     * Create a new product
     */
    public Product createProduct(String name, String description, BigDecimal price) {
        long startTime = System.currentTimeMillis();
        
        try {
            Product product = new Product(name, description, price);
            
            // Save to database
            productDatabase.put(product.getId(), product);
            
            // Update cache
            productCache.put(product.getId(), product);
            
            // Record metrics
            metricsCollector.recordMetric("product.created", 1);
            metricsCollector.recordLatency("product.creation.latency", 
                                         System.currentTimeMillis() - startTime);
            
            System.out.println("Product created: " + product.getName() + " (ID: " + product.getId() + ")");
            return product;
            
        } catch (Exception e) {
            metricsCollector.recordMetric("product.creation.error", 1);
            throw e;
        }
    }
    
    /**
     * Find product by ID with caching
     */
    public Product findProductById(String productId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Try cache first
            Product product = productCache.get(productId);
            if (product != null) {
                metricsCollector.recordMetric("product.cache.hit", 1);
                return product;
            }
            
            // Cache miss - load from database
            metricsCollector.recordMetric("product.cache.miss", 1);
            product = productDatabase.get(productId);
            
            if (product != null) {
                // Update cache
                productCache.put(productId, product);
            }
            
            metricsCollector.recordLatency("product.lookup.latency", 
                                         System.currentTimeMillis() - startTime);
            return product;
            
        } catch (Exception e) {
            metricsCollector.recordMetric("product.lookup.error", 1);
            throw e;
        }
    }
    
    /**
     * Search products by name (simplified search implementation)
     */
    public List<Product> searchProducts(String searchTerm) {
        long startTime = System.currentTimeMillis();
        
        try {
            String lowerSearchTerm = searchTerm.toLowerCase();
            
            List<Product> results = productDatabase.values().stream()
                    .filter(product -> product.getName().toLowerCase().contains(lowerSearchTerm) ||
                                     product.getDescription().toLowerCase().contains(lowerSearchTerm))
                    .filter(Product::isAvailable)
                    .collect(Collectors.toList());
            
            // Record metrics
            metricsCollector.recordMetric("product.search.performed", 1);
            metricsCollector.recordMetric("product.search.results", results.size());
            metricsCollector.recordLatency("product.search.latency", 
                                         System.currentTimeMillis() - startTime);
            
            System.out.println("Product search for '" + searchTerm + "' returned " + results.size() + " results");
            return results;
            
        } catch (Exception e) {
            metricsCollector.recordMetric("product.search.error", 1);
            throw e;
        }
    }
    
    /**
     * Update product stock
     */
    public Product updateStock(String productId, int newStock) {
        Product product = findProductById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
        
        Product updatedProduct = product.withStock(newStock);
        
        // Update database and cache
        productDatabase.put(productId, updatedProduct);
        productCache.put(productId, updatedProduct);
        
        metricsCollector.recordMetric("product.stock.updated", 1);
        return updatedProduct;
    }
    
    /**
     * Update product price
     */
    public Product updatePrice(String productId, BigDecimal newPrice) {
        Product product = findProductById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
        
        Product updatedProduct = product.withPrice(newPrice);
        
        // Update database and cache
        productDatabase.put(productId, updatedProduct);
        productCache.put(productId, updatedProduct);
        
        metricsCollector.recordMetric("product.price.updated", 1);
        return updatedProduct;
    }
    
    /**
     * Get all available products
     */
    public List<Product> getAllAvailableProducts() {
        return productDatabase.values().stream()
                .filter(Product::isAvailable)
                .collect(Collectors.toList());
    }
}
