import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Chain of Responsibility Pattern Implementation for Content Moderation System
 * 
 * Real-world Use Case: Multi-platform content moderation (Netflix, Uber, WhatsApp)
 * - Automated spam detection
 * - Profanity filtering
 * - Violence/inappropriate content detection
 * - Copyright violation checks
 * - Community guidelines enforcement
 * - Human moderator escalation
 * 
 * This demonstrates how requests can be passed through a chain of handlers,
 * where each handler decides whether to process the request or pass it to the next handler.
 */

// Handler interface - defines the contract for all moderation handlers
interface ContentModerationHandler {
    void setNext(ContentModerationHandler nextHandler);
    ModerationResult handleContent(ContentModerationRequest request);
    String getHandlerName();
    int getPriority(); // Lower number = higher priority
}

// Request object containing content to be moderated
class ContentModerationRequest {
    private final String contentId;
    private final String userId;
    private final String platform; // NETFLIX, UBER, WHATSAPP
    private final String contentType; // COMMENT, REVIEW, MESSAGE, VIDEO_TITLE, etc.
    private final String content;
    private final LocalDateTime submittedAt;
    private final Map<String, Object> metadata;

    public ContentModerationRequest(String contentId, String userId, String platform, 
                                  String contentType, String content, Map<String, Object> metadata) {
        this.contentId = contentId;
        this.userId = userId;
        this.platform = platform;
        this.contentType = contentType;
        this.content = content;
        this.submittedAt = LocalDateTime.now();
        this.metadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
    }

    // Getters
    public String getContentId() { return contentId; }
    public String getUserId() { return userId; }
    public String getPlatform() { return platform; }
    public String getContentType() { return contentType; }
    public String getContent() { return content; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }

    @Override
    public String toString() {
        return String.format("ContentRequest{id='%s', platform='%s', type='%s', content='%s'}", 
                contentId, platform, contentType, content.length() > 50 ? 
                content.substring(0, 47) + "..." : content);
    }
}

// Result object from moderation process
class ModerationResult {
    private final boolean isApproved;
    private final String action; // APPROVE, REJECT, ESCALATE, REQUIRE_REVIEW
    private final String reason;
    private final List<String> violations;
    private final String handledBy;
    private final int confidenceScore; // 0-100
    private final Map<String, Object> additionalData;

    public ModerationResult(boolean isApproved, String action, String reason, 
                          List<String> violations, String handledBy, int confidenceScore) {
        this.isApproved = isApproved;
        this.action = action;
        this.reason = reason;
        this.violations = new ArrayList<>(violations != null ? violations : new ArrayList<>());
        this.handledBy = handledBy;
        this.confidenceScore = confidenceScore;
        this.additionalData = new HashMap<>();
    }

    // Getters
    public boolean isApproved() { return isApproved; }
    public String getAction() { return action; }
    public String getReason() { return reason; }
    public List<String> getViolations() { return new ArrayList<>(violations); }
    public String getHandledBy() { return handledBy; }
    public int getConfidenceScore() { return confidenceScore; }
    public Map<String, Object> getAdditionalData() { return new HashMap<>(additionalData); }

    public void addAdditionalData(String key, Object value) {
        additionalData.put(key, value);
    }

    @Override
    public String toString() {
        return String.format("ModerationResult{approved=%s, action='%s', reason='%s', handledBy='%s', confidence=%d%%}", 
                isApproved, action, reason, handledBy, confidenceScore);
    }
}

// Abstract base handler with common functionality
abstract class AbstractModerationHandler implements ContentModerationHandler {
    private ContentModerationHandler nextHandler;
    protected final String handlerName;

    public AbstractModerationHandler(String handlerName) {
        this.handlerName = handlerName;
    }

    @Override
    public void setNext(ContentModerationHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public ModerationResult handleContent(ContentModerationRequest request) {
        ModerationResult result = processContent(request);
        
        if (result != null) {
            System.out.println("[" + handlerName + "] Processed: " + result);
            return result;
        }
        
        if (nextHandler != null) {
            System.out.println("[" + handlerName + "] Passing to next handler...");
            return nextHandler.handleContent(request);
        }
        
        // Default approval if no handler processed it
        System.out.println("[" + handlerName + "] End of chain - defaulting to approval");
        return new ModerationResult(true, "APPROVE", "No violations detected", 
                                  new ArrayList<>(), handlerName, 85);
    }

    @Override
    public String getHandlerName() {
        return handlerName;
    }

    // Abstract method for specific processing logic
    protected abstract ModerationResult processContent(ContentModerationRequest request);
}

// Concrete Handler - Spam Detection
class SpamDetectionHandler extends AbstractModerationHandler {
    private final Set<String> spamKeywords;
    private final Pattern repetitivePattern;
    private final Pattern urlPattern;

