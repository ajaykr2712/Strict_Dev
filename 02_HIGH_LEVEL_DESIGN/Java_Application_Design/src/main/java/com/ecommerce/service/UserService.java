package com.ecommerce.service;

import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Service - Spring Service Layer
 * 
 * Handles user management operations with:
 * - Spring Data JPA integration
 * - Caching with Spring Cache abstraction
 * - Transaction management
 * - Business logic orchestration
 * 
 * Features:
 * - User registration and authentication
 * - Profile management
 * - Activity tracking
 * - Performance optimization with caching
 */
@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Register a new user
     * 
     * @param email User email address
     * @param name User full name
     * @return Created user entity
     * @throws IllegalArgumentException if user already exists
     */
    @Transactional
    public User registerUser(String email, String name) {
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }
        
        // Create and save new user
        User user = new User(email, name);
        User savedUser = userRepository.save(user);
        
        System.out.println("User registered successfully: " + savedUser.getEmail());
        return savedUser;
    }
    
    /**
     * Find user by ID with caching
     * 
     * @param userId User ID
     * @return User entity if found
     */
    @Cacheable(value = "users", key = "#userId")
    @Transactional(readOnly = true)
    public Optional<User> findUserById(UUID userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * Find user by email
     * 
     * @param email User email address
     * @return User entity if found
     */
    @Cacheable(value = "users", key = "#email")
    @Transactional(readOnly = true)
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Get all active users
     * 
     * @return List of active users
     */
    @Cacheable(value = "activeUsers")
    @Transactional(readOnly = true)
    public List<User> findAllActiveUsers() {
        return userRepository.findByStatus(User.UserStatus.ACTIVE);
    }
    
    /**
     * Update user login timestamp
     * 
     * @param userId User ID
     * @return Updated user entity
     */
    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public User updateLastLogin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.updateLastLogin();
        return userRepository.save(user);
    }
    
    /**
     * Deactivate user account
     * 
     * @param userId User ID
     * @return Updated user entity
     */
    @CacheEvict(value = {"users", "activeUsers"}, key = "#userId")
    @Transactional
    public User deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.deactivate();
        return userRepository.save(user);
    }
    
    /**
     * Get user count by status
     * 
     * @param status User status
     * @return Count of users with given status
     */
    @Transactional(readOnly = true)
    public long getUserCountByStatus(User.UserStatus status) {
        return userRepository.countByStatus(status);
    }
    
    /**
     * Find users with recent activity
     * 
     * @param since Date threshold for recent activity
     * @return List of users with recent login activity
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithRecentActivity(LocalDateTime since) {
        return userRepository.findUsersWithRecentActivity(since);
    }
    
    /**
     * Check if user can place orders
     * 
     * @param userId User ID
     * @return true if user can place orders
     */
    @Transactional(readOnly = true)
    public boolean canUserPlaceOrders(UUID userId) {
        return findUserById(userId)
                .map(User::canPlaceOrder)
                .orElse(false);
    }
}
