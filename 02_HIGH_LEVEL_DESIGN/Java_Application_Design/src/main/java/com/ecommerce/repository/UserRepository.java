package com.ecommerce.repository;

import com.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Repository - Spring Data JPA Repository
 * 
 * Provides data access layer for User entities with:
 * - Standard CRUD operations via JpaRepository
 * - Custom query methods using Spring Data conventions
 * - Custom JPQL queries for complex operations
 * - Repository pattern implementation
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Find user by email address
     * Uses Spring Data method naming convention
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find users by status
     * Useful for filtering active/inactive users
     */
    List<User> findByStatus(User.UserStatus status);
    
    /**
     * Check if user exists by email
     * Efficient existence check without loading full entity
     */
    boolean existsByEmail(String email);
    
    /**
     * Find active users created after a specific date
     * Custom JPQL query for complex filtering
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.createdAt > :date")
    List<User> findActiveUsersCreatedAfter(java.time.LocalDateTime date);
    
    /**
     * Count users by status
     * Useful for analytics and reporting
     */
    long countByStatus(User.UserStatus status);
    
    /**
     * Find users with recent login activity
     * Custom query for user engagement analysis
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt > :since ORDER BY u.lastLoginAt DESC")
    List<User> findUsersWithRecentActivity(java.time.LocalDateTime since);
}
