package com.systemdesign.messaging;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced Message Queue Implementation
 * 
 * High-performance message queue system inspired by WhatsApp's messaging
 * infrastructure. Supports priorities, dead letter queues, batching,
 * and guaranteed delivery patterns.
 * 
 * Features:
 * - Priority-based message ordering
 * - Dead letter queue for failed messages
 * - Batch processing for efficiency
 * - Message persistence and durability
 * - Consumer group load balancing
 */

// Message structure
class Message {
    private final String id;
    private final String payload;
    private final MessagePriority priority;
    private final long timestamp;
    private final Map<String, String> headers;
    private final AtomicInteger retryCount;
    private final long expirationTime;
    
    public Message(String id, String payload, MessagePriority priority) {
        this.id = id;
        this.payload = payload;
        this.priority = priority;
        this.timestamp = System.currentTimeMillis();
        this.headers = new ConcurrentHashMap<>();
        this.retryCount = new AtomicInteger(0);
        this.expirationTime = timestamp + TimeUnit.HOURS.toMillis(24); // 24h TTL
    }
    
    public Message(String id, String payload, MessagePriority priority, 
                  Map<String, String> headers) {
        this(id, payload, priority);
        if (headers != null) {
            this.headers.putAll(headers);
        }
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
    
    public int incrementRetryCount() {
        return retryCount.incrementAndGet();
    }
    
    // Getters
    public String getId() { return id; }
    public String getPayload() { return payload; }
    public MessagePriority getPriority() { return priority; }
    public long getTimestamp() { return timestamp; }
    public Map<String, String> getHeaders() { return headers; }
    public int getRetryCount() { return retryCount.get(); }
    public long getExpirationTime() { return expirationTime; }
    
    @Override
    public String toString() {
        return String.format("Message{id='%s', priority=%s, retries=%d, payload='%.30s...'}",
                           id, priority, retryCount.get(), payload);
    }
}

// Message priority levels
enum MessagePriority {
    CRITICAL(1),    // System alerts, payment notifications
    HIGH(2),        // Real-time messages, calls
    NORMAL(3),      // Regular chat messages
    LOW(4),         // File transfers, media
    BACKGROUND(5);  // Analytics, logs
    
    private final int level;
    
    MessagePriority(int level) {
        this.level = level;
    }
    
    public int getLevel() { return level; }
}

// Message consumer interface
interface MessageConsumer {
    boolean processMessage(Message message);
    String getConsumerId();
    MessagePriority[] getSupportedPriorities();
}

// Batch of messages for efficient processing
class MessageBatch {
    private final List<Message> messages;
    private final long batchId;
    private final long createdAt;
    
    public MessageBatch(List<Message> messages) {
        this.messages = new ArrayList<>(messages);
        this.batchId = System.nanoTime();
        this.createdAt = System.currentTimeMillis();
    }
    
    public List<Message> getMessages() { return Collections.unmodifiableList(messages); }
    public long getBatchId() { return batchId; }
    public long getCreatedAt() { return createdAt; }
    public int getSize() { return messages.size(); }
}

// Dead letter queue for failed messages
class DeadLetterQueue {
    private final Queue<Message> deadMessages;
    private final AtomicLong totalDeadMessages;
    private final Map<String, Integer> failureReasons;
    
    public DeadLetterQueue() {
        this.deadMessages = new ConcurrentLinkedQueue<>();
        this.totalDeadMessages = new AtomicLong(0);
        this.failureReasons = new ConcurrentHashMap<>();
    }
    
    public void addDeadMessage(Message message, String reason) {
        deadMessages.offer(message);
        totalDeadMessages.incrementAndGet();
        failureReasons.merge(reason, 1, Integer::sum);
        
        System.out.println("Message moved to DLQ: " + message.getId() + 
                         " (Reason: " + reason + ")");
    }
    
    public Message pollDeadMessage() {
        return deadMessages.poll();
    }
    
    public int getSize() {
        return deadMessages.size();
    }
    
    public long getTotalDeadMessages() {
        return totalDeadMessages.get();
    }
    
    public Map<String, Integer> getFailureReasons() {
        return new HashMap<>(failureReasons);
    }
}

// Consumer group for load balancing
class ConsumerGroup {
    private final String groupId;
    private final List<MessageConsumer> consumers;
    private final AtomicInteger roundRobinIndex;
    private final Map<String, AtomicLong> consumerMessageCounts;
    
