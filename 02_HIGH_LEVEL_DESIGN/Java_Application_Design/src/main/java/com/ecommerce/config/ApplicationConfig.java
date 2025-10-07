package com.ecommerce.config;

/**
 * Application Configuration
 * 
 * Central configuration class for the e-commerce application.
 * Will be enhanced with Spring Boot configuration in next phase.
 */
public class ApplicationConfig {
    
    // Database configuration
    private String databaseUrl = "jdbc:h2:mem:ecommerce";
    private String databaseUsername = "sa";
    private String databasePassword = "";
    
    // Cache configuration
    private boolean cacheEnabled = true;
    private int cacheSize = 1000;
    
    // Circuit breaker configuration
    private int circuitBreakerThreshold = 5;
    private long circuitBreakerTimeout = 30000;
    
    public void initialize() {
        System.out.println("ApplicationConfig initialized");
        System.out.println("Database URL: " + databaseUrl);
        System.out.println("Cache enabled: " + cacheEnabled);
        System.out.println("Circuit breaker threshold: " + circuitBreakerThreshold);
    }
    
    // Getters and setters
    public String getDatabaseUrl() { return databaseUrl; }
    public void setDatabaseUrl(String databaseUrl) { this.databaseUrl = databaseUrl; }
    
    public String getDatabaseUsername() { return databaseUsername; }
    public void setDatabaseUsername(String databaseUsername) { this.databaseUsername = databaseUsername; }
    
    public String getDatabasePassword() { return databasePassword; }
    public void setDatabasePassword(String databasePassword) { this.databasePassword = databasePassword; }
    
    public boolean isCacheEnabled() { return cacheEnabled; }
    public void setCacheEnabled(boolean cacheEnabled) { this.cacheEnabled = cacheEnabled; }
    
    public int getCacheSize() { return cacheSize; }
    public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }
    
    public int getCircuitBreakerThreshold() { return circuitBreakerThreshold; }
    public void setCircuitBreakerThreshold(int circuitBreakerThreshold) { this.circuitBreakerThreshold = circuitBreakerThreshold; }
    
    public long getCircuitBreakerTimeout() { return circuitBreakerTimeout; }
    public void setCircuitBreakerTimeout(long circuitBreakerTimeout) { this.circuitBreakerTimeout = circuitBreakerTimeout; }
}
