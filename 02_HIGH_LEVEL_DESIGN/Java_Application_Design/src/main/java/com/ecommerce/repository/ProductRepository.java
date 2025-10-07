package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product Repository Interface
 * 
 * Provides data access operations for Product entities.
 * Extends JpaRepository for CRUD operations and custom queries.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
    /**
     * Find products by category
     */
    List<Product> findByCategory(Product.ProductCategory category);
    
    /**
     * Find products by status
     */
    List<Product> findByStatus(Product.ProductStatus status);
    
    /**
     * Find active products with stock
     */
    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.stockQuantity > 0")
    List<Product> findAvailableProducts(@Param("status") Product.ProductStatus status);
    
    /**
     * Find products by name containing (case insensitive)
     */
    List<Product> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find products within price range
     */
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.status = 'ACTIVE'")
    List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                   @Param("maxPrice") BigDecimal maxPrice);
    
    /**
     * Find products by category and status
     */
    List<Product> findByCategoryAndStatus(Product.ProductCategory category, Product.ProductStatus status);
    
    /**
     * Find low stock products (stock quantity below threshold)
     */
    @Query("SELECT p FROM Product p WHERE p.stockQuantity < :threshold AND p.status = 'ACTIVE'")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);
    
    /**
     * Count products by category
     */
    long countByCategory(Product.ProductCategory category);
    
    /**
     * Check if product exists by name (for uniqueness validation)
     */
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * Find products ordered by name
     */
    List<Product> findAllByOrderByNameAsc();
    
    /**
     * Find products ordered by price
     */
    List<Product> findAllByOrderByPriceAsc();
    
    /**
     * Find products ordered by creation date (newest first)
     */
    List<Product> findAllByOrderByCreatedAtDesc();
}
