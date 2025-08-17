/**
 * Simplified Secure AI Chatbot Implementation Example
 * Demonstrates enterprise-grade chatbot architecture with security patterns
 */

import java.util.*;
import java.util.concurrent.*;
import java.time.Instant;

class SecureAIChatbotSimplified {

    // Observer pattern for chatbot events
    interface ChatbotEventObserver {
        void onUserInteraction(UserInteraction interaction);
        void onSecurityEvent(SecurityEvent event);
        void onAIResponse(AIResponseEvent event);
    }

    // Strategy pattern for different AI providers
    interface LLMStrategy {
        AIResponse generateResponse(String context, String userMessage);
        boolean canHandle(MessageType messageType);
        double getCost();
        String getProviderName();
    }

    // Chain of Responsibility for message processing
    abstract static class MessageProcessor {
        protected MessageProcessor nextProcessor;
        
        public void setNext(MessageProcessor processor) {
            this.nextProcessor = processor;
        }
        
        public abstract ProcessingResult process(UserMessage message, SecurityContext context);
        
        protected ProcessingResult passToNext(UserMessage message, SecurityContext context) {
            if (nextProcessor != null) {
                return nextProcessor.process(message, context);
            }
            return ProcessingResult.success();
        }
    }

    // Security validation processor
    static class SecurityValidationProcessor extends MessageProcessor {
        private final RateLimiter rateLimiter = new RateLimiter(100, 60);
        
        @Override
        public ProcessingResult process(UserMessage message, SecurityContext context) {
            // Rate limiting check
            if (!rateLimiter.allowRequest(context.getUserId())) {
                return ProcessingResult.failure("Rate limit exceeded");
            }
            
            // Input validation
            if (message.getContent() == null || message.getContent().length() > 5000) {
                return ProcessingResult.failure("Invalid input length");
            }
            
            // Basic security checks
            if (message.getContent().toLowerCase().contains("malicious")) {
                return ProcessingResult.failure("Malicious content detected");
            }
            
            return passToNext(message, context);
        }
    }

    // Content moderation processor
    static class ContentModerationProcessor extends MessageProcessor {
        
        @Override
        public ProcessingResult process(UserMessage message, SecurityContext context) {
            // Simulate content moderation
            String content = message.getContent().toLowerCase();
            
            if (content.contains("inappropriate") || content.contains("blocked")) {
                return ProcessingResult.failure("Content moderation failed");
            }
            
            return passToNext(message, context);
        }
    }

    // AI response generation processor
    static class AIResponseProcessor extends MessageProcessor {
        private final LLMOrchestrator llmOrchestrator = new LLMOrchestrator();
        
        @Override
        public ProcessingResult process(UserMessage message, SecurityContext context) {
            try {
                AIResponse response = llmOrchestrator.generateResponse(
                    context.getUserId(), message.getContent()
                );
                return ProcessingResult.success(response);
            } catch (Exception e) {
                return ProcessingResult.failure("AI processing failed: " + e.getMessage());
            }
        }
    }

    // LLM Orchestrator implementing multiple strategies
    static class LLMOrchestrator {
        private final List<LLMStrategy> strategies;
        
        public LLMOrchestrator() {
            this.strategies = Arrays.asList(
                new OpenAIStrategy(),
                new LocalModelStrategy(),
                new FallbackStrategy()
            );
        }
        
        public AIResponse generateResponse(String userId, String message) {
            MessageType messageType = classifyMessage(message);
            
            // Select appropriate strategy
            LLMStrategy selectedStrategy = selectStrategy(messageType);
            
            if (selectedStrategy == null) {
                selectedStrategy = new FallbackStrategy();
            }
            
            return selectedStrategy.generateResponse(userId, message);
        }
        
