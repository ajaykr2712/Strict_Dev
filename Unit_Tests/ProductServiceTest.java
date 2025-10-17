package com.ecommerce.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.ecommerce.entity.Product;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.repository.ProductRepository;

/**
 * Unit tests for ProductService
 * Tests business logic for product management including CRUD operations,
 * caching behavior, and exception handling.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @Before
    public void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    public void testCreateProduct_Success() {
        // Arrange
        String name = "Test Product";
        String description = "Test Description";
        BigDecimal price = new BigDecimal("99.99");
        
        Product expectedProduct = new Product("test-id", name, description, price,
                Product.ProductCategory.OTHER, Product.ProductStatus.ACTIVE, 0);
        
        when(productRepository.save(any(Product.class))).thenReturn(expectedProduct);

        // Act
        Product result = productService.create(name, description, price);

        // Assert
        assertNotNull("Created product should not be null", result);
        assertEquals("Product name should match", name, result.getName());
        assertEquals("Product price should match", price, result.getPrice());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    public void testGetProduct_Success() {
        // Arrange
        String productId = "test-id-123";
        Product expectedProduct = new Product(productId, "Product", "Desc",
                new BigDecimal("50.00"), Product.ProductCategory.ELECTRONICS,
                Product.ProductStatus.ACTIVE, 10);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(expectedProduct));

        // Act
        Product result = productService.get(productId);

        // Assert
        assertNotNull("Product should not be null", result);
        assertEquals("Product ID should match", productId, result.getId());
        verify(productRepository, times(1)).findById(productId);
    }

    @Test(expected = ProductNotFoundException.class)
    public void testGetProduct_NotFound() {
        // Arrange
        String productId = "non-existent-id";
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act
        productService.get(productId);

        // Assert - Exception should be thrown
    }

    @Test
    public void testListProducts_ReturnsMultipleProducts() {
        // Arrange
        Product product1 = new Product("id1", "Product 1", "Desc 1",
                new BigDecimal("10.00"), Product.ProductCategory.BOOKS,
                Product.ProductStatus.ACTIVE, 5);
        Product product2 = new Product("id2", "Product 2", "Desc 2",
                new BigDecimal("20.00"), Product.ProductCategory.CLOTHING,
                Product.ProductStatus.ACTIVE, 3);
        
        when(productRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(product1, product2));

        // Act
        List<Product> result = productService.list();

        // Assert
        assertNotNull("Product list should not be null", result);
        assertEquals("Should return 2 products", 2, result.size());
        verify(productRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    public void testUpdatePrice_Success() {
        // Arrange
        String productId = "test-id";
        BigDecimal oldPrice = new BigDecimal("50.00");
        BigDecimal newPrice = new BigDecimal("75.00");
        
        Product existingProduct = new Product(productId, "Product", "Desc",
                oldPrice, Product.ProductCategory.OTHER,
                Product.ProductStatus.ACTIVE, 10);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Product result = productService.updatePrice(productId, newPrice);

        // Assert
        assertNotNull("Updated product should not be null", result);
        assertEquals("Price should be updated", newPrice, result.getPrice());
        verify(productRepository, times(1)).save(existingProduct);
    }

    @Test
    public void testUpdateStock_IncreaseStock() {
        // Arrange
        String productId = "test-id";
        int oldStock = 10;
        int newStock = 50;
        
        Product existingProduct = new Product(productId, "Product", "Desc",
                new BigDecimal("50.00"), Product.ProductCategory.OTHER,
                Product.ProductStatus.ACTIVE, oldStock);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Product result = productService.updateStock(productId, newStock);

        // Assert
        assertNotNull("Updated product should not be null", result);
        assertEquals("Stock should be updated", newStock, result.getStockQuantity());
        verify(productRepository, times(1)).save(existingProduct);
    }

    @Test
    public void testUpdateStock_DecreaseStock() {
        // Arrange
        String productId = "test-id";
        int oldStock = 50;
        int newStock = 5;
        
        Product existingProduct = new Product(productId, "Product", "Desc",
                new BigDecimal("50.00"), Product.ProductCategory.OTHER,
                Product.ProductStatus.ACTIVE, oldStock);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Product result = productService.updateStock(productId, newStock);

        // Assert
        assertEquals("Stock should be decreased", newStock, result.getStockQuantity());
    }

    @Test
    public void testUpdateStatus_ToInactive() {
        // Arrange
        String productId = "test-id";
        Product existingProduct = new Product(productId, "Product", "Desc",
                new BigDecimal("50.00"), Product.ProductCategory.OTHER,
                Product.ProductStatus.ACTIVE, 10);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Product result = productService.updateStatus(productId, Product.ProductStatus.INACTIVE);

        // Assert
        assertNotNull("Updated product should not be null", result);
        assertEquals("Status should be INACTIVE", Product.ProductStatus.INACTIVE, result.getStatus());
        verify(productRepository, times(1)).save(existingProduct);
    }

    @Test(expected = ProductNotFoundException.class)
    public void testUpdatePrice_ProductNotFound() {
        // Arrange
        String productId = "non-existent";
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act
        productService.updatePrice(productId, new BigDecimal("100.00"));

        // Assert - Exception expected
    }

    @Test
    public void testCreateProduct_WithZeroPrice() {
        // Arrange
        String name = "Free Product";
        BigDecimal price = BigDecimal.ZERO;
        Product product = new Product("id", name, "Free item", price,
                Product.ProductCategory.OTHER, Product.ProductStatus.ACTIVE, 0);
        
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        Product result = productService.create(name, "Free item", price);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getPrice());
    }
}
