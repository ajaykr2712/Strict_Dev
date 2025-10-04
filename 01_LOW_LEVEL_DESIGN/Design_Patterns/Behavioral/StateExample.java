import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State Pattern Implementation for WhatsApp-like Messaging Platform
 * 
 * Real-world Use Case: Message state management in a messaging application
 * - Messages go through different states: Draft, Sent, Delivered, Read
 * - Each state has different behaviors and transitions
 * - State-specific validations and actions
 * - Message encryption/decryption based on state
 * - Retry mechanisms for failed messages
 * 
 * This demonstrates how an object's behavior can change when its internal state changes.
 */

// State interface - defines what all message states can do
interface MessageState {
    void send(MessageContext context);
    void markAsDelivered(MessageContext context);
    void markAsRead(MessageContext context);
    void retry(MessageContext context);
    void encrypt(MessageContext context);
    void decrypt(MessageContext context);
    String getStateName();
    String getStateDescription();
    List<String> getAllowedTransitions();
}

// Context class - represents a WhatsApp message
class MessageContext {
    private MessageState currentState;
    private final String messageId;
    private final String senderId;
    private final String receiverId;
    private String content;
    private boolean isEncrypted;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private int retryCount;
    private final List<String> stateHistory;
    private String encryptionKey;

    public MessageContext(String messageId, String senderId, String receiverId, String content) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.isEncrypted = false;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
        this.stateHistory = new ArrayList<>();
        this.encryptionKey = generateEncryptionKey();
        
        // Start in draft state
        this.currentState = new DraftState();
        addStateToHistory("DRAFT");
    }

    // State management
    public void setState(MessageState state) {
        this.currentState = state;
        addStateToHistory(state.getStateName());
        System.out.println("[MESSAGE " + messageId + "] State changed to: " + state.getStateName());
    }

    public MessageState getState() {
        return currentState;
    }

    private void addStateToHistory(String stateName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        stateHistory.add(timestamp + " - " + stateName);
    }

    // Delegate state-specific operations to current state
    public void send() {
        currentState.send(this);
    }

    public void markAsDelivered() {
        currentState.markAsDelivered(this);
    }

    public void markAsRead() {
        currentState.markAsRead(this);
    }

    public void retry() {
        currentState.retry(this);
    }

    public void encrypt() {
        currentState.encrypt(this);
    }

    public void decrypt() {
        currentState.decrypt(this);
    }

    // Getters and setters
    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isEncrypted() { return isEncrypted; }
    public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }
    public String getEncryptionKey() { return encryptionKey; }
    public List<String> getStateHistory() { return new ArrayList<>(stateHistory); }

    private String generateEncryptionKey() {
        return "key_" + messageId + "_" + System.currentTimeMillis();
    }

    public void printStatus() {
        System.out.println("\n[MESSAGE STATUS]");
        System.out.println("  ID: " + messageId);
        System.out.println("  From: " + senderId + " To: " + receiverId);
        System.out.println("  Content: " + (isEncrypted ? "[ENCRYPTED]" : content));
        System.out.println("  Current State: " + currentState.getStateName());
        System.out.println("  Retry Count: " + retryCount);
        System.out.println("  State History: " + String.join(" → ", stateHistory));
    }
}

// Concrete State - Draft state (message being composed)
class DraftState implements MessageState {
    @Override
    public void send(MessageContext context) {
        System.out.println("[DRAFT] Preparing message for sending...");
        
        // Validate message content
        if (context.getContent() == null || context.getContent().trim().isEmpty()) {
            System.out.println("[DRAFT] Cannot send empty message");
            return;
        }
        
        // Encrypt message before sending
        encrypt(context);
        
        // Simulate sending process
        context.setSentAt(LocalDateTime.now());
        System.out.println("[DRAFT] Message sent successfully");
        
        // Transition to sent state
        context.setState(new SentState());
    }

    @Override
    public void markAsDelivered(MessageContext context) {
        System.out.println("[DRAFT] Cannot mark draft message as delivered");
    }

    @Override
    public void markAsRead(MessageContext context) {
        System.out.println("[DRAFT] Cannot mark draft message as read");
    }

    @Override
    public void retry(MessageContext context) {
        System.out.println("[DRAFT] Draft messages don't need retry");
    }

    @Override
    public void encrypt(MessageContext context) {
        if (!context.isEncrypted()) {
            System.out.println("[DRAFT] Encrypting message content...");
            String originalContent = context.getContent();
            context.setContent("ENC[" + originalContent + "]_" + context.getEncryptionKey());
            context.setEncrypted(true);
            System.out.println("[DRAFT] Message encrypted");
        }
    }

    @Override
    public void decrypt(MessageContext context) {
        if (context.isEncrypted()) {
            System.out.println("[DRAFT] Decrypting message content...");
            String encryptedContent = context.getContent();
            String originalContent = extractOriginalContent(encryptedContent);
            context.setContent(originalContent);
            context.setEncrypted(false);
            System.out.println("[DRAFT] Message decrypted");
        }
    }