        private MessageType classifyMessage(String message) {
            if (message.toLowerCase().contains("code") || message.contains("```")) {
                return MessageType.CODE_RELATED;
            } else if (message.toLowerCase().contains("help") || message.contains("?")) {
                return MessageType.HELP_REQUEST;
            } else {
                return MessageType.GENERAL_CHAT;
            }
        }
        
        private LLMStrategy selectStrategy(MessageType messageType) {
            return strategies.stream()
                .filter(strategy -> strategy.canHandle(messageType))
                .min(Comparator.comparing(LLMStrategy::getCost))
                .orElse(null);
        }
    }

    // OpenAI strategy implementation
    static class OpenAIStrategy implements LLMStrategy {
        private final double costPerToken = 0.002;
        
        @Override
        public AIResponse generateResponse(String context, String userMessage) {
            // Simulate OpenAI API call
            String response = "OpenAI response to: " + userMessage;
            
            return AIResponse.builder()
                .content(response)
                .provider("OpenAI")
                .model("gpt-4")
                .tokens(response.split(" ").length)
                .cost(response.split(" ").length * costPerToken)
                .timestamp(Instant.now())
                .safe(true)
                .build();
        }
        
        @Override
        public boolean canHandle(MessageType messageType) {
            return true; // OpenAI can handle all message types
        }
        
        @Override
        public double getCost() {
            return costPerToken;
        }
        
        @Override
        public String getProviderName() {
            return "OpenAI";
        }
    }

    // Local model strategy for sensitive data
    static class LocalModelStrategy implements LLMStrategy {
        private final double costPerToken = 0.0; // No external cost
        
        @Override
        public AIResponse generateResponse(String context, String userMessage) {
            // Simulate local model processing
            String response = "Local model response to: " + userMessage;
            
            return AIResponse.builder()
                .content(response)
                .provider("Local")
                .model("local-llm")
                .tokens(response.split(" ").length)
                .cost(0.0)
                .timestamp(Instant.now())
                .safe(true)
                .build();
        }
        
        @Override
        public boolean canHandle(MessageType messageType) {
            return true;
        }
        
        @Override
        public double getCost() {
            return costPerToken;
        }
        
        @Override
        public String getProviderName() {
            return "Local";
        }
    }

    // Fallback strategy
    static class FallbackStrategy implements LLMStrategy {
        private final List<String> fallbackResponses = Arrays.asList(
            "I apologize, but I'm experiencing technical difficulties. Please try again later.",
            "I'm currently unable to process your request. Our team has been notified.",
            "Thank you for your patience. Our AI services are temporarily unavailable."
        );
        
        @Override
        public AIResponse generateResponse(String context, String userMessage) {
            Random random = new Random();
            String response = fallbackResponses.get(random.nextInt(fallbackResponses.size()));
            
            return AIResponse.builder()
                .content(response)
                .provider("Fallback")
                .model("fallback")
                .tokens(response.split(" ").length)
                .cost(0.0)
                .timestamp(Instant.now())
                .safe(true)
                .build();
        }
        
        @Override
        public boolean canHandle(MessageType messageType) {
            return true; // Fallback can handle anything
        }
        
        @Override
        public double getCost() {
            return 0.0;
        }
        
        @Override
        public String getProviderName() {
            return "Fallback";
        }
    }

    // Main chatbot service
    static class SecureAIChatbotService {
        private final MessageProcessor processingChain;
        private final List<ChatbotEventObserver> observers;
        
        public SecureAIChatbotService() {
            this.processingChain = buildProcessingChain();
            this.observers = new ArrayList<>();
        }
        
