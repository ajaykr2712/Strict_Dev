/**
 * AI Assistant and Chatbot Architecture Example
 * Demonstrates how to integrate AI/LLM capabilities with existing design patterns
 * for secure, scalable chatbot systems.
 */

import java.util.*;
import java.util.concurrent.*;
import java.time.Instant;

public class AIAssistantArchitectureExample {

    // Strategy Pattern for Different AI Providers
    interface AIProvider {
        AIResponse processQuery(String query, AIContext context);
        boolean isAvailable();
        double getCostPerRequest();
        String getProviderName();
    }

    // Chain of Responsibility for AI Request Processing
    abstract static class AIRequestHandler {
        protected AIRequestHandler nextHandler;
        
        public void setNext(AIRequestHandler handler) {
            this.nextHandler = handler;
        }
        
        public abstract AIRequestResult handle(AIRequest request);
        
        protected AIRequestResult passToNext(AIRequest request) {
            if (nextHandler != null) {
                return nextHandler.handle(request);
            }
            return AIRequestResult.unhandled();
        }
    }

    // Observer Pattern for AI Events
    interface AIEventObserver {
        void onAIResponse(AIResponseEvent event);
        void onSecurityAlert(SecurityEvent event);
        void onPerformanceMetric(PerformanceEvent event);
    }

    // State Pattern for Conversation Management
    interface ConversationState {
        void handleMessage(ConversationContext context, String message);
        String getStateName();
    }

    // Security Handler
    static class SecurityValidationHandler extends AIRequestHandler {
        private final RateLimiter rateLimiter = new RateLimiter(100);
        
        @Override
        public AIRequestResult handle(AIRequest request) {
            // Rate limiting
            if (!rateLimiter.allowRequest(request.getUserId())) {
                return AIRequestResult.rejected("Rate limit exceeded");
            }
            
            // Input validation
            if (request.getQuery() == null || request.getQuery().length() > 10000) {
                return AIRequestResult.rejected("Invalid input");
            }
            
            // Content filtering
            if (containsProhibitedContent(request.getQuery())) {
                return AIRequestResult.rejected("Prohibited content detected");
            }
            
            return passToNext(request);
        }
        
        private boolean containsProhibitedContent(String query) {
            String[] prohibitedTerms = {"malicious", "harmful", "illegal"};
            String lowercaseQuery = query.toLowerCase();
            return Arrays.stream(prohibitedTerms)
                .anyMatch(lowercaseQuery::contains);
        }
    }

    // Content Moderation Handler
    static class ContentModerationHandler extends AIRequestHandler {
        @Override
        public AIRequestResult handle(AIRequest request) {
            // AI-powered content moderation
            ContentModerationResult moderation = moderateContent(request.getQuery());
            
            if (!moderation.isAllowed()) {
                return AIRequestResult.rejected("Content moderation failed: " + moderation.getReason());
            }
            
            return passToNext(request);
        }
        
        private ContentModerationResult moderateContent(String content) {
            // Simulate AI content moderation
            if (content.toLowerCase().contains("inappropriate")) {
                return new ContentModerationResult(false, "Inappropriate content detected");
            }
            return new ContentModerationResult(true, "Content approved");
        }
    }

    // AI Processing Handler
    static class AIProcessingHandler extends AIRequestHandler {
        private final AIProviderManager providerManager = new AIProviderManager();
        
        @Override
        public AIRequestResult handle(AIRequest request) {
            try {
                AIProvider provider = providerManager.selectProvider(request);
                AIContext context = createAIContext(request);
                
                AIResponse response = provider.processQuery(request.getQuery(), context);
                
                return AIRequestResult.success(response);
            } catch (Exception e) {
                return AIRequestResult.error("AI processing failed: " + e.getMessage());
            }
        }
        
        private AIContext createAIContext(AIRequest request) {
            return new AIContext(request.getUserId(), request.getSessionId(), 
                               request.getConversationHistory());
        }
    }

    // AI Provider Manager
    static class AIProviderManager {
        private final List<AIProvider> providers;
        
        public AIProviderManager() {
            this.providers = Arrays.asList(
                new OpenAIProvider(),
                new LocalModelProvider(),
                new FallbackProvider()
            );
        }
        
        public AIProvider selectProvider(AIRequest request) {
            // Select based on availability, cost, and requirements
            return providers.stream()
                .filter(AIProvider::isAvailable)
                .min(Comparator.comparing(AIProvider::getCostPerRequest))
                .orElseGet(FallbackProvider::new);
        }
    }

    // OpenAI Provider Implementation
    static class OpenAIProvider implements AIProvider {
        @Override
        public AIResponse processQuery(String query, AIContext context) {
            // Simulate OpenAI API call
            String response = generateOpenAIResponse(query, context);
            return new AIResponse(response, "OpenAI", 0.002, true);
        }
        