    private String extractOriginalContent(String encryptedContent) {
        if (encryptedContent.startsWith("ENC[") && encryptedContent.contains("]_")) {
            int start = encryptedContent.indexOf("[") + 1;
            int end = encryptedContent.indexOf("]_");
            return encryptedContent.substring(start, end);
        }
        return encryptedContent;
    }

    @Override
    public String getStateName() {
        return "DRAFT";
    }

    @Override
    public String getStateDescription() {
        return "Message is being composed";
    }

    @Override
    public List<String> getAllowedTransitions() {
        return Arrays.asList("SENT");
    }
}

// Concrete State - Sent state (message sent but not yet delivered)
class SentState implements MessageState {
    @Override
    public void send(MessageContext context) {
        System.out.println("[SENT] Message already sent");
    }

    @Override
    public void markAsDelivered(MessageContext context) {
        System.out.println("[SENT] Message delivered to recipient's device");
        context.setDeliveredAt(LocalDateTime.now());
        
        // Transition to delivered state
        context.setState(new DeliveredState());
    }

    @Override
    public void markAsRead(MessageContext context) {
        System.out.println("[SENT] Cannot mark as read - message not yet delivered");
    }

    @Override
    public void retry(MessageContext context) {
        System.out.println("[SENT] Retrying message delivery...");
        context.incrementRetryCount();
        
        if (context.getRetryCount() > 3) {
            System.out.println("[SENT] Max retry attempts reached - moving to failed state");
            context.setState(new FailedState());
        } else {
            System.out.println("[SENT] Retry attempt " + context.getRetryCount() + " completed");
            // In real implementation, this might trigger actual retry logic
        }
    }

    @Override
    public void encrypt(MessageContext context) {
        System.out.println("[SENT] Message already encrypted during sending");
    }

    @Override
    public void decrypt(MessageContext context) {
        System.out.println("[SENT] Cannot decrypt sent message - wait for delivery");
    }

    @Override
    public String getStateName() {
        return "SENT";
    }

    @Override
    public String getStateDescription() {
        return "Message sent, waiting for delivery confirmation";
    }

    @Override
    public List<String> getAllowedTransitions() {
        return Arrays.asList("DELIVERED", "FAILED");
    }
}

// Concrete State - Delivered state (message delivered but not read)
class DeliveredState implements MessageState {
    @Override
    public void send(MessageContext context) {
        System.out.println("[DELIVERED] Message already sent and delivered");
    }

    @Override
    public void markAsDelivered(MessageContext context) {
        System.out.println("[DELIVERED] Message already delivered");
    }

    @Override
    public void markAsRead(MessageContext context) {
        System.out.println("[DELIVERED] Message read by recipient");
        context.setReadAt(LocalDateTime.now());
        
        // Transition to read state
        context.setState(new ReadState());
    }

    @Override
    public void retry(MessageContext context) {
        System.out.println("[DELIVERED] No retry needed - message successfully delivered");
    }

    @Override
    public void encrypt(MessageContext context) {
        System.out.println("[DELIVERED] Message already encrypted");
    }

    @Override
    public void decrypt(MessageContext context) {
        if (context.isEncrypted()) {
            System.out.println("[DELIVERED] Decrypting message for recipient...");
            String encryptedContent = context.getContent();
            String originalContent = extractOriginalContent(encryptedContent);
            context.setContent(originalContent);
            context.setEncrypted(false);
            System.out.println("[DELIVERED] Message decrypted for reading");
        }
    }

    private String extractOriginalContent(String encryptedContent) {
        if (encryptedContent.startsWith("ENC[") && encryptedContent.contains("]_")) {
            int start = encryptedContent.indexOf("[") + 1;
            int end = encryptedContent.indexOf("]_");
            return encryptedContent.substring(start, end);
        }
        return encryptedContent;
    }

    @Override
    public String getStateName() {
        return "DELIVERED";
    }

    @Override
    public String getStateDescription() {
        return "Message delivered to recipient's device";
    }

    @Override
    public List<String> getAllowedTransitions() {
        return Arrays.asList("READ");
    }
}

// Concrete State - Read state (message read by recipient)
class ReadState implements MessageState {
    @Override
    public void send(MessageContext context) {
        System.out.println("[READ] Message already sent, delivered, and read");
    }

    @Override
    public void markAsDelivered(MessageContext context) {
        System.out.println("[READ] Message already delivered and read");
    }

    @Override
    public void markAsRead(MessageContext context) {
        System.out.println("[READ] Message already read");
    }

    @Override
    public void retry(MessageContext context) {
        System.out.println("[READ] No retry needed - message successfully read");
    }

    @Override
    public void encrypt(MessageContext context) {
        System.out.println("[READ] Cannot encrypt read message");
    }

    @Override
    public void decrypt(MessageContext context) {
        System.out.println("[READ] Message already decrypted when read");
    }

    @Override
    public String getStateName() {
        return "READ";
    }

    @Override
    public String getStateDescription() {
        return "Message read by recipient (blue ticks)";
    }

    @Override
    public List<String> getAllowedTransitions() {
        return Arrays.asList(); // Terminal state
    }
}

