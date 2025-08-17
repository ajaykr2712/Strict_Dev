import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Circuit Breaker Pattern Implementation for Microservices
 * 
 * Real-world Use Case: Resilient service-to-service communication
 * - Netflix: Content recommendation service calling user preference service
 * - Uber: Ride matching service calling driver location service  
 * - WhatsApp: Message delivery service calling notification service
 * 
 * The Circuit Breaker prevents cascading failures by monitoring service calls
 * and "opening" when failures exceed a threshold, allowing services to fail fast
 * and recover gracefully.
 * 
 * States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Failures exceeded threshold, requests fail immediately
 * - HALF_OPEN: Testing if service has recovered
 */

// Circuit Breaker states
enum CircuitBreakerState {
    CLOSED,    // Normal operation
    OPEN,      // Failing fast
    HALF_OPEN  // Testing recovery
}

// Result of a service call
class ServiceCallResult<T> {
    private final boolean isSuccess;
    private final T data;
    private final Exception exception;
    private final long executionTimeMs;

    private ServiceCallResult(boolean isSuccess, T data, Exception exception, long executionTimeMs) {
        this.isSuccess = isSuccess;
        this.data = data;
        this.exception = exception;
        this.executionTimeMs = executionTimeMs;
    }

    public static <T> ServiceCallResult<T> success(T data, long executionTimeMs) {
        return new ServiceCallResult<>(true, data, null, executionTimeMs);
    }

    public static <T> ServiceCallResult<T> failure(Exception exception, long executionTimeMs) {
        return new ServiceCallResult<>(false, null, exception, executionTimeMs);
    }

    public boolean isSuccess() { return isSuccess; }
    public T getData() { return data; }
    public Exception getException() { return exception; }
    public long getExecutionTimeMs() { return executionTimeMs; }
}

// Circuit Breaker configuration
class CircuitBreakerConfig {
    private final int failureThreshold;
    private final Duration timeoutDuration;
    private final Duration recoveryTimeout;
    private final int halfOpenMaxCalls;
    private final int minimumCallsBeforeTrip;

    public CircuitBreakerConfig(int failureThreshold, Duration timeoutDuration, 
                               Duration recoveryTimeout, int halfOpenMaxCalls, 
                               int minimumCallsBeforeTrip) {
        this.failureThreshold = failureThreshold;
        this.timeoutDuration = timeoutDuration;
        this.recoveryTimeout = recoveryTimeout;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
        this.minimumCallsBeforeTrip = minimumCallsBeforeTrip;
    }

    public int getFailureThreshold() { return failureThreshold; }
    public Duration getTimeoutDuration() { return timeoutDuration; }
    public Duration getRecoveryTimeout() { return recoveryTimeout; }
    public int getHalfOpenMaxCalls() { return halfOpenMaxCalls; }
    public int getMinimumCallsBeforeTrip() { return minimumCallsBeforeTrip; }
}

// Circuit Breaker implementation
class CircuitBreaker {
    private final String serviceName;
    private final CircuitBreakerConfig config;
    private volatile CircuitBreakerState state;
    private final AtomicInteger failureCount;
    private final AtomicInteger successCount;
    private final AtomicInteger totalCalls;
    private final AtomicLong lastFailureTime;
    private final AtomicInteger halfOpenCalls;

    public CircuitBreaker(String serviceName, CircuitBreakerConfig config) {
        this.serviceName = serviceName;
        this.config = config;
        this.state = CircuitBreakerState.CLOSED;
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.totalCalls = new AtomicInteger(0);
        this.lastFailureTime = new AtomicLong(0);
        this.halfOpenCalls = new AtomicInteger(0);
    }