        @Override
        public boolean isAvailable() {
            return true; // Simulate API availability check
        }
        
        @Override
        public double getCostPerRequest() {
            return 0.002;
        }
        
        @Override
        public String getProviderName() {
            return "OpenAI";
        }
        
        private String generateOpenAIResponse(String query, AIContext context) {
            // Simulate AI response generation
            return "AI Response: Based on your query '" + query + "', here's a helpful response.";
        }
    }

    // Local Model Provider
    static class LocalModelProvider implements AIProvider {
        @Override
        public AIResponse processQuery(String query, AIContext context) {
            String response = generateLocalResponse(query);
            return new AIResponse(response, "LocalModel", 0.0, true);
        }
        
        @Override
        public boolean isAvailable() {
            return true; // Local model always available
        }
        
        @Override
        public double getCostPerRequest() {
            return 0.0; // No external cost
        }
        
        @Override
        public String getProviderName() {
            return "LocalModel";
        }
        
        private String generateLocalResponse(String query) {
            return "Local AI Response: Processing your query about '" + query + "' locally.";
        }
    }

    // Fallback Provider
    static class FallbackProvider implements AIProvider {
        private final List<String> fallbackResponses = Arrays.asList(
            "I'm currently experiencing technical difficulties. Please try again later.",
            "I apologize, but I'm unable to process your request at this time.",
            "Our AI services are temporarily unavailable. Thank you for your patience."
        );
        
        @Override
        public AIResponse processQuery(String query, AIContext context) {
            Random random = new Random();
            String response = fallbackResponses.get(random.nextInt(fallbackResponses.size()));
            return new AIResponse(response, "Fallback", 0.0, true);
        }
        
        @Override
        public boolean isAvailable() {
            return true;
        }
        
        @Override
        public double getCostPerRequest() {
            return 0.0;
        }
        
        @Override
        public String getProviderName() {
            return "Fallback";
        }
    }

    // Conversation State Implementations
    static class InitialConversationState implements ConversationState {
        @Override
        public void handleMessage(ConversationContext context, String message) {
            if (isGreeting(message)) {
                context.addResponse("Hello! How can I assist you today?");
                context.setState(new ActiveConversationState());
            } else {
                context.setState(new ActiveConversationState());
                context.getCurrentState().handleMessage(context, message);
            }
        }
        
        @Override
        public String getStateName() {
            return "Initial";
        }
        
        private boolean isGreeting(String message) {
            String[] greetings = {"hello", "hi", "hey", "greetings"};
            String lowerMessage = message.toLowerCase();
            return Arrays.stream(greetings).anyMatch(lowerMessage::contains);
        }
    }

    static class ActiveConversationState implements ConversationState {
        @Override
        public void handleMessage(ConversationContext context, String message) {
            if (isEndConversation(message)) {
                context.addResponse("Thank you for chatting! Have a great day!");
                context.setState(new EndedConversationState());
            } else {
                // Process normal conversation
                String response = processConversationMessage(message, context);
                context.addResponse(response);
            }
        }
        
        @Override
        public String getStateName() {
            return "Active";
        }
        
        private boolean isEndConversation(String message) {
            String[] endings = {"goodbye", "bye", "thanks", "end"};
            String lowerMessage = message.toLowerCase();
            return Arrays.stream(endings).anyMatch(lowerMessage::contains);
        }
        
        private String processConversationMessage(String message, ConversationContext context) {
            // This would integrate with the AI processing pipeline
            return "I understand you're asking about: " + message + ". Let me help you with that.";
        }
    }

    static class EndedConversationState implements ConversationState {
        @Override
        public void handleMessage(ConversationContext context, String message) {
            if (isRestartConversation(message)) {
                context.setState(new InitialConversationState());
                context.getCurrentState().handleMessage(context, message);
            } else {
                context.addResponse("Our conversation has ended. Say 'hello' to start a new conversation.");
            }
        }
        
        @Override
        public String getStateName() {
            return "Ended";
        }
        
        private boolean isRestartConversation(String message) {
            String[] starters = {"hello", "hi", "start", "new"};
            String lowerMessage = message.toLowerCase();
            return Arrays.stream(starters).anyMatch(lowerMessage::contains);
        }
    }

    // Main AI Assistant Service
    static class AIAssistantService {
        private final AIRequestHandler handlerChain;
        private final ConversationManager conversationManager;
        private final List<AIEventObserver> observers;
        
        public AIAssistantService() {
            this.handlerChain = buildHandlerChain();
            this.conversationManager = new ConversationManager();
            this.observers = new ArrayList<>();
        }
        
