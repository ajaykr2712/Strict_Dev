package com.ecommerce.controller;

import com.ecommerce.dto.UserRegistrationRequest;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Controller - REST API Controller
 * 
 * Provides REST endpoints for user management operations:
 * - User registration and profile management
 * - User lookup and search operations
 * - User activity tracking
 * - Admin operations for user management
 * 
 * Features:
 * - RESTful API design with proper HTTP status codes
 * - Request/Response DTOs for API contract
 * - Input validation with Bean Validation
 * - OpenAPI documentation with Swagger annotations
 * - Error handling and exception mapping
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "APIs for managing users in the e-commerce platform")
public class UserController {
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Register a new user
     * 
     * @param request User registration request
     * @return Created user response
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", 
               description = "Creates a new user account with email and name")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        
        User user = userService.registerUser(request.getEmail(), request.getName());
        UserResponse response = convertToResponse(user);
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    /**
     * Get user by ID
     * 
     * @param userId User ID
     * @return User response or 404 if not found
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", 
               description = "Retrieves user information by user ID")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        return userService.findUserById(userId)
                .map(user -> ResponseEntity.ok(convertToResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get user by email
     * 
     * @param email User email address
     * @return User response or 404 if not found
     */
    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", 
               description = "Retrieves user information by email address")
    public ResponseEntity<UserResponse> getUserByEmail(
            @Parameter(description = "User email address") @PathVariable String email) {
        
        return userService.findUserByEmail(email)
                .map(user -> ResponseEntity.ok(convertToResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all active users
     * 
     * @return List of active users
     */
    @GetMapping("/active")
    @Operation(summary = "Get all active users", 
               description = "Retrieves all users with ACTIVE status")
    public ResponseEntity<List<UserResponse>> getActiveUsers() {
        List<User> users = userService.findAllActiveUsers();
        List<UserResponse> responses = users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Update user login timestamp
     * 
     * @param userId User ID
     * @return Updated user response
     */
    @PutMapping("/{userId}/login")
    @Operation(summary = "Update user login", 
               description = "Updates the last login timestamp for a user")
    public ResponseEntity<UserResponse> updateLastLogin(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        try {
            User user = userService.updateLastLogin(userId);
            return ResponseEntity.ok(convertToResponse(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Deactivate user account
     * 
     * @param userId User ID
     * @return Updated user response
     */
    @PutMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate user account", 
               description = "Deactivates a user account (admin operation)")
    public ResponseEntity<UserResponse> deactivateUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        try {
            User user = userService.deactivateUser(userId);
            return ResponseEntity.ok(convertToResponse(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get user statistics by status
     * 
     * @param status User status
     * @return Count of users with given status
     */
    @GetMapping("/stats/count")
    @Operation(summary = "Get user count by status", 
               description = "Returns the count of users by their status")
    public ResponseEntity<Long> getUserCountByStatus(
            @Parameter(description = "User status") @RequestParam User.UserStatus status) {
        
        long count = userService.getUserCountByStatus(status);
        return ResponseEntity.ok(count);
    }
    
    /**
     * Get users with recent activity
     * 
     * @param hours Number of hours to look back for activity
     * @return List of users with recent activity
     */
    @GetMapping("/recent-activity")
    @Operation(summary = "Get users with recent activity", 
               description = "Retrieves users who have logged in within the specified hours")
    public ResponseEntity<List<UserResponse>> getUsersWithRecentActivity(
            @Parameter(description = "Hours to look back") @RequestParam(defaultValue = "24") int hours) {
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<User> users = userService.findUsersWithRecentActivity(since);
        List<UserResponse> responses = users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Check if user can place orders
     * 
     * @param userId User ID
     * @return Boolean indicating if user can place orders
     */
    @GetMapping("/{userId}/can-order")
    @Operation(summary = "Check if user can place orders", 
               description = "Checks if a user is eligible to place orders")
    public ResponseEntity<Boolean> canUserPlaceOrders(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        boolean canOrder = userService.canUserPlaceOrders(userId);
        return ResponseEntity.ok(canOrder);
    }
    
    /**
     * Convert User entity to UserResponse DTO
     * 
     * @param user User entity
     * @return UserResponse DTO
     */
    private UserResponse convertToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
