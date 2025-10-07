package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import java.util.List;

/**
 * Order Repository Interface
 * 
 * Provides data access operations for Order entities.
 * Simplified version for current project structure.
 */
public interface OrderRepository {
    
    /**
     * Save an order
     */
    Order save(Order order);
    
    /**
     * Find order by ID
     */
    Order findById(String id);
    
    /**
     * Find orders by user ID
     */
    List<Order> findByUserId(String userId);
    
    /**
     * Find orders by status
     */
    List<Order> findByStatus(Order.OrderStatus status);
    
    /**
     * Find all orders
     */
    List<Order> findAll();
    
    /**
     * Delete an order
     */
    void delete(Order order);
    
    /**
     * Check if order exists
     */
    boolean existsById(String id);
}