    public SpamDetectionHandler() {
        super("SpamDetector");
        this.spamKeywords = Set.of("click here", "free money", "urgent", "limited time", 
                                  "act now", "guaranteed", "no risk", "100% free");
        this.repetitivePattern = Pattern.compile("(.)\\1{4,}"); // 5+ consecutive same characters
        this.urlPattern = Pattern.compile("https?://[\\w.-]+");
    }

    @Override
    protected ModerationResult processContent(ContentModerationRequest request) {
        String content = request.getContent().toLowerCase();
        List<String> violations = new ArrayList<>();
        
        // Check for spam keywords
        for (String keyword : spamKeywords) {
            if (content.contains(keyword)) {
                violations.add("Spam keyword detected: " + keyword);
            }
        }
        
        // Check for repetitive characters
        if (repetitivePattern.matcher(content).find()) {
            violations.add("Excessive repetitive characters");
        }
        
        // Check for excessive URLs (suspicious for spam)
        long urlCount = urlPattern.matcher(content).results().count();
        if (urlCount > 3) {
            violations.add("Excessive URLs detected: " + urlCount);
        }
        
        // Check for ALL CAPS (potential spam indicator)
        if (content.length() > 20 && content.equals(content.toUpperCase())) {
            violations.add("Excessive capitalization");
        }
        
        if (!violations.isEmpty()) {
            return new ModerationResult(false, "REJECT", "Spam content detected", 
                                      violations, handlerName, 90);
        }
        
        return null; // Pass to next handler
    }

    @Override
    public int getPriority() {
        return 1; // High priority - catch spam early
    }
}

// Concrete Handler - Profanity Filter
class ProfanityFilterHandler extends AbstractModerationHandler {
    private final Set<String> profanityWords;
    private final Set<String> mildProfanity;

    public ProfanityFilterHandler() {
        super("ProfanityFilter");
        this.profanityWords = Set.of("badword1", "offensive", "hate", "inappropriate");
        this.mildProfanity = Set.of("damn", "hell", "crap");
    }

    @Override
    protected ModerationResult processContent(ContentModerationRequest request) {
        String content = request.getContent().toLowerCase();
        List<String> violations = new ArrayList<>();
        
        // Check for severe profanity
        for (String word : profanityWords) {
            if (content.contains(word)) {
                violations.add("Profanity detected: " + word);
            }
        }
        
        // Check for mild profanity
        List<String> mildViolations = new ArrayList<>();
        for (String word : mildProfanity) {
            if (content.contains(word)) {
                mildViolations.add("Mild profanity: " + word);
            }
        }
        
        if (!violations.isEmpty()) {
            return new ModerationResult(false, "REJECT", "Severe profanity detected", 
                                      violations, handlerName, 95);
        }
        
        if (!mildViolations.isEmpty()) {
            // Platform-specific handling
            if ("WHATSAPP".equals(request.getPlatform())) {
                // WhatsApp might be more lenient for private messages
                return new ModerationResult(true, "APPROVE_WITH_WARNING", 
                                          "Mild profanity in private message", 
                                          mildViolations, handlerName, 70);
            } else {
                // Netflix/Uber reviews might have stricter policies
                return new ModerationResult(false, "REQUIRE_REVIEW", 
                                          "Mild profanity requires human review", 
                                          mildViolations, handlerName, 60);
            }
        }
        
        return null; // Pass to next handler
    }

    @Override
    public int getPriority() {
        return 2; // High priority
    }
}

// Concrete Handler - Violence and Inappropriate Content
class ViolenceDetectionHandler extends AbstractModerationHandler {
    private final Set<String> violenceKeywords;
    private final Set<String> threateningWords;

    public ViolenceDetectionHandler() {
        super("ViolenceDetector");
        this.violenceKeywords = Set.of("violence", "kill", "murder", "attack", "hurt", "harm");
        this.threateningWords = Set.of("threat", "threaten", "will hurt", "watch out", "get you");
    }