    public ConsumerGroup(String groupId) {
        this.groupId = groupId;
        this.consumers = new CopyOnWriteArrayList<>();
        this.roundRobinIndex = new AtomicInteger(0);
        this.consumerMessageCounts = new ConcurrentHashMap<>();
    }
    
    public void addConsumer(MessageConsumer consumer) {
        consumers.add(consumer);
        consumerMessageCounts.put(consumer.getConsumerId(), new AtomicLong(0));
        System.out.println("Added consumer " + consumer.getConsumerId() + 
                         " to group " + groupId);
    }
    
    public void removeConsumer(MessageConsumer consumer) {
        consumers.remove(consumer);
        consumerMessageCounts.remove(consumer.getConsumerId());
        System.out.println("Removed consumer " + consumer.getConsumerId() + 
                         " from group " + groupId);
    }
    
    public MessageConsumer getNextConsumer(MessagePriority priority) {
        List<MessageConsumer> eligibleConsumers = consumers.stream()
            .filter(consumer -> Arrays.asList(consumer.getSupportedPriorities())
                                     .contains(priority))
            .collect(java.util.stream.Collectors.toList());
        
        if (eligibleConsumers.isEmpty()) {
            return null;
        }
        
        // Round-robin load balancing
        int index = roundRobinIndex.getAndIncrement() % eligibleConsumers.size();
        MessageConsumer selected = eligibleConsumers.get(index);
        consumerMessageCounts.get(selected.getConsumerId()).incrementAndGet();
        
        return selected;
    }
    
    public String getGroupId() { return groupId; }
    public int getConsumerCount() { return consumers.size(); }
    
    public void printStats() {
        System.out.println("Consumer Group: " + groupId);
        consumerMessageCounts.forEach((consumerId, count) -> {
            System.out.println("  " + consumerId + ": " + count.get() + " messages");
        });
    }
}

// Main message queue implementation
class AdvancedMessageQueue {
    private final String queueName;
    private final PriorityBlockingQueue<Message> messageQueue;
    private final DeadLetterQueue deadLetterQueue;
    private final Map<String, ConsumerGroup> consumerGroups;
    private final ScheduledExecutorService processingExecutor;
    private final ExecutorService consumerExecutor;
    
    // Configuration
    private final int maxRetries;
    private final int batchSize;
    private final long batchTimeoutMs;
    
    // Metrics
    private final AtomicLong totalMessagesProduced;
    private final AtomicLong totalMessagesConsumed;
    private final AtomicLong totalBatchesProcessed;
    private final Map<MessagePriority, AtomicLong> priorityStats;
    
    public AdvancedMessageQueue(String queueName, int maxRetries, 
                               int batchSize, long batchTimeoutMs) {
        this.queueName = queueName;
        this.maxRetries = maxRetries;
        this.batchSize = batchSize;
        this.batchTimeoutMs = batchTimeoutMs;
        
        // Priority comparator (lower number = higher priority)
        this.messageQueue = new PriorityBlockingQueue<>(1000, 
            Comparator.comparing((Message m) -> m.getPriority().getLevel())
                     .thenComparing(Message::getTimestamp));
        
        this.deadLetterQueue = new DeadLetterQueue();
        this.consumerGroups = new ConcurrentHashMap<>();
        this.processingExecutor = Executors.newScheduledThreadPool(4);
        this.consumerExecutor = Executors.newCachedThreadPool();
        
        // Initialize metrics
        this.totalMessagesProduced = new AtomicLong(0);
        this.totalMessagesConsumed = new AtomicLong(0);
        this.totalBatchesProcessed = new AtomicLong(0);
        this.priorityStats = new ConcurrentHashMap<>();
        
        for (MessagePriority priority : MessagePriority.values()) {
            priorityStats.put(priority, new AtomicLong(0));
        }
        
        startProcessing();
    }
    
    // Producer methods
    public boolean produce(Message message) {
        if (message.isExpired()) {
            deadLetterQueue.addDeadMessage(message, "Message expired");
            return false;
        }
        
        boolean added = messageQueue.offer(message);
        if (added) {
            totalMessagesProduced.incrementAndGet();
            priorityStats.get(message.getPriority()).incrementAndGet();
            System.out.println("Produced: " + message);
        }
        return added;
    }
    
    public void produceBatch(List<Message> messages) {
        for (Message message : messages) {
            produce(message);
        }
    }
    
    // Consumer group management
    public void createConsumerGroup(String groupId) {
        consumerGroups.put(groupId, new ConsumerGroup(groupId));
        System.out.println("Created consumer group: " + groupId);
    }
    