    public <T> ServiceCallResult<T> execute(Supplier<T> serviceCall, String operationName) {
        // Check if circuit breaker should allow the call
        if (!allowCall()) {
            return ServiceCallResult.failure(
                new RuntimeException("Circuit breaker is OPEN for service: " + serviceName), 0
            );
        }

        long startTime = System.currentTimeMillis();
        
        try {
            // Execute the service call with timeout
            T result = executeWithTimeout(serviceCall);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record success
            onSuccess();
            
            System.out.println("[CIRCUIT-BREAKER] SUCCESS - " + serviceName + "." + operationName + 
                             " (executed in " + executionTime + "ms)");
            
            return ServiceCallResult.success(result, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record failure
            onFailure();
            
            System.out.println("[CIRCUIT-BREAKER] FAILURE - " + serviceName + "." + operationName + 
                             " (" + e.getMessage() + ")");
            
            return ServiceCallResult.failure(e, executionTime);
        }
    }

    private <T> T executeWithTimeout(Supplier<T> serviceCall) throws Exception {
        // In a real implementation, this would use proper timeout mechanisms
        // For demo purposes, we'll simulate timeout by checking execution time
        long startTime = System.currentTimeMillis();
        
        try {
            T result = serviceCall.get();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > config.getTimeoutDuration().toMillis()) {
                throw new RuntimeException("Service call timeout after " + executionTime + "ms");
            }
            
            return result;
        } catch (RuntimeException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            if (executionTime > config.getTimeoutDuration().toMillis()) {
                throw new RuntimeException("Service call timeout after " + executionTime + "ms");
            }
            throw e;
        }
    }

    private boolean allowCall() {
        switch (state) {
            case CLOSED:
                return true;
                
            case OPEN:
                // Check if recovery timeout has passed
                if (isRecoveryTimeoutPassed()) {
                    transitionToHalfOpen();
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                // Allow limited calls to test recovery
                return halfOpenCalls.get() < config.getHalfOpenMaxCalls();
                
            default:
                return false;
        }
    }

    private void onSuccess() {
        successCount.incrementAndGet();
        totalCalls.incrementAndGet();
        
        if (state == CircuitBreakerState.HALF_OPEN) {
            int currentHalfOpenCalls = halfOpenCalls.incrementAndGet();
            
            // If enough successful calls in half-open state, transition to closed
            if (currentHalfOpenCalls >= config.getHalfOpenMaxCalls()) {
                transitionToClosed();
            }
        }
        
        // Reset failure count on success
        failureCount.set(0);
    }

    private void onFailure() {
        int currentFailures = failureCount.incrementAndGet();
        totalCalls.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (state == CircuitBreakerState.HALF_OPEN) {
            // Any failure in half-open state goes back to open
            transitionToOpen();
        } else if (state == CircuitBreakerState.CLOSED) {
            // Check if we should trip the circuit breaker
            int totalCallsCount = totalCalls.get();
            if (totalCallsCount >= config.getMinimumCallsBeforeTrip() &&
                currentFailures >= config.getFailureThreshold()) {
                transitionToOpen();
            }
        }
    }

    private void transitionToOpen() {
        if (state != CircuitBreakerState.OPEN) {
            state = CircuitBreakerState.OPEN;
            halfOpenCalls.set(0);
            System.out.println("[CIRCUIT-BREAKER] " + serviceName + " - State changed to OPEN " +
                             "(failures: " + failureCount.get() + "/" + config.getFailureThreshold() + ")");
        }
    }

    private void transitionToHalfOpen() {
        if (state == CircuitBreakerState.OPEN) {
            state = CircuitBreakerState.HALF_OPEN;
            halfOpenCalls.set(0);
            System.out.println("[CIRCUIT-BREAKER] " + serviceName + " - State changed to HALF_OPEN " +
                             "(testing recovery)");
        }
    }

    private void transitionToClosed() {
        state = CircuitBreakerState.CLOSED;
        failureCount.set(0);
        successCount.set(0);
        totalCalls.set(0);
        halfOpenCalls.set(0);
        System.out.println("[CIRCUIT-BREAKER] " + serviceName + " - State changed to CLOSED " +
                         "(service recovered)");
    }

    private boolean isRecoveryTimeoutPassed() {
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        return timeSinceLastFailure >= config.getRecoveryTimeout().toMillis();
    }

    public CircuitBreakerState getState() {
        return state;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("serviceName", serviceName);
        metrics.put("state", state.toString());
        metrics.put("failureCount", failureCount.get());
        metrics.put("successCount", successCount.get());
        metrics.put("totalCalls", totalCalls.get());
        metrics.put("halfOpenCalls", halfOpenCalls.get());
        metrics.put("failureRate", totalCalls.get() > 0 ? 
                   (double) failureCount.get() / totalCalls.get() * 100 : 0.0);
        return metrics;
    }
}

// Service simulation classes
abstract class ExternalService {
    protected final String serviceName;
    protected double failureRate;
    protected int responseTimeMs;