        public AssistantResponse processMessage(String userId, String sessionId, String message) {
            try {
                // Get or create conversation context
                ConversationContext conversation = conversationManager.getConversation(userId, sessionId);
                
                // Add user message to conversation
                conversation.addUserMessage(message);
                
                // Handle conversation state
                conversation.getCurrentState().handleMessage(conversation, message);
                
                // Create AI request
                AIRequest aiRequest = new AIRequest(userId, sessionId, message, 
                                                   conversation.getConversationHistory());
                
                // Process through handler chain
                AIRequestResult result = handlerChain.handle(aiRequest);
                
                if (result.isSuccess()) {
                    AIResponse aiResponse = result.getResponse();
                    
                    // Add AI response to conversation
                    conversation.addAIResponse(aiResponse.getContent());
                    
                    // Notify observers
                    notifyObservers(new AIResponseEvent(userId, aiResponse));
                    
                    return AssistantResponse.success(aiResponse.getContent());
                } else {
                    return AssistantResponse.error(result.getErrorMessage());
                }
                
            } catch (Exception e) {
                return AssistantResponse.error("Processing failed: " + e.getMessage());
            }
        }
        
        private AIRequestHandler buildHandlerChain() {
            SecurityValidationHandler securityHandler = new SecurityValidationHandler();
            ContentModerationHandler moderationHandler = new ContentModerationHandler();
            AIProcessingHandler processingHandler = new AIProcessingHandler();
            
            securityHandler.setNext(moderationHandler);
            moderationHandler.setNext(processingHandler);
            
            return securityHandler;
        }
        
        public void addObserver(AIEventObserver observer) {
            observers.add(observer);
        }
        
        private void notifyObservers(AIResponseEvent event) {
            observers.forEach(observer -> observer.onAIResponse(event));
        }
    }

    // Demo and Test
    public static void main(String[] args) {
        System.out.println("=== AI Assistant Architecture Demo ===\n");
        
        AIAssistantService assistant = new AIAssistantService();
        
        // Add observer
        assistant.addObserver(new AIEventObserver() {
            @Override
            public void onAIResponse(AIResponseEvent event) {
                System.out.println("[OBSERVER] AI response generated: " + 
                                 event.getResponse().getProvider());
            }
            
            @Override
            public void onSecurityAlert(SecurityEvent event) {
                System.out.println("[OBSERVER] Security alert: " + event.getType());
            }
            
            @Override
            public void onPerformanceMetric(PerformanceEvent event) {
                System.out.println("[OBSERVER] Performance metric: " + event.getMetric());
            }
        });
        
        String userId = "user123";
        String sessionId = "session456";
        
        // Test conversation flow
        String[] testMessages = {
            "Hello!",
            "Can you help me understand design patterns?",
            "What is the Observer pattern?",
            "How do I implement a secure chatbot?",
            "Thanks for your help!",
            "Goodbye!"
        };
        
        System.out.println("Starting AI Assistant conversation...\n");
        
        for (String message : testMessages) {
            System.out.println("User: " + message);
            AssistantResponse response = assistant.processMessage(userId, sessionId, message);
            System.out.println("Assistant: " + response.getMessage());
            System.out.println();
            
            // Small delay between messages
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Test rate limiting
        System.out.println("Testing rate limiting...");
        for (int i = 0; i < 105; i++) {
            AssistantResponse response = assistant.processMessage(userId, sessionId, "Test " + i);
            if (!response.isSuccess()) {
                System.out.println("Rate limit triggered at request " + i);
                break;
            }
        }
    }
}

// Supporting Classes and Data Structures

class AIRequest {
    private final String userId;
    private final String sessionId;
    private final String query;
    private final List<String> conversationHistory;
    
    public AIRequest(String userId, String sessionId, String query, List<String> history) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.query = query;
        this.conversationHistory = history != null ? new ArrayList<>(history) : new ArrayList<>();
    }
    
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public String getQuery() { return query; }
    public List<String> getConversationHistory() { return conversationHistory; }
}

class AIResponse {
    private final String content;
    private final String provider;
    private final double cost;
    private final boolean safe;
    private final Instant timestamp;
    
    public AIResponse(String content, String provider, double cost, boolean safe) {
        this.content = content;
        this.provider = provider;
        this.cost = cost;
        this.safe = safe;
        this.timestamp = Instant.now();
    }
    
    public String getContent() { return content; }
    public String getProvider() { return provider; }
    public double getCost() { return cost; }
    public boolean isSafe() { return safe; }
    public Instant getTimestamp() { return timestamp; }
}

class AIContext {
    private final String userId;
    private final String sessionId;
    private final List<String> conversationHistory;
    
