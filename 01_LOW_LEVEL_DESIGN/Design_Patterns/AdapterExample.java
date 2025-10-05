import java.util.*;

/**
 * Adapter Pattern Implementation
 * 
 * Real-world Use Case: WhatsApp Message Format Adapter
 * Adapting different message formats for unified processing
 */

public class AdapterExample {
    
    // Target interface - what the client expects
    interface UnifiedMessageProcessor {
        void processMessage(String messageId, String content, String sender, long timestamp);
        String formatMessage(String content);
        boolean validateMessage(String content);
    }
    
    // Legacy SMS system that needs to be adapted
    class LegacySMSSystem {
        public void sendSMS(String phoneNumber, String text) {
            System.out.println("[SMS] Sending to " + phoneNumber + ": " + text);
        }
        
        public boolean isValidSMS(String text) {
            return text != null && text.length() <= 160;
        }
        
        public String prepareSMSText(String originalText) {
            if (originalText.length() > 160) {
                return originalText.substring(0, 157) + "...";
            }
            return originalText;
        }
    }
    
    // Email system with different interface
    class EmailSystem {
        public void sendEmail(String to, String subject, String body) {
            System.out.println("[EMAIL] To: " + to + ", Subject: " + subject + ", Body: " + body);
        }
        
        public boolean validateEmailContent(String subject, String body) {
            return subject != null && !subject.isEmpty() && body != null;
        }
        
        public String formatEmailContent(String content) {
            return "Subject: WhatsApp Message\n\n" + content;
        }
    }
    
    // Push notification service
    class PushNotificationService {
        public void sendPush(String deviceToken, Map<String, Object> payload) {
            System.out.println("[PUSH] Device: " + deviceToken + ", Payload: " + payload);
        }
        
        public boolean isValidPushPayload(Map<String, Object> payload) {
            return payload.containsKey("message") && payload.containsKey("title");
        }
        
