package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * E-Commerce Platform - Spring Boot Application
 * 
 * Main application entry point for the data-intensive e-commerce platform.
 * This application demonstrates high-level system design principles from
 * "Designing Data-Intensive Applications" including:
 * 
 * - Reliability: Circuit breakers, retries, graceful degradation
 * - Scalability: Horizontal scaling, caching, async processing
 * - Maintainability: Clean architecture, dependency injection, monitoring
 * 
 * Architecture Features:
 * - Domain-Driven Design (DDD) with clear bounded contexts
 * - CQRS (Command Query Responsibility Segregation) for read/write separation
 * - Event Sourcing for audit trails and state reconstruction
 * - Microservices-ready modular design
 * - Distributed caching and database sharding support
 * 
 * @author System Design Team
 * @version 1.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class ECommerceApplication {
    
    /**
     * Main method - Application entry point
     * 
     * Starts the Spring Boot application with embedded Tomcat server
     * and auto-configuration for all components.
     */
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("E-Commerce Platform - Spring Boot");
        System.out.println("Based on 'Designing Data-Intensive Applications'");
        System.out.println("Starting application...");
        System.out.println("========================================");
        
        SpringApplication.run(ECommerceApplication.class, args);
        
        System.out.println("Application started successfully!");
        System.out.println("Access the API documentation at: http://localhost:8080/swagger-ui.html");
        System.out.println("Health check endpoint: http://localhost:8080/actuator/health");
        System.out.println("Metrics endpoint: http://localhost:8080/actuator/metrics");
    }
    


}