    @Override
    protected ModerationResult processContent(ContentModerationRequest request) {
        String content = request.getContent().toLowerCase();
        List<String> violations = new ArrayList<>();
        
        // Check for violence keywords
        for (String keyword : violenceKeywords) {
            if (content.contains(keyword)) {
                violations.add("Violence-related content: " + keyword);
            }
        }
        
        // Check for threatening language
        for (String threat : threateningWords) {
            if (content.contains(threat)) {
                violations.add("Threatening language: " + threat);
            }
        }
        
        if (!violations.isEmpty()) {
            // Violence is always escalated for human review
            return new ModerationResult(false, "ESCALATE", 
                                      "Violence/threatening content requires immediate attention", 
                                      violations, handlerName, 98);
        }
        
        return null; // Pass to next handler
    }

    @Override
    public int getPriority() {
        return 0; // Highest priority - safety first
    }
}

// Concrete Handler - Platform-Specific Guidelines
class PlatformGuidelinesHandler extends AbstractModerationHandler {
    
    public PlatformGuidelinesHandler() {
        super("PlatformGuidelines");
    }

    @Override
    protected ModerationResult processContent(ContentModerationRequest request) {
        String platform = request.getPlatform();
        String contentType = request.getContentType();
        String content = request.getContent();
        List<String> violations = new ArrayList<>();
        
        switch (platform) {
            case "NETFLIX":
                violations.addAll(checkNetflixGuidelines(content, contentType));
                break;
            case "UBER":
                violations.addAll(checkUberGuidelines(content, contentType));
                break;
            case "WHATSAPP":
                violations.addAll(checkWhatsAppGuidelines(content, contentType));
                break;
        }
        
        if (!violations.isEmpty()) {
            return new ModerationResult(false, "REQUIRE_REVIEW", 
                                      "Platform guideline violations", 
                                      violations, handlerName, 75);
        }
        
        return null; // Pass to next handler
    }

    private List<String> checkNetflixGuidelines(String content, String contentType) {
        List<String> violations = new ArrayList<>();
        
        // Netflix-specific rules for reviews and comments
        if ("REVIEW".equals(contentType)) {
            if (content.length() < 10) {
                violations.add("Review too short (minimum 10 characters)");
            }
            if (content.toLowerCase().contains("spoiler") && !content.toLowerCase().contains("spoiler alert")) {
                violations.add("Potential spoiler without warning");
            }
        }
        
        return violations;
    }

    private List<String> checkUberGuidelines(String content, String contentType) {
        List<String> violations = new ArrayList<>();
        
        // Uber-specific rules for driver/rider feedback
        if ("FEEDBACK".equals(contentType)) {
            if (content.toLowerCase().contains("personal information")) {
                violations.add("Contains personal information sharing");
            }
            if (content.toLowerCase().contains("tip") && content.toLowerCase().contains("cash")) {
                violations.add("References cash tips (against platform policy)");
            }
        }
        
        return violations;
    }

    private List<String> checkWhatsAppGuidelines(String content, String contentType) {
        List<String> violations = new ArrayList<>();
        
        // WhatsApp-specific rules
        if ("MESSAGE".equals(contentType)) {
            // Check for forwarded chain messages
            if (content.toLowerCase().contains("forward this to") || 
                content.toLowerCase().contains("send this to 10 people")) {
                violations.add("Chain message detected");
            }
        }
        
        return violations;
    }

    @Override
    public int getPriority() {
        return 4; // Lower priority - platform-specific checks
    }
}

// Concrete Handler - Human Moderator (last resort)
class HumanModeratorHandler extends AbstractModerationHandler {
    
    public HumanModeratorHandler() {
        super("HumanModerator");
    }

    @Override
    protected ModerationResult processContent(ContentModerationRequest request) {
        // Simulate human moderator decision
        System.out.println("[" + handlerName + "] Escalating to human moderator for manual review");
        
        // In real implementation, this would queue the content for human review
        // For demo purposes, we'll simulate different outcomes based on content length
        boolean approved = request.getContent().length() < 100; // Shorter content more likely to be approved
        
        if (approved) {
            return new ModerationResult(true, "APPROVE", 
                                      "Approved by human moderator after review", 
                                      new ArrayList<>(), handlerName, 99);
        } else {
            return new ModerationResult(false, "REJECT", 
                                      "Rejected by human moderator", 
                                      Arrays.asList("Human moderator determined content violates guidelines"), 
                                      handlerName, 99);
        }
    }

    @Override
    public int getPriority() {
        return 10; // Lowest priority - last resort
    }
}

// Content Moderation System
class ContentModerationSystem {
    private ContentModerationHandler handlerChain;
    private final Map<String, Integer> moderationStats;

