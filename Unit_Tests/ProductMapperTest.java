package com.ecommerce.mapper;

import static org.junit.Assert.*;
import org.junit.Test;

import com.ecommerce.dto.ProductDTO;
import com.ecommerce.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unit tests for ProductMapper
 * Tests DTO mapping and null handling
 */
public class ProductMapperTest {

    @Test
    public void testToDto_ValidProduct_MapsCorrectly() {
        // Arrange
        String id = "prod-123";
        String name = "Test Product";
        String description = "Test Description";
        BigDecimal price = new BigDecimal("99.99");
        LocalDateTime now = LocalDateTime.now();
        
        Product product = new Product(id, name, description, price,
                Product.ProductCategory.ELECTRONICS,
                Product.ProductStatus.ACTIVE, 10);

        // Act
        ProductDTO result = ProductMapper.toDto(product);

        // Assert
        assertNotNull("DTO should not be null", result);
        assertEquals("ID should match", id, result.getId());
        assertEquals("Name should match", name, result.getName());
        assertEquals("Description should match", description, result.getDescription());
        assertEquals("Price should match", price, result.getPrice());
        assertEquals("Category should match", "ELECTRONICS", result.getCategory());
        assertEquals("Status should match", "ACTIVE", result.getStatus());
        assertEquals("Stock should match", 10, result.getStockQuantity());
    }

    @Test
    public void testToDto_NullProduct_ReturnsNull() {
        // Act
        ProductDTO result = ProductMapper.toDto(null);

        // Assert
        assertNull("DTO should be null when input is null", result);
    }

    @Test
    public void testToDto_WithDifferentCategory() {
        // Arrange
        Product product = new Product("id", "Name", "Desc",
                new BigDecimal("10.00"), Product.ProductCategory.BOOKS,
                Product.ProductStatus.ACTIVE, 5);

        // Act
        ProductDTO result = ProductMapper.toDto(product);

        // Assert
        assertEquals("Category should be BOOKS", "BOOKS", result.getCategory());
    }

    @Test
    public void testToDto_WithInactiveStatus() {
        // Arrange
        Product product = new Product("id", "Name", "Desc",
                new BigDecimal("10.00"), Product.ProductCategory.OTHER,
                Product.ProductStatus.INACTIVE, 0);

        // Act
        ProductDTO result = ProductMapper.toDto(product);

        // Assert
        assertEquals("Status should be INACTIVE", "INACTIVE", result.getStatus());
    }

    @Test
    public void testToDto_WithZeroStock() {
        // Arrange
        Product product = new Product("id", "Name", "Desc",
                new BigDecimal("10.00"), Product.ProductCategory.OTHER,
                Product.ProductStatus.ACTIVE, 0);

        // Act
        ProductDTO result = ProductMapper.toDto(product);

        // Assert
        assertEquals("Stock should be 0", 0, result.getStockQuantity());
    }

    @Test
    public void testToDto_PreservesTimestamps() {
        // Arrange
        Product product = new Product("id", "Name", "Desc",
                new BigDecimal("10.00"), Product.ProductCategory.OTHER,
                Product.ProductStatus.ACTIVE, 5);

        // Act
        ProductDTO result = ProductMapper.toDto(product);

        // Assert
        assertNotNull("Created timestamp should not be null", result.getCreatedAt());
        assertNotNull("Updated timestamp should not be null", result.getUpdatedAt());
    }
}