        public Map<String, Object> createPushPayload(String title, String message) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("message", message);
            payload.put("timestamp", System.currentTimeMillis());
            return payload;
        }
    }
    
    // Adapter for SMS System
    class SMSAdapter implements UnifiedMessageProcessor {
        private final LegacySMSSystem smsSystem;
        
        public SMSAdapter(LegacySMSSystem smsSystem) {
            this.smsSystem = smsSystem;
        }
        
        @Override
        public void processMessage(String messageId, String content, String sender, long timestamp) {
            String formattedContent = formatMessage(content);
            smsSystem.sendSMS(sender, formattedContent);
        }
        
        @Override
        public String formatMessage(String content) {
            return smsSystem.prepareSMSText(content);
        }
        
        @Override
        public boolean validateMessage(String content) {
            return smsSystem.isValidSMS(content);
        }
    }
    
    // Adapter for Email System
    class EmailAdapter implements UnifiedMessageProcessor {
        private final EmailSystem emailSystem;
        
        public EmailAdapter(EmailSystem emailSystem) {
            this.emailSystem = emailSystem;
        }
        
        @Override
        public void processMessage(String messageId, String content, String sender, long timestamp) {
            String formattedContent = formatMessage(content);
            emailSystem.sendEmail(sender, "WhatsApp Message", formattedContent);
        }
        
        @Override
        public String formatMessage(String content) {
            return emailSystem.formatEmailContent(content);
        }
        
        @Override
        public boolean validateMessage(String content) {
            return emailSystem.validateEmailContent("WhatsApp Message", content);
        }
    }
    
    // Adapter for Push Notification System
    class PushNotificationAdapter implements UnifiedMessageProcessor {
        private final PushNotificationService pushService;
        
        public PushNotificationAdapter(PushNotificationService pushService) {
            this.pushService = pushService;
        }
        
        @Override
        public void processMessage(String messageId, String content, String sender, long timestamp) {
            Map<String, Object> payload = pushService.createPushPayload("New Message", content);
            payload.put("sender", sender);
            payload.put("messageId", messageId);
            pushService.sendPush(sender, payload);
        }
        
        @Override
        public String formatMessage(String content) {
            Map<String, Object> payload = pushService.createPushPayload("Message", content);
            return payload.toString();
        }
        
        @Override
        public boolean validateMessage(String content) {
            Map<String, Object> payload = pushService.createPushPayload("Test", content);
            return pushService.isValidPushPayload(payload);
        }
    }
    
    // WhatsApp Message Router that uses adapters
    class WhatsAppMessageRouter {
        private final Map<String, UnifiedMessageProcessor> processors;
        
        public WhatsAppMessageRouter() {
            this.processors = new HashMap<>();
        }
        
        public void addProcessor(String type, UnifiedMessageProcessor processor) {
            processors.put(type, processor);
        }
        
        public void routeMessage(String messageId, String content, String sender, 
                               long timestamp, List<String> deliveryMethods) {
            for (String method : deliveryMethods) {
                UnifiedMessageProcessor processor = processors.get(method);
                if (processor != null) {
                    if (processor.validateMessage(content)) {
                        processor.processMessage(messageId, content, sender, timestamp);
                    } else {
                        System.out.println("[ROUTER] Invalid message for " + method + ": " + content);
                    }
                } else {
                    System.out.println("[ROUTER] No processor found for: " + method);
                }
            }
        }
        
        public void showAvailableProcessors() {
            System.out.println("[ROUTER] Available processors: " + processors.keySet());
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Adapter Pattern Demo: WhatsApp Message Routing ===\n");
        
        AdapterExample example = new AdapterExample();
        
        // Create legacy systems
        LegacySMSSystem smsSystem = example.new LegacySMSSystem();
        EmailSystem emailSystem = example.new EmailSystem();
        PushNotificationService pushService = example.new PushNotificationService();
        
        // Create adapters
        SMSAdapter smsAdapter = example.new SMSAdapter(smsSystem);
        EmailAdapter emailAdapter = example.new EmailAdapter(emailSystem);
        PushNotificationAdapter pushAdapter = example.new PushNotificationAdapter(pushService);
        
        // Create message router
        WhatsAppMessageRouter router = example.new WhatsAppMessageRouter();
        router.addProcessor("SMS", smsAdapter);
        router.addProcessor("EMAIL", emailAdapter);
        router.addProcessor("PUSH", pushAdapter);
        
        router.showAvailableProcessors();
        System.out.println();
        
        // Test message routing
        String messageId = "msg_001";
        String content = "Hello! This is a test message from WhatsApp.";
        String sender = "+1234567890";
        long timestamp = System.currentTimeMillis();
        
        System.out.println("1. Routing short message to all systems:");
        router.routeMessage(messageId, content, sender, timestamp, 
                          Arrays.asList("SMS", "EMAIL", "PUSH"));
        
        System.out.println("\n2. Routing long message (SMS will be truncated):");
        String longMessage = "This is a very long message that exceeds the SMS character limit. " +
                           "It contains important information that needs to be delivered to the user " +
                           "but SMS has a 160 character limit so it will be truncated by the adapter.";
        
        router.routeMessage("msg_002", longMessage, sender, timestamp, 
                          Arrays.asList("SMS", "EMAIL", "PUSH"));
        
        System.out.println("\n3. Testing validation with invalid content:");
        router.routeMessage("msg_003", null, sender, timestamp, 
                          Arrays.asList("EMAIL"));
        
        System.out.println("\n4. Testing with unknown delivery method:");
        router.routeMessage("msg_004", "Test message", sender, timestamp, 
                          Arrays.asList("TELEGRAM"));
        
        System.out.println("\n=== Adapter Pattern Benefits ===");
        System.out.println("✓ Allows incompatible interfaces to work together");
        System.out.println("✓ Reuses existing code without modification");
        System.out.println("✓ Separates interface conversion from business logic");
        System.out.println("✓ Makes legacy systems compatible with new architecture");
        System.out.println("✓ Provides unified interface for different implementations");
    }
}