    public ExternalService(String serviceName, double failureRate, int responseTimeMs) {
        this.serviceName = serviceName;
        this.failureRate = failureRate;
        this.responseTimeMs = responseTimeMs;
    }

    public void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }

    public void setResponseTime(int responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    protected void simulateNetworkDelay() {
        try {
            Thread.sleep(responseTimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void simulateFailure() {
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new RuntimeException("Simulated service failure: " + serviceName);
        }
    }
}

// Netflix Content Recommendation Service
class NetflixRecommendationService extends ExternalService {
    public NetflixRecommendationService(double failureRate, int responseTimeMs) {
        super("NetflixRecommendationService", failureRate, responseTimeMs);
    }

    public List<String> getRecommendations(String userId) {
        simulateNetworkDelay();
        simulateFailure();
        
        return Arrays.asList(
            "Stranger Things S5",
            "The Crown S6", 
            "Wednesday S2",
            "Money Heist: Korea"
        );
    }

    public Map<String, Double> getUserPreferences(String userId) {
        simulateNetworkDelay();
        simulateFailure();
        
        Map<String, Double> preferences = new HashMap<>();
        preferences.put("Drama", 0.8);
        preferences.put("Thriller", 0.6);
        preferences.put("Comedy", 0.4);
        return preferences;
    }
}

// Uber Driver Location Service
class UberDriverLocationService extends ExternalService {
    public UberDriverLocationService(double failureRate, int responseTimeMs) {
        super("UberDriverLocationService", failureRate, responseTimeMs);
    }

    public List<String> getNearbyDrivers(double latitude, double longitude) {
        simulateNetworkDelay();
        simulateFailure();
        
        return Arrays.asList("driver123", "driver456", "driver789");
    }

    public Map<String, Object> getDriverLocation(String driverId) {
        simulateNetworkDelay();
        simulateFailure();
        
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", 37.7749 + ThreadLocalRandom.current().nextDouble(-0.01, 0.01));
        location.put("longitude", -122.4194 + ThreadLocalRandom.current().nextDouble(-0.01, 0.01));
        location.put("timestamp", LocalDateTime.now());
        return location;
    }
}

// WhatsApp Notification Service
class WhatsAppNotificationService extends ExternalService {
    public WhatsAppNotificationService(double failureRate, int responseTimeMs) {
        super("WhatsAppNotificationService", failureRate, responseTimeMs);
    }

    public boolean sendPushNotification(String userId, String message) {
        simulateNetworkDelay();
        simulateFailure();
        
        return true; // Successfully sent
    }

    public boolean sendSMSFallback(String phoneNumber, String message) {
        simulateNetworkDelay();
        simulateFailure();
        
        return true; // Successfully sent
    }
}

// Service facade with circuit breakers
class ResilientServiceFacade {
    private final Map<String, CircuitBreaker> circuitBreakers;
    private final NetflixRecommendationService netflixService;
    private final UberDriverLocationService uberService;
    private final WhatsAppNotificationService whatsappService;

    public ResilientServiceFacade() {
        this.circuitBreakers = new ConcurrentHashMap<>();
        
        // Initialize services
        this.netflixService = new NetflixRecommendationService(0.1, 200);
        this.uberService = new UberDriverLocationService(0.1, 150);
        this.whatsappService = new WhatsAppNotificationService(0.1, 100);
        
        // Initialize circuit breakers with different configurations
        initializeCircuitBreakers();
    }

    private void initializeCircuitBreakers() {
        // Netflix service - more tolerant to failures (recommendation is non-critical)
        CircuitBreakerConfig netflixConfig = new CircuitBreakerConfig(
            5,                              // failure threshold
            Duration.ofSeconds(3),          // timeout duration
            Duration.ofSeconds(30),         // recovery timeout
            3,                              // half-open max calls
            10                              // minimum calls before trip
        );
        circuitBreakers.put("netflix", new CircuitBreaker("Netflix", netflixConfig));

        // Uber service - less tolerant (critical for ride matching)
        CircuitBreakerConfig uberConfig = new CircuitBreakerConfig(
            3,                              // failure threshold
            Duration.ofSeconds(2),          // timeout duration
            Duration.ofSeconds(20),         // recovery timeout
            2,                              // half-open max calls
            5                               // minimum calls before trip
        );
        circuitBreakers.put("uber", new CircuitBreaker("Uber", uberConfig));

        // WhatsApp service - balanced approach
        CircuitBreakerConfig whatsappConfig = new CircuitBreakerConfig(
            4,                              // failure threshold
            Duration.ofSeconds(2),          // timeout duration
            Duration.ofSeconds(25),         // recovery timeout
            3,                              // half-open max calls
            8                               // minimum calls before trip
        );
        circuitBreakers.put("whatsapp", new CircuitBreaker("WhatsApp", whatsappConfig));
    }

    // Netflix operations with circuit breaker
    public ServiceCallResult<List<String>> getNetflixRecommendations(String userId) {
        CircuitBreaker cb = circuitBreakers.get("netflix");
        return cb.execute(() -> netflixService.getRecommendations(userId), "getRecommendations");
    }

    public ServiceCallResult<Map<String, Double>> getNetflixUserPreferences(String userId) {
        CircuitBreaker cb = circuitBreakers.get("netflix");
        return cb.execute(() -> netflixService.getUserPreferences(userId), "getUserPreferences");
    }

    // Uber operations with circuit breaker
    public ServiceCallResult<List<String>> getUberNearbyDrivers(double lat, double lon) {
        CircuitBreaker cb = circuitBreakers.get("uber");
        return cb.execute(() -> uberService.getNearbyDrivers(lat, lon), "getNearbyDrivers");
    }

    public ServiceCallResult<Map<String, Object>> getUberDriverLocation(String driverId) {
        CircuitBreaker cb = circuitBreakers.get("uber");
        return cb.execute(() -> uberService.getDriverLocation(driverId), "getDriverLocation");
    }

    // WhatsApp operations with circuit breaker
    public ServiceCallResult<Boolean> sendWhatsAppNotification(String userId, String message) {
        CircuitBreaker cb = circuitBreakers.get("whatsapp");
        return cb.execute(() -> whatsappService.sendPushNotification(userId, message), "sendPushNotification");
    }

    public ServiceCallResult<Boolean> sendWhatsAppSMSFallback(String phoneNumber, String message) {
        CircuitBreaker cb = circuitBreakers.get("whatsapp");
        return cb.execute(() -> whatsappService.sendSMSFallback(phoneNumber, message), "sendSMSFallback");
    }

    // Control methods for demo
    public void setServiceFailureRate(String service, double failureRate) {
        switch (service.toLowerCase()) {
            case "netflix":
                netflixService.setFailureRate(failureRate);
                break;
            case "uber":
                uberService.setFailureRate(failureRate);
                break;
            case "whatsapp":
                whatsappService.setFailureRate(failureRate);
                break;
        }
    }

    public void printCircuitBreakerStatus() {
        System.out.println("\n=== Circuit Breaker Status ===");
        circuitBreakers.forEach((name, cb) -> {
            Map<String, Object> metrics = cb.getMetrics();
            System.out.printf("%s: State=%s, Calls=%d, Failures=%d (%.1f%%)%n",
                name.toUpperCase(),
                metrics.get("state"),
                metrics.get("totalCalls"),
                metrics.get("failureCount"),
                metrics.get("failureRate")
            );
        });
    }
}

// Demonstration class
class CircuitBreakerDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Circuit Breaker Pattern Demo ===\n");

        ResilientServiceFacade serviceFacade = new ResilientServiceFacade();

        System.out.println("1. Normal operation - all services healthy");
        performNormalOperations(serviceFacade);
        serviceFacade.printCircuitBreakerStatus();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("2. Introducing failures - Netflix service degraded");
        serviceFacade.setServiceFailureRate("netflix", 0.8); // 80% failure rate

        // Generate enough calls to trip the circuit breaker
        for (int i = 0; i < 15; i++) {
            serviceFacade.getNetflixRecommendations("user" + i);
            Thread.sleep(50);
        }
        serviceFacade.printCircuitBreakerStatus();

        System.out.println("\n3. Circuit breaker is OPEN - calls fail fast");
        for (int i = 0; i < 5; i++) {
            serviceFacade.getNetflixRecommendations("user" + i);
        }

        System.out.println("\n4. Waiting for recovery timeout...");
        Thread.sleep(31000); // Wait for recovery timeout (30 seconds)

        System.out.println("\n5. Circuit breaker transitions to HALF_OPEN");
        serviceFacade.setServiceFailureRate("netflix", 0.1); // Service recovered
        
        // Test calls in half-open state
        for (int i = 0; i < 4; i++) {
            serviceFacade.getNetflixRecommendations("user" + i);
            Thread.sleep(100);
        }
        serviceFacade.printCircuitBreakerStatus();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("6. Testing multiple services with different failure patterns");
        
        // Uber service becomes unreliable
        serviceFacade.setServiceFailureRate("uber", 0.7);
        
        // WhatsApp service has intermittent issues
        serviceFacade.setServiceFailureRate("whatsapp", 0.5);

        // Simulate concurrent load
        for (int i = 0; i < 12; i++) {
            // Try different services
            serviceFacade.getUberNearbyDrivers(37.7749, -122.4194);
            serviceFacade.sendWhatsAppNotification("user" + i, "Test message");
            serviceFacade.getNetflixUserPreferences("user" + i);
            Thread.sleep(100);
        }

        serviceFacade.printCircuitBreakerStatus();

        System.out.println("\n=== Circuit Breaker Benefits Demonstrated ===");
        System.out.println("✓ Fail-fast: Prevents cascading failures by failing quickly when service is down");
        System.out.println("✓ Automatic recovery: Tests service health and automatically recovers");
        System.out.println("✓ Resource protection: Prevents resource exhaustion from repeated failed calls");
        System.out.println("✓ Different policies: Each service can have different circuit breaker configurations");
        System.out.println("✓ Monitoring: Provides metrics about service health and failure rates");
    }

    private static void performNormalOperations(ResilientServiceFacade serviceFacade) {
        // Test normal operations
        serviceFacade.getNetflixRecommendations("user123");
        serviceFacade.getNetflixUserPreferences("user123");
        serviceFacade.getUberNearbyDrivers(37.7749, -122.4194);
        serviceFacade.getUberDriverLocation("driver123");
        serviceFacade.sendWhatsAppNotification("user123", "Hello!");
        serviceFacade.sendWhatsAppSMSFallback("+1234567890", "Fallback message");
    }
}
