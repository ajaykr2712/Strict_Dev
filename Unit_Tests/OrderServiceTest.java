package com.ecommerce.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.ecommerce.domain.Order;
import com.ecommerce.domain.Product;
import com.ecommerce.infrastructure.monitoring.MetricsCollector;

/**
 * Unit tests for OrderService
 * Tests order creation, payment processing, and fulfillment workflows
 */
@RunWith(MockitoJUnitRunner.class)
public class OrderServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private MetricsCollector metricsCollector;

    private OrderService orderService;

    @Before
    public void setUp() {
        orderService = new OrderService(productService, paymentService, metricsCollector);
        orderService.initialize();
    }

    @Test
    public void testCreateOrder_Success() {
        // Arrange
        String userId = "user-123";
        String productId = "product-456";
        int quantity = 2;
        
        Product mockProduct = mock(Product.class);
        when(mockProduct.getName()).thenReturn("Test Product");
        when(mockProduct.getPrice()).thenReturn(new java.math.BigDecimal("50.00"));
        when(mockProduct.canFulfillOrder(quantity)).thenReturn(true);
        when(mockProduct.getStockQuantity()).thenReturn(10);
        
        when(productService.findProductById(productId)).thenReturn(mockProduct);

        // Act
        Order result = orderService.createOrder(userId, productId, quantity);

        // Assert
        assertNotNull("Order should not be null", result);
        assertEquals("Order user ID should match", userId, result.getUserId());
        verify(productService, times(1)).findProductById(productId);
        verify(productService, times(1)).updateStock(eq(productId), eq(8));
        verify(metricsCollector, times(1)).recordMetric("order.created", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOrder_ProductNotFound() {
        // Arrange
        String userId = "user-123";
        String productId = "non-existent";
        int quantity = 1;
        
        when(productService.findProductById(productId)).thenReturn(null);

        // Act
        orderService.createOrder(userId, productId, quantity);

        // Assert - Exception expected
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateOrder_InsufficientStock() {
        // Arrange
        String userId = "user-123";
        String productId = "product-456";
        int quantity = 100;
        
        Product mockProduct = mock(Product.class);
        when(mockProduct.canFulfillOrder(quantity)).thenReturn(false);
        when(productService.findProductById(productId)).thenReturn(mockProduct);

        // Act
        orderService.createOrder(userId, productId, quantity);

        // Assert - Exception expected
    }

    @Test
    public void testCreateOrder_MetricsRecorded() {
        // Arrange
        String userId = "user-123";
        String productId = "product-456";
        int quantity = 1;
        
        Product mockProduct = mock(Product.class);
        when(mockProduct.canFulfillOrder(quantity)).thenReturn(true);
        when(mockProduct.getStockQuantity()).thenReturn(10);
        when(productService.findProductById(productId)).thenReturn(mockProduct);

        // Act
        orderService.createOrder(userId, productId, quantity);

        // Assert
        verify(metricsCollector, times(1)).recordMetric(eq("order.created"), eq(1));
        verify(metricsCollector, times(1)).recordLatency(eq("order.creation.latency"), anyLong());
    }

    @Test
    public void testFindOrderById_Success() {
        // Arrange
        String userId = "user-123";
        String productId = "product-456";
        
        Product mockProduct = mock(Product.class);
        when(mockProduct.canFulfillOrder(1)).thenReturn(true);
        when(mockProduct.getStockQuantity()).thenReturn(10);
        when(productService.findProductById(productId)).thenReturn(mockProduct);
        
        Order createdOrder = orderService.createOrder(userId, productId, 1);

        // Act
        Order foundOrder = orderService.findOrderById(createdOrder.getId());

        // Assert
        assertNotNull("Order should be found", foundOrder);
        assertEquals("Order ID should match", createdOrder.getId(), foundOrder.getId());
    }

    @Test
    public void testFindOrderById_NotFound() {
        // Act
        Order result = orderService.findOrderById("non-existent-id");

        // Assert
        assertNull("Order should not be found", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessOrderPayment_OrderNotFound() {
        // Act
        orderService.processOrderPayment("non-existent-id");

        // Assert - Exception expected
    }

    @Test
    public void testCreateOrder_RecordsErrorMetricOnFailure() {
        // Arrange
        String userId = "user-123";
        String productId = "product-456";
        
        when(productService.findProductById(productId))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        try {
            orderService.createOrder(userId, productId, 1);
            fail("Expected exception was not thrown");
        } catch (RuntimeException e) {
            // Expected
        }

        // Assert
        verify(metricsCollector, times(1)).recordMetric("order.creation.error", 1);
    }
}