        public ChatbotResponse processMessage(String userId, String sessionId, String message) {
            try {
                // Create security context
                SecurityContext securityContext = new SecurityContext(userId, sessionId);
                
                // Create user message
                UserMessage userMessage = new UserMessage(message, Instant.now());
                
                // Process through chain
                ProcessingResult result = processingChain.process(userMessage, securityContext);
                
                if (!result.isSuccess()) {
                    return ChatbotResponse.error(result.getErrorMessage());
                }
                
                // Notify observers
                notifyObservers(new UserInteraction(userId, message, Instant.now()));
                
                AIResponse aiResponse = result.getAIResponse();
                if (aiResponse != null) {
                    notifyObservers(new AIResponseEvent(userId, aiResponse));
                    return ChatbotResponse.success(aiResponse.getContent());
                } else {
                    return ChatbotResponse.success("Message processed successfully");
                }
                
            } catch (Exception e) {
                System.err.println("Chatbot processing error: " + e.getMessage());
                return ChatbotResponse.error("I apologize, but I encountered an error processing your message.");
            }
        }
        
        private MessageProcessor buildProcessingChain() {
            SecurityValidationProcessor securityProcessor = new SecurityValidationProcessor();
            ContentModerationProcessor moderationProcessor = new ContentModerationProcessor();
            AIResponseProcessor aiProcessor = new AIResponseProcessor();
            
            securityProcessor.setNext(moderationProcessor);
            moderationProcessor.setNext(aiProcessor);
            
            return securityProcessor;
        }
        
        public void addObserver(ChatbotEventObserver observer) {
            observers.add(observer);
        }
        
        private void notifyObservers(Object event) {
            observers.forEach(observer -> {
                if (event instanceof UserInteraction) {
                    observer.onUserInteraction((UserInteraction) event);
                } else if (event instanceof SecurityEvent) {
                    observer.onSecurityEvent((SecurityEvent) event);
                } else if (event instanceof AIResponseEvent) {
                    observer.onAIResponse((AIResponseEvent) event);
                }
            });
        }
    }

    // Demo class
    public static void main(String[] args) {
        System.out.println("=== Secure AI Chatbot Demo ===\n");
        
        SecureAIChatbotService chatbot = new SecureAIChatbotService();
        
        // Add observers
        chatbot.addObserver(new ChatbotEventObserver() {
            @Override
            public void onUserInteraction(UserInteraction interaction) {
                System.out.println("[OBSERVER] User interaction: " + interaction.getMessage());
            }
            
            @Override
            public void onSecurityEvent(SecurityEvent event) {
                System.out.println("[OBSERVER] Security event: " + event.getType());
            }
            
            @Override
            public void onAIResponse(AIResponseEvent event) {
                System.out.println("[OBSERVER] AI response generated for user: " + event.getUserId());
            }
        });
        
        // Simulate conversation
        String userId = "user123";
        String sessionId = "session456";
        
        System.out.println("Starting conversation...\n");
        
        // Test messages
        String[] testMessages = {
            "Hello!",
            "Can you help me with Java programming?",
            "What are design patterns?",
            "Thanks for your help!",
            "Goodbye!"
        };
        
        for (String message : testMessages) {
            System.out.println("User: " + message);
            ChatbotResponse response = chatbot.processMessage(userId, sessionId, message);
            System.out.println("Bot: " + response.getMessage());
            System.out.println();
            
            // Simulate delay between messages
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Test rate limiting
        System.out.println("Testing rate limiting...");
        for (int i = 0; i < 105; i++) {
            ChatbotResponse response = chatbot.processMessage(userId, sessionId, "Test message " + i);
            if (!response.isSuccess()) {
                System.out.println("Rate limit hit at message " + i + ": " + response.getMessage());
                break;
            }
        }
    }
}

// Supporting data classes
class UserMessage {
    private final String content;
    private final Instant timestamp;
    
    public UserMessage(String content, Instant timestamp) {
        this.content = content;
        this.timestamp = timestamp;
    }
    
    public String getContent() { return content; }
    public Instant getTimestamp() { return timestamp; }
}

class AIResponse {
    private final String content;
    private final String provider;
    private final String model;
    private final int tokens;
    private final double cost;
    private final Instant timestamp;
    private final boolean safe;
    