// Concrete State - Failed state (message delivery failed)
class FailedState implements MessageState {
    @Override
    public void send(MessageContext context) {
        System.out.println("[FAILED] Attempting to resend failed message...");
        context.setState(new SentState());
        context.send();
    }

    @Override
    public void markAsDelivered(MessageContext context) {
        System.out.println("[FAILED] Cannot mark failed message as delivered");
    }

    @Override
    public void markAsRead(MessageContext context) {
        System.out.println("[FAILED] Cannot mark failed message as read");
    }

    @Override
    public void retry(MessageContext context) {
        System.out.println("[FAILED] Retrying failed message...");
        context.setState(new SentState());
        context.retry();
    }

    @Override
    public void encrypt(MessageContext context) {
        System.out.println("[FAILED] Message remains encrypted");
    }

    @Override
    public void decrypt(MessageContext context) {
        System.out.println("[FAILED] Cannot decrypt failed message");
    }

    @Override
    public String getStateName() {
        return "FAILED";
    }

    @Override
    public String getStateDescription() {
        return "Message delivery failed";
    }

    @Override
    public List<String> getAllowedTransitions() {
        return Arrays.asList("SENT");
    }
}

// WhatsApp Message Manager
class WhatsAppMessageManager {
    private final Map<String, MessageContext> messages;

    public WhatsAppMessageManager() {
        this.messages = new ConcurrentHashMap<>();
    }

    public MessageContext createMessage(String senderId, String receiverId, String content) {
        String messageId = "msg_" + System.currentTimeMillis();
        MessageContext message = new MessageContext(messageId, senderId, receiverId, content);
        messages.put(messageId, message);
        
        System.out.println("[MANAGER] Created message: " + messageId);
        return message;
    }

    public MessageContext getMessage(String messageId) {
        return messages.get(messageId);
    }

    public void simulateMessageDelivery(String messageId) {
        MessageContext message = messages.get(messageId);
        if (message != null) {
            // Simulate network delay
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            message.markAsDelivered();
        }
    }

    public void simulateMessageRead(String messageId) {
        MessageContext message = messages.get(messageId);
        if (message != null) {
            message.decrypt(); // Decrypt for reading
            message.markAsRead();
        }
    }

    public void printAllMessages() {
        System.out.println("\n=== All Messages Status ===");
        messages.values().forEach(MessageContext::printStatus);
    }
}

// Demonstration class
class StatePatternDemo {
    public static void main(String[] args) {
        System.out.println("=== State Pattern Demo: WhatsApp Message States ===\n");

        WhatsAppMessageManager messageManager = new WhatsAppMessageManager();

        System.out.println("1. Creating and sending a normal message:");
        MessageContext message1 = messageManager.createMessage("alice", "bob", "Hello Bob!");
        message1.printStatus();

        // Send the message
        message1.send();
        message1.printStatus();

        // Simulate delivery
        messageManager.simulateMessageDelivery(message1.getMessageId());
        message1.printStatus();

        // Simulate reading
        messageManager.simulateMessageRead(message1.getMessageId());
        message1.printStatus();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("2. Creating a message that will fail:");
        MessageContext message2 = messageManager.createMessage("charlie", "david", "Hey David!");
        message2.send();
        
        // Simulate multiple failed retries
        for (int i = 0; i < 4; i++) {
            message2.retry();
        }
        message2.printStatus();

        // Try to resend failed message
        System.out.println("\n3. Attempting to resend failed message:");
        message2.send();
        messageManager.simulateMessageDelivery(message2.getMessageId());
        messageManager.simulateMessageRead(message2.getMessageId());
        message2.printStatus();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("4. Testing invalid operations:");
        MessageContext message3 = messageManager.createMessage("eve", "frank", "Test message");
        
        // Try invalid operations in draft state
        message3.markAsDelivered(); // Should fail
        message3.markAsRead();      // Should fail
        
        message3.send();
        message3.send(); // Try to send again - should indicate already sent
        
        messageManager.simulateMessageDelivery(message3.getMessageId());
        message3.markAsDelivered(); // Try to mark as delivered again
        
        messageManager.simulateMessageRead(message3.getMessageId());
        message3.markAsRead(); // Try to mark as read again

        System.out.println("\n" + "=".repeat(50));
        System.out.println("5. Demonstrating encryption/decryption:");
        MessageContext message4 = messageManager.createMessage("grace", "henry", "Secret message!");
        System.out.println("Original content: " + message4.getContent());
        
        message4.encrypt();
        System.out.println("After encryption: " + (message4.isEncrypted() ? "[ENCRYPTED]" : message4.getContent()));
        
        message4.decrypt();
        System.out.println("After decryption: " + message4.getContent());

        // Final summary
        messageManager.printAllMessages();

        System.out.println("\n=== State Pattern Benefits Demonstrated ===");
        System.out.println("✓ State-specific behavior: Each state handles operations differently");
        System.out.println("✓ State transitions: Objects change behavior when state changes");
        System.out.println("✓ Invalid operation prevention: States prevent invalid transitions");
        System.out.println("✓ Clean state management: State logic is encapsulated in state classes");
        System.out.println("✓ Extensibility: New states can be added without modifying existing code");
    }
}
