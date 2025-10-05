/**
 * Singleton Pattern Implementation
 * 
 * The Singleton pattern ensures that a class has only one instance and provides
 * a global point of access to that instance.
 * 
 * Real-world examples:
 * - Database Connection Pool
 * - Configuration Manager
 * - Logger
 * - Cache Manager
 * 
 * Thread-Safe Implementation using Bill Pugh Singleton Pattern
 */

public class SingletonPattern {
    
    // Private constructor to prevent instantiation
    private SingletonPattern() {
        // Prevent reflection-based instantiation
        if (SingletonHelper.INSTANCE != null) {
            throw new IllegalStateException("Singleton instance already created!");
        }
    }
    
    // Static nested class - inner class
    private static class SingletonHelper {
        // This will be loaded only when getInstance() is called
        private static final SingletonPattern INSTANCE = new SingletonPattern();
    }
    
    // Global access point
    public static SingletonPattern getInstance() {
        return SingletonHelper.INSTANCE;
    }
    
    // Example business methods
    public void doSomething() {
        System.out.println("Doing something with singleton instance: " + this.hashCode());
    }
    
    // Prevent cloning
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Clone is not allowed for Singleton");
    }
}

/**
 * Database Connection Pool Example using Singleton
 */
class DatabaseConnectionPool {
    private static volatile DatabaseConnectionPool instance;
    private final int maxConnections = 10;
    private int currentConnections = 0;
    
    private DatabaseConnectionPool() {
        // Initialize connection pool
        System.out.println("Database Connection Pool initialized");
    }
    
    // Double-checked locking pattern
    public static DatabaseConnectionPool getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionPool.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionPool();
                }
            }
        }
        return instance;
    }
    
    public synchronized String getConnection() {
        if (currentConnections < maxConnections) {
            currentConnections++;
            return "Connection-" + currentConnections;
        }
        return null; // No available connections
    }
    
    public synchronized void releaseConnection(String connection) {
        if (currentConnections > 0) {
            currentConnections--;
            System.out.println("Released: " + connection);
        }
    }
    
    public int getAvailableConnections() {
        return maxConnections - currentConnections;
    }
}

/**
 * Configuration Manager using Enum Singleton (Best Practice)
 */
enum ConfigurationManager {
    INSTANCE;
    
    private String databaseUrl;
    private String apiKey;
    private int timeout;
    
    ConfigurationManager() {
        // Load configuration from file/environment
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        this.databaseUrl = "jdbc:mysql://localhost:3306/mydb";
        this.apiKey = "your-api-key-here";
        this.timeout = 30000;
    }
    
    public String getDatabaseUrl() { return databaseUrl; }
    public String getApiKey() { return apiKey; }
    public int getTimeout() { return timeout; }
    
    public void updateConfiguration(String dbUrl, String key, int timeoutMs) {
        this.databaseUrl = dbUrl;
        this.apiKey = key;
        this.timeout = timeoutMs;
    }
}

/**
 * Demo class to test Singleton implementations
 */
class SingletonDemo {
    public static void main(String[] args) {
        System.out.println("=== Singleton Pattern Demo ===\n");
        
        // Test Bill Pugh Singleton
        System.out.println("1. Bill Pugh Singleton Pattern:");
        SingletonPattern instance1 = SingletonPattern.getInstance();
        SingletonPattern instance2 = SingletonPattern.getInstance();
        
        System.out.println("Instance 1 hash: " + instance1.hashCode());
        System.out.println("Instance 2 hash: " + instance2.hashCode());
        System.out.println("Are they same? " + (instance1 == instance2));
        
        instance1.doSomething();
        instance2.doSomething();
        
        System.out.println("\n2. Database Connection Pool:");
        DatabaseConnectionPool pool1 = DatabaseConnectionPool.getInstance();
        DatabaseConnectionPool pool2 = DatabaseConnectionPool.getInstance();
        
        System.out.println("Pool instances are same: " + (pool1 == pool2));
        
        String conn1 = pool1.getConnection();
        String conn2 = pool1.getConnection();
        System.out.println("Got connections: " + conn1 + ", " + conn2);
        System.out.println("Available connections: " + pool1.getAvailableConnections());
        
        pool1.releaseConnection(conn1);
        System.out.println("Available connections after release: " + pool1.getAvailableConnections());
        
        System.out.println("\n3. Enum Singleton (Configuration Manager):");
        ConfigurationManager config1 = ConfigurationManager.INSTANCE;
        ConfigurationManager config2 = ConfigurationManager.INSTANCE;
        
        System.out.println("Config instances are same: " + (config1 == config2));
        System.out.println("Database URL: " + config1.getDatabaseUrl());
        System.out.println("API Key: " + config1.getApiKey());
        System.out.println("Timeout: " + config1.getTimeout());
        
        // Update configuration
        config1.updateConfiguration("jdbc:postgresql://localhost:5432/newdb", "new-api-key", 60000);
        System.out.println("Updated Database URL: " + config2.getDatabaseUrl());
        
        // Thread safety test
        System.out.println("\n4. Thread Safety Test:");
        testThreadSafety();
    }
    
    private static void testThreadSafety() {
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                SingletonPattern instance = SingletonPattern.getInstance();
                System.out.println("Thread " + Thread.currentThread().getName() + 
                                 " got instance: " + instance.hashCode());
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

/*
 * When to use Singleton Pattern:
 * 
 * ✅ Use when:
 * - You need exactly one instance of a class
 * - Global access point is required
 * - Instance creation is expensive
 * - Managing shared resources (DB connections, file systems)
 * 
 * ❌ Avoid when:
 * - You might need multiple instances in the future
 * - Testing becomes difficult due to global state
 * - It introduces tight coupling
 * - Concurrent access without proper synchronization
 * 
 * Best Practices:
 * 1. Use Enum for simple singletons (thread-safe by default)
 * 2. Use Bill Pugh pattern for lazy initialization
 * 3. Use double-checked locking for performance-critical scenarios
 * 4. Prevent reflection and cloning attacks
 * 5. Consider dependency injection alternatives
 */