    public AIContext(String userId, String sessionId, List<String> history) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.conversationHistory = history != null ? new ArrayList<>(history) : new ArrayList<>();
    }
    
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public List<String> getConversationHistory() { return conversationHistory; }
}

class AIRequestResult {
    private final boolean success;
    private final AIResponse response;
    private final String errorMessage;
    private final String status;
    
    private AIRequestResult(boolean success, AIResponse response, String errorMessage, String status) {
        this.success = success;
        this.response = response;
        this.errorMessage = errorMessage;
        this.status = status;
    }
    
    public static AIRequestResult success(AIResponse response) {
        return new AIRequestResult(true, response, null, "SUCCESS");
    }
    
    public static AIRequestResult rejected(String reason) {
        return new AIRequestResult(false, null, reason, "REJECTED");
    }
    
    public static AIRequestResult error(String error) {
        return new AIRequestResult(false, null, error, "ERROR");
    }
    
    public static AIRequestResult unhandled() {
        return new AIRequestResult(false, null, "Request unhandled", "UNHANDLED");
    }
    
    public boolean isSuccess() { return success; }
    public AIResponse getResponse() { return response; }
    public String getErrorMessage() { return errorMessage; }
    public String getStatus() { return status; }
}

class AssistantResponse {
    private final boolean success;
    private final String message;
    
    private AssistantResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public static AssistantResponse success(String message) {
        return new AssistantResponse(true, message);
    }
    
    public static AssistantResponse error(String message) {
        return new AssistantResponse(false, message);
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}

class ConversationContext {
    private final String userId;
    private final String sessionId;
    private final List<String> conversationHistory;
    private AIAssistantArchitectureExample.ConversationState currentState;
    private final Instant createdAt;
    
    public ConversationContext(String userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.conversationHistory = new ArrayList<>();
        this.currentState = new AIAssistantArchitectureExample.InitialConversationState();
        this.createdAt = Instant.now();
    }
    
    public void addUserMessage(String message) {
        conversationHistory.add("User: " + message);
    }
    
    public void addResponse(String response) {
        conversationHistory.add("Assistant: " + response);
    }
    
    public void addAIResponse(String response) {
        conversationHistory.add("AI: " + response);
    }
    
    public void setState(AIAssistantArchitectureExample.ConversationState state) {
        this.currentState = state;
    }
    
    public AIAssistantArchitectureExample.ConversationState getCurrentState() { return currentState; }
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public List<String> getConversationHistory() { return new ArrayList<>(conversationHistory); }
    public Instant getCreatedAt() { return createdAt; }
}

class ConversationManager {
    private final Map<String, ConversationContext> activeConversations = new ConcurrentHashMap<>();
    
    public ConversationContext getConversation(String userId, String sessionId) {
        String key = userId + ":" + sessionId;
        return activeConversations.computeIfAbsent(key, 
            k -> new ConversationContext(userId, sessionId));
    }
    
    public void endConversation(String userId, String sessionId) {
        String key = userId + ":" + sessionId;
        activeConversations.remove(key);
    }
}

class ContentModerationResult {
    private final boolean allowed;
    private final String reason;
    
    public ContentModerationResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason;
    }
    
    public boolean isAllowed() { return allowed; }
    public String getReason() { return reason; }
}

class RateLimiter {
    private final int maxRequests;
    private final Map<String, Queue<Instant>> requestWindows = new ConcurrentHashMap<>();
    
    public RateLimiter(int maxRequests) {
        this.maxRequests = maxRequests;
    }
    
    public boolean allowRequest(String userId) {
        Queue<Instant> userRequests = requestWindows.computeIfAbsent(userId, k -> new LinkedList<>());
        
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(60); // 1-minute window
        
        // Remove old requests
        userRequests.removeIf(timestamp -> timestamp.isBefore(windowStart));
        
        if (userRequests.size() < maxRequests) {
            userRequests.offer(now);
            return true;
        }
        
        return false;
    }
}

// Event Classes for Observer Pattern
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

class SecurityEvent {
    private final String type;
    private final String details;
    private final Instant timestamp;
    
    public SecurityEvent(String type, String details) {
        this.type = type;
        this.details = details;
        this.timestamp = Instant.now();
    }
    
    public String getType() { return type; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }
}

class PerformanceEvent {
    private final String metric;
    private final double value;
    private final Instant timestamp;
    
    public PerformanceEvent(String metric, double value) {
        this.metric = metric;
        this.value = value;
        this.timestamp = Instant.now();
    }
    
    public String getMetric() { return metric; }
    public double getValue() { return value; }
    public Instant getTimestamp() { return timestamp; }
}
