package com.ecommerce.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * User Domain Entity
 * 
 * Represents a user in the e-commerce system with immutable design
 * following Domain-Driven Design principles.
 * 
 * Key Design Principles:
 * - Immutability for thread safety and cache reliability
 * - Value object pattern for identity
 * - Rich domain model with behavior
 */
public class User {
    private final String id;
    private final String email;
    private final String name;
    private final UserStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLoginAt;
    
    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, DELETED
    }
    
    // Constructor for new user creation
    public User(String email, String name) {
        this.id = UUID.randomUUID().toString();
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = null;
        
        validateEmail(email);
    }
    
    // Constructor for existing user (from database)
    public User(String id, String email, String name, UserStatus status, 
                LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.lastLoginAt = lastLoginAt;
    }
    
    private void validateEmail(String email) {
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    // Create a new instance with updated login time (immutable update)
    public User withLastLogin(LocalDateTime lastLoginAt) {
        return new User(this.id, this.email, this.name, this.status, 
                       this.createdAt, lastLoginAt);
    }
    
    // Create a new instance with updated status (immutable update)
    public User withStatus(UserStatus status) {
        return new User(this.id, this.email, this.name, status, 
                       this.createdAt, this.lastLoginAt);
    }
    
    // Business logic methods
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
    
    public boolean canPlaceOrder() {
        return isActive();
    }
    
    // Getters
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