    public ContentModerationSystem() {
        this.moderationStats = new HashMap<>();
        setupModerationChain();
    }

    private void setupModerationChain() {
        // Create handlers
        List<ContentModerationHandler> handlers = Arrays.asList(
            new ViolenceDetectionHandler(),
            new SpamDetectionHandler(),
            new ProfanityFilterHandler(),
            new PlatformGuidelinesHandler(),
            new HumanModeratorHandler()
        );

        // Sort by priority (lower number = higher priority)
        handlers.sort(Comparator.comparingInt(ContentModerationHandler::getPriority));

        // Build the chain
        for (int i = 0; i < handlers.size() - 1; i++) {
            handlers.get(i).setNext(handlers.get(i + 1));
        }

        this.handlerChain = handlers.get(0);
        
        System.out.println("[MODERATION-SYSTEM] Chain established with order:");
        handlers.forEach(handler -> 
            System.out.println("  " + handler.getPriority() + ". " + handler.getHandlerName())
        );
    }

    public ModerationResult moderateContent(ContentModerationRequest request) {
        System.out.println("\n[MODERATION-SYSTEM] Processing: " + request);
        
        ModerationResult result = handlerChain.handleContent(request);
        
        // Update statistics
        moderationStats.merge(result.getAction(), 1, Integer::sum);
        
        System.out.println("[MODERATION-SYSTEM] Final result: " + result);
        return result;
    }

    public void printModerationStats() {
        System.out.println("\n=== Moderation Statistics ===");
        moderationStats.forEach((action, count) -> 
            System.out.println("  " + action + ": " + count + " cases")
        );
    }
}

// Demonstration class
class ChainOfResponsibilityDemo {
    public static void main(String[] args) {
        System.out.println("=== Chain of Responsibility Demo: Content Moderation ===\n");

        ContentModerationSystem moderationSystem = new ContentModerationSystem();

        // Test cases for different platforms and content types
        List<ContentModerationRequest> testRequests = Arrays.asList(
            // Normal content
            new ContentModerationRequest("content001", "user123", "NETFLIX", "REVIEW", 
                "Great movie! Really enjoyed the storyline and acting.", new HashMap<>()),
            
            // Spam content
            new ContentModerationRequest("content002", "user456", "UBER", "FEEDBACK", 
                "CLICK HERE FOR FREE MONEY!!! URGENT!!! LIMITED TIME!!!", new HashMap<>()),
            
            // Profanity content
            new ContentModerationRequest("content003", "user789", "WHATSAPP", "MESSAGE", 
                "This is damn frustrating, but I can handle it.", new HashMap<>()),
            
            // Violence content
            new ContentModerationRequest("content004", "user321", "NETFLIX", "COMMENT", 
                "I will hurt anyone who disagrees with me about this show!", new HashMap<>()),
            
            // Platform guideline violation
            new ContentModerationRequest("content005", "user654", "NETFLIX", "REVIEW", 
                "Bad movie! Contains spoiler: the main character dies at the end.", new HashMap<>()),
            
            // Chain message
            new ContentModerationRequest("content006", "user987", "WHATSAPP", "MESSAGE", 
                "Forward this to 10 people or you will have bad luck for 7 years!", new HashMap<>()),
            
            // Long content requiring human review
            new ContentModerationRequest("content007", "user111", "UBER", "FEEDBACK", 
                "This is a very long and detailed feedback about my experience with the driver. " +
                "I want to share personal information about our conversation during the ride. " +
                "The driver mentioned giving cash tips which I think might be against policy. " +
                "Overall, it was an interesting experience that I felt needed to be documented.", new HashMap<>())
        );

        System.out.println("Processing " + testRequests.size() + " content moderation requests:\n");

        // Process each request
        for (ContentModerationRequest request : testRequests) {
            ModerationResult result = moderationSystem.moderateContent(request);
            System.out.println("----------------------------------------");
        }

        // Show final statistics
        moderationSystem.printModerationStats();

        System.out.println("\n=== Chain of Responsibility Benefits Demonstrated ===");
        System.out.println("✓ Loose coupling: Handlers don't know about each other");
        System.out.println("✓ Dynamic chain: Handlers can be added/removed/reordered easily");
        System.out.println("✓ Single responsibility: Each handler focuses on one type of moderation");
        System.out.println("✓ Flexible processing: Request can be handled by any handler in the chain");
        System.out.println("✓ Guaranteed processing: Either handled by a specific handler or default behavior");
    }
}
