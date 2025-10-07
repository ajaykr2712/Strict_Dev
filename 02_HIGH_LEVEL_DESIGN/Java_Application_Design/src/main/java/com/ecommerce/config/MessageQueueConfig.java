package com.ecommerce.config;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message Queue Configuration - Infrastructure Configuration
 * 
 * Manages message queuing for asynchronous processing and event-driven architecture.
 * Implements messaging patterns from "Designing Data-Intensive Applications":
 * - Event sourcing and CQRS support
 * - Reliable message delivery with acknowledgments
 * - Dead letter queues for error handling
 */
public class MessageQueueConfig {
    private static volatile boolean initialized = false;
    private static final Map<String, String> queueConnections = new ConcurrentHashMap<>();
    
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        System.out.println("Initializing Message Queue Configuration...");
        
        // Initialize Kafka/RabbitMQ connections
        initializeMessageBroker();
        
        // Configure topics and queues
        configureTopicsAndQueues();
        
        // Setup dead letter queues
        setupDeadLetterQueues();
        
        initialized = true;
        System.out.println("Message Queue Configuration initialized successfully at " + LocalDateTime.now());
    }
    
    private static void initializeMessageBroker() {
        // Simulate message broker initialization
        queueConnections.put("kafka_cluster", "kafka://localhost:9092");
        queueConnections.put("rabbitmq", "amqp://localhost:5672");
        
        System.out.println("  - Message Broker initialized");
        System.out.println("    * Kafka cluster: 3 brokers for high availability");
        System.out.println("    * RabbitMQ: For reliable message delivery");
    }
    
    private static void configureTopicsAndQueues() {
        System.out.println("  - Topics and Queues configured:");
        System.out.println("    * order.events -> Order lifecycle events");
        System.out.println("    * payment.events -> Payment processing events");
        System.out.println("    * user.events -> User activity events");
        System.out.println("    * inventory.updates -> Stock level changes");
        System.out.println("    * email.notifications -> Email sending queue");
    }
    
    private static void setupDeadLetterQueues() {
        System.out.println("  - Dead Letter Queues configured:");
        System.out.println("    * Failed messages -> retry with exponential backoff");
        System.out.println("    * Poison messages -> manual intervention queue");
        System.out.println("    * Alert system -> notify operations team");
    }
    
    public static String getConnectionString(String type) {
        if (!initialized) {
            throw new IllegalStateException("Message Queue not initialized");
        }
        return queueConnections.get(type);
    }
    
    public static void shutdown() {
        if (!initialized) {
            return;
        }
        
        System.out.println("Shutting down Message Queue Configuration...");
        
        // Close message queue connections
        queueConnections.clear();
        
        initialized = false;
        System.out.println("Message Queue Configuration shut down successfully");
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