    public void addConsumer(String groupId, MessageConsumer consumer) {
        ConsumerGroup group = consumerGroups.get(groupId);
        if (group != null) {
            group.addConsumer(consumer);
        }
    }
    
    public void removeConsumer(String groupId, MessageConsumer consumer) {
        ConsumerGroup group = consumerGroups.get(groupId);
        if (group != null) {
            group.removeConsumer(consumer);
        }
    }
    
    // Start background processing
    private void startProcessing() {
        // Batch processing scheduler
        processingExecutor.scheduleAtFixedRate(this::processBatch, 
            0, batchTimeoutMs, TimeUnit.MILLISECONDS);
        
        // Clean up expired messages
        processingExecutor.scheduleAtFixedRate(this::cleanupExpiredMessages, 
            0, 60, TimeUnit.SECONDS);
        
        // Stats reporting
        processingExecutor.scheduleAtFixedRate(this::printStats, 
            10, 30, TimeUnit.SECONDS);
    }
    
    private void processBatch() {
        List<Message> batchMessages = new ArrayList<>();
        
        // Collect messages for batch
        for (int i = 0; i < batchSize && !messageQueue.isEmpty(); i++) {
            Message message = messageQueue.poll();
            if (message != null && !message.isExpired()) {
                batchMessages.add(message);
            } else if (message != null) {
                deadLetterQueue.addDeadMessage(message, "Expired during processing");
            }
        }
        
        if (!batchMessages.isEmpty()) {
            MessageBatch batch = new MessageBatch(batchMessages);
            processMessageBatch(batch);
        }
    }
    
    private void processMessageBatch(MessageBatch batch) {
        totalBatchesProcessed.incrementAndGet();
        
        System.out.println("Processing batch " + batch.getBatchId() + 
                         " with " + batch.getSize() + " messages");
        
        // Group messages by priority for efficient processing
        Map<MessagePriority, List<Message>> priorityGroups = new HashMap<>();
        for (Message message : batch.getMessages()) {
            priorityGroups.computeIfAbsent(message.getPriority(), 
                                         k -> new ArrayList<>()).add(message);
        }
        
        // Process each priority group
        priorityGroups.forEach((priority, messages) -> {
            CompletableFuture.runAsync(() -> 
                processPriorityGroup(priority, messages), consumerExecutor);
        });
    }
    
    private void processPriorityGroup(MessagePriority priority, List<Message> messages) {
        for (Message message : messages) {
            boolean processed = false;
            
            // Try to find a consumer for this priority
            for (ConsumerGroup group : consumerGroups.values()) {
                MessageConsumer consumer = group.getNextConsumer(priority);
                if (consumer != null) {
                    try {
                        processed = consumer.processMessage(message);
                        if (processed) {
                            totalMessagesConsumed.incrementAndGet();
                            break;
                        }
                    } catch (Exception e) {
                        System.err.println("Consumer error: " + e.getMessage());
                    }
                }
            }
            
            // Handle processing failure
            if (!processed) {
                handleProcessingFailure(message);
            }
        }
    }
    
    private void handleProcessingFailure(Message message) {
        int retries = message.incrementRetryCount();
        
        if (retries < maxRetries) {
            // Re-queue for retry with exponential backoff
            processingExecutor.schedule(() -> messageQueue.offer(message), 
                (long) Math.pow(2, retries), TimeUnit.SECONDS);
            
            System.out.println("Requeuing message " + message.getId() + 
                             " (retry " + retries + "/" + maxRetries + ")");
        } else {
            deadLetterQueue.addDeadMessage(message, "Max retries exceeded");
        }
    }
    
    private void cleanupExpiredMessages() {
        Iterator<Message> iterator = messageQueue.iterator();
        int cleaned = 0;
        
        while (iterator.hasNext()) {
            Message message = iterator.next();
            if (message.isExpired()) {
                iterator.remove();
                deadLetterQueue.addDeadMessage(message, "Expired during cleanup");
                cleaned++;
            }
        }
        
        if (cleaned > 0) {
            System.out.println("Cleaned up " + cleaned + " expired messages");
        }
    }
    