    private AIResponse(Builder builder) {
        this.content = builder.content;
        this.provider = builder.provider;
        this.model = builder.model;
        this.tokens = builder.tokens;
        this.cost = builder.cost;
        this.timestamp = builder.timestamp;
        this.safe = builder.safe;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String content;
        private String provider;
        private String model;
        private int tokens;
        private double cost;
        private Instant timestamp;
        private boolean safe = true;
        
        public Builder content(String content) { this.content = content; return this; }
        public Builder provider(String provider) { this.provider = provider; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder tokens(int tokens) { this.tokens = tokens; return this; }
        public Builder cost(double cost) { this.cost = cost; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder safe(boolean safe) { this.safe = safe; return this; }
        
        public AIResponse build() {
            return new AIResponse(this);
        }
    }
    
    // Getters
    public String getContent() { return content; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public int getTokens() { return tokens; }
    public double getCost() { return cost; }
    public Instant getTimestamp() { return timestamp; }
    public boolean isSafe() { return safe; }
}

class ChatbotResponse {
    private final boolean success;
    private final String message;
    private final String errorCode;
    
    private ChatbotResponse(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
    }
    
    public static ChatbotResponse success(String message) {
        return new ChatbotResponse(true, message, null);
    }
    
    public static ChatbotResponse error(String message) {
        return new ChatbotResponse(false, message, "ERROR");
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
}

class ProcessingResult {
    private final boolean success;
    private final String errorMessage;
    private final AIResponse aiResponse;
    
    private ProcessingResult(boolean success, String errorMessage, AIResponse aiResponse) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.aiResponse = aiResponse;
    }
    
    public static ProcessingResult success() {
        return new ProcessingResult(true, null, null);
    }
    
    public static ProcessingResult success(AIResponse response) {
        return new ProcessingResult(true, null, response);
    }
    
    public static ProcessingResult failure(String errorMessage) {
        return new ProcessingResult(false, errorMessage, null);
    }
    
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public AIResponse getAIResponse() { return aiResponse; }
}

// Enum for message types
enum MessageType {
    GENERAL_CHAT,
    CODE_RELATED,
    HELP_REQUEST,
    COMPLEX_QUERY
}

// Event classes for observer pattern
class UserInteraction {
    private final String userId;
    private final String message;
    private final Instant timestamp;
    
    public UserInteraction(String userId, String message, Instant timestamp) {
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
}

class SecurityEvent {
    private final String type;
    private final String userId;
    private final String details;
    private final Instant timestamp;
    
    public SecurityEvent(String type, String userId, String details) {
        this.type = type;
        this.userId = userId;
        this.details = details;
        this.timestamp = Instant.now();
    }
    
    public String getType() { return type; }
    public String getUserId() { return userId; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }
}

class AIResponseEvent {
    private final String userId;
    private final AIResponse response;
    private final Instant timestamp;
    
    public AIResponseEvent(String userId, AIResponse response) {
        this.userId = userId;
        this.response = response;
        this.timestamp = Instant.now();
    }
    
    public String getUserId() { return userId; }
    public AIResponse getResponse() { return response; }
    public Instant getTimestamp() { return timestamp; }
}

class SecurityContext {
    private final String userId;
    private final String sessionId;
    
    public SecurityContext(String userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
    }
    
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
}

class RateLimiter {
    private final int maxRequests;
    private final int windowSeconds;
    private final Map<String, Queue<Instant>> requestWindows = new ConcurrentHashMap<>();
    
    public RateLimiter(int maxRequests, int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }
    
    public boolean allowRequest(String userId) {
        Queue<Instant> userRequests = requestWindows.computeIfAbsent(userId, k -> new LinkedList<>());
        
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);
        
        // Remove old requests
        userRequests.removeIf(timestamp -> timestamp.isBefore(windowStart));
        
        // Check if under limit
        if (userRequests.size() < maxRequests) {
            userRequests.offer(now);
            return true;
        }
        
        return false;
    }
}