    // Statistics and monitoring
    public void printStats() {
        System.out.println("\n=== Queue Statistics: " + queueName + " ===");
        System.out.println("Queue size: " + messageQueue.size());
        System.out.println("Total produced: " + totalMessagesProduced.get());
        System.out.println("Total consumed: " + totalMessagesConsumed.get());
        System.out.println("Total batches: " + totalBatchesProcessed.get());
        System.out.println("Dead letter queue: " + deadLetterQueue.getSize());
        
        System.out.println("Priority distribution:");
        priorityStats.forEach((priority, count) -> {
            System.out.println("  " + priority + ": " + count.get());
        });
        
        System.out.println("Consumer groups:");
        consumerGroups.values().forEach(ConsumerGroup::printStats);
        
        if (deadLetterQueue.getSize() > 0) {
            System.out.println("DLQ failure reasons:");
            deadLetterQueue.getFailureReasons().forEach((reason, count) -> {
                System.out.println("  " + reason + ": " + count);
            });
        }
        System.out.println();
    }
    
    public void shutdown() {
        processingExecutor.shutdown();
        consumerExecutor.shutdown();
    }
    
    // Getters for monitoring
    public int getQueueSize() { return messageQueue.size(); }
    public long getTotalProduced() { return totalMessagesProduced.get(); }
    public long getTotalConsumed() { return totalMessagesConsumed.get(); }
    public int getDeadLetterQueueSize() { return deadLetterQueue.getSize(); }
}

// Sample consumer implementations
class ChatMessageConsumer implements MessageConsumer {
    private final String consumerId;
    private final Random random = new Random();
    
    public ChatMessageConsumer(String consumerId) {
        this.consumerId = consumerId;
    }
    
    @Override
    public boolean processMessage(Message message) {
        try {
            // Simulate message processing time
            Thread.sleep(random.nextInt(100) + 50);
            
            System.out.println(consumerId + " processed chat message: " + 
                             message.getId());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    @Override
    public String getConsumerId() {
        return consumerId;
    }
    
    @Override
    public MessagePriority[] getSupportedPriorities() {
        return new MessagePriority[]{MessagePriority.HIGH, MessagePriority.NORMAL};
    }
}

class NotificationConsumer implements MessageConsumer {
    private final String consumerId;
    
    public NotificationConsumer(String consumerId) {
        this.consumerId = consumerId;
    }
    
    @Override
    public boolean processMessage(Message message) {
        System.out.println(consumerId + " sent notification for: " + 
                         message.getId());
        return true;
    }
    
    @Override
    public String getConsumerId() {
        return consumerId;
    }
    
    @Override
    public MessagePriority[] getSupportedPriorities() {
        return new MessagePriority[]{MessagePriority.CRITICAL, MessagePriority.HIGH};
    }
}

public class AdvancedMessageQueueExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Advanced Message Queue Demo (WhatsApp-style) ===\n");
        
        // Create message queue
        AdvancedMessageQueue queue = new AdvancedMessageQueue(
            "whatsapp-messages", 3, 10, 2000
        );
        
        // Create consumer groups
        queue.createConsumerGroup("chat-processors");
        queue.createConsumerGroup("notification-senders");
        
        // Add consumers
        queue.addConsumer("chat-processors", new ChatMessageConsumer("chat-worker-1"));
        queue.addConsumer("chat-processors", new ChatMessageConsumer("chat-worker-2"));
        queue.addConsumer("notification-senders", new NotificationConsumer("notif-worker-1"));
        
        // Simulate message production
        simulateMessageTraffic(queue);
        
        // Let the system process for a while
        Thread.sleep(15000);
        
        queue.shutdown();
        System.out.println("Demo completed");
    }
    
    private static void simulateMessageTraffic(AdvancedMessageQueue queue) {
        Random random = new Random();
        String[] payloads = {
            "Hello, how are you?",
            "Payment confirmation for order #12345",
            "System alert: High CPU usage",
            "Meeting reminder in 15 minutes",
            "File transfer completed",
            "New message in group chat",
            "Voice call incoming"
        };
        
        MessagePriority[] priorities = MessagePriority.values();
        
        // Produce messages over time
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    String payload = payloads[random.nextInt(payloads.length)];
                    MessagePriority priority = priorities[random.nextInt(priorities.length)];
                    
                    Message message = new Message(
                        "MSG_" + System.nanoTime(),
                        payload,
                        priority
                    );
                    
                    queue.produce(message);
                    
                    // Vary production rate
                    Thread.sleep(random.nextInt(200) + 50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // Simulate some batch production
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds
                
                List<Message> batch = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                    batch.add(new Message(
                        "BATCH_MSG_" + i,
                        "Batch message " + i,
                        MessagePriority.NORMAL
                    ));
                }
                
                System.out.println("Producing batch of " + batch.size() + " messages");
                queue.produceBatch(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
