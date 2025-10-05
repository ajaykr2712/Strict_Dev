# Java Design Patterns for Real-World Use Cases

## Overview
This document presents Java design patterns derived from analyzing three major system architectures: Netflix (streaming platform), Uber (ride-sharing), and WhatsApp (messaging). Each pattern addresses specific challenges identified in these real-world systems.

---

## Use Case Analysis Summary

### Netflix Key Challenges
- **Content delivery at scale**: Global CDN with adaptive streaming
- **Recommendation system**: Personalized content for 200M+ users  
- **Microservices resilience**: Circuit breakers, chaos engineering
- **Event-driven architecture**: Asynchronous processing, CQRS

### Uber Key Challenges
- **Real-time matching**: Efficient rider-driver pairing algorithms
- **Location tracking**: Geospatial data processing at scale
- **State management**: Trip lifecycle with complex state transitions
- **Dynamic pricing**: Real-time surge pricing calculations

### WhatsApp Key Challenges
- **Message routing**: Efficient delivery to 2B+ users
- **Connection management**: Massive concurrent connections
- **Media handling**: Optimized media compression and storage
- **End-to-end encryption**: Secure message processing

---

## Pattern 1: Adaptive Streaming Strategy (Netflix)

### Problem
Netflix needs to deliver video content at different quality levels based on user's network conditions and device capabilities.

### Solution: Strategy Pattern with Factory

```java
/**
 * Strategy Pattern for Adaptive Streaming
 * Handles different streaming qualities based on network conditions
 */
public interface StreamingStrategy {
    StreamingConfig getOptimalConfig(NetworkCondition condition, DeviceCapability device);
    String getQualityDescription();
    int getBitrate();
}

public class HighDefinitionStrategy implements StreamingStrategy {
    @Override
    public StreamingConfig getOptimalConfig(NetworkCondition condition, DeviceCapability device) {
        if (condition.getBandwidth() >= 5000 && device.supports4K()) {
            return StreamingConfig.builder()
                    .resolution("3840x2160")
                    .bitrate(25000)
                    .codec("H.265")
                    .bufferSize(10)
                    .build();
        }
        return null; // Fallback to lower strategy
    }
    
    @Override
    public String getQualityDescription() { return "4K Ultra HD"; }
    
    @Override
    public int getBitrate() { return 25000; }
}

public class StandardDefinitionStrategy implements StreamingStrategy {
    @Override
    public StreamingConfig getOptimalConfig(NetworkCondition condition, DeviceCapability device) {
        if (condition.getBandwidth() >= 1500) {
            return StreamingConfig.builder()
                    .resolution("1280x720")
                    .bitrate(3000)
                    .codec("H.264")
                    .bufferSize(5)
                    .build();
        }
        return null;
    }
    
    @Override
    public String getQualityDescription() { return "720p HD"; }
    
    @Override
    public int getBitrate() { return 3000; }
}

public class LowDefinitionStrategy implements StreamingStrategy {
    @Override
    public StreamingConfig getOptimalConfig(NetworkCondition condition, DeviceCapability device) {
        return StreamingConfig.builder()
                .resolution("640x480")
                .bitrate(800)
                .codec("H.264")
                .bufferSize(3)
                .build();
    }
    
    @Override
    public String getQualityDescription() { return "480p SD"; }
    
    @Override
    public int getBitrate() { return 800; }
}

/**
 * Factory for creating appropriate streaming strategies
 */
public class StreamingStrategyFactory {
    private static final List<StreamingStrategy> STRATEGIES = Arrays.asList(
        new HighDefinitionStrategy(),
        new StandardDefinitionStrategy(),
        new LowDefinitionStrategy()
    );
    
    public static StreamingStrategy getOptimalStrategy(NetworkCondition condition, DeviceCapability device) {
        return STRATEGIES.stream()
                .filter(strategy -> strategy.getOptimalConfig(condition, device) != null)
                .findFirst()
                .orElse(new LowDefinitionStrategy()); // Always fallback to lowest quality
    }
}

/**
 * Adaptive Streaming Manager using Strategy Pattern
 */
public class AdaptiveStreamingManager {
    private StreamingStrategy currentStrategy;
    private final NetworkMonitor networkMonitor;
    private final DeviceDetector deviceDetector;
    
    public AdaptiveStreamingManager(NetworkMonitor networkMonitor, DeviceDetector deviceDetector) {
        this.networkMonitor = networkMonitor;
        this.deviceDetector = deviceDetector;
        this.currentStrategy = new LowDefinitionStrategy(); // Default
    }
    
    public StreamingConfig getCurrentStreamingConfig() {
        NetworkCondition currentCondition = networkMonitor.getCurrentCondition();
        DeviceCapability currentDevice = deviceDetector.getDeviceCapability();
        
        StreamingStrategy optimalStrategy = StreamingStrategyFactory
                .getOptimalStrategy(currentCondition, currentDevice);
        
        if (!optimalStrategy.equals(currentStrategy)) {
            System.out.println("Switching from " + currentStrategy.getQualityDescription() + 
                             " to " + optimalStrategy.getQualityDescription());
            this.currentStrategy = optimalStrategy;
        }
        
        return currentStrategy.getOptimalConfig(currentCondition, currentDevice);
    }
}
```

---

## Pattern 2: Geospatial Matching Algorithm (Uber)

### Problem
Uber needs to efficiently match riders with nearby drivers while considering multiple factors like distance, rating, and driver preferences.

### Solution: Strategy Pattern with Composite Algorithm

```java
/**
 * Driver Matching Strategy Interface
 */
public interface DriverMatchingStrategy {
    List<DriverMatch> findBestMatches(RideRequest request, List<Driver> availableDrivers, int maxMatches);
    String getAlgorithmName();
}

/**
 * Proximity-based matching (primary strategy)
 */
public class ProximityMatchingStrategy implements DriverMatchingStrategy {
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    @Override
    public List<DriverMatch> findBestMatches(RideRequest request, List<Driver> availableDrivers, int maxMatches) {
        return availableDrivers.stream()
                .map(driver -> new DriverMatch(driver, calculateDistance(request.getPickupLocation(), driver.getLocation())))
                .filter(match -> match.getDistance() <= 10.0) // Within 10km
                .sorted(Comparator.comparing(DriverMatch::getDistance))
                .limit(maxMatches)
                .collect(Collectors.toList());
    }
    
    private double calculateDistance(Location pickup, Location driver) {
        double lat1Rad = Math.toRadians(pickup.getLatitude());
        double lat2Rad = Math.toRadians(driver.getLatitude());
        double deltaLatRad = Math.toRadians(driver.getLatitude() - pickup.getLatitude());
        double deltaLonRad = Math.toRadians(driver.getLongitude() - pickup.getLongitude());
        
        double a = Math.sin(deltaLatRad/2) * Math.sin(deltaLatRad/2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad/2) * Math.sin(deltaLonRad/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS_KM * c;
    }
    
    @Override
    public String getAlgorithmName() { return "Proximity-Based"; }
}

/**
 * Rating-aware matching strategy
 */
public class RatingAwareMatchingStrategy implements DriverMatchingStrategy {
    @Override
    public List<DriverMatch> findBestMatches(RideRequest request, List<Driver> availableDrivers, int maxMatches) {
        return availableDrivers.stream()
                .map(driver -> {
                    double distance = calculateDistance(request.getPickupLocation(), driver.getLocation());
                    double score = calculateCompositeScore(driver, distance);
                    return new DriverMatch(driver, distance, score);
                })
                .filter(match -> match.getDistance() <= 15.0) // Wider radius for better drivers
                .sorted(Comparator.comparing(DriverMatch::getScore).reversed())
                .limit(maxMatches)
                .collect(Collectors.toList());
    }
    
    private double calculateCompositeScore(Driver driver, double distance) {
        double distanceScore = Math.max(0, 10 - distance) / 10.0; // Closer is better
        double ratingScore = driver.getRating() / 5.0; // Rating out of 5
        double acceptanceScore = driver.getAcceptanceRate(); // 0.0 to 1.0
        
        return (distanceScore * 0.4) + (ratingScore * 0.4) + (acceptanceScore * 0.2);
    }
    
    private double calculateDistance(Location pickup, Location driver) {
        // Same implementation as ProximityMatchingStrategy
        return 0.0; // Simplified for brevity
    }
    
    @Override
    public String getAlgorithmName() { return "Rating-Aware"; }
}

/**
 * Machine Learning enhanced matching (for peak times)
 */
public class MLEnhancedMatchingStrategy implements DriverMatchingStrategy {
    private final MLModel predictionModel;
    
    public MLEnhancedMatchingStrategy(MLModel predictionModel) {
        this.predictionModel = predictionModel;
    }
    
    @Override
    public List<DriverMatch> findBestMatches(RideRequest request, List<Driver> availableDrivers, int maxMatches) {
        return availableDrivers.stream()
                .map(driver -> {
                    double distance = calculateDistance(request.getPickupLocation(), driver.getLocation());
                    double eta = predictionModel.predictETA(driver, request.getPickupLocation());
                    double acceptanceProbability = predictionModel.predictAcceptance(driver, request);
                    
                    double mlScore = calculateMLScore(distance, eta, acceptanceProbability);
                    return new DriverMatch(driver, distance, mlScore, eta, acceptanceProbability);
                })
                .filter(match -> match.getAcceptanceProbability() > 0.3) // Likely to accept
                .sorted(Comparator.comparing(DriverMatch::getScore).reversed())
                .limit(maxMatches)
                .collect(Collectors.toList());
    }
    
    private double calculateMLScore(double distance, double eta, double acceptanceProbability) {
        double distanceScore = Math.max(0, 1 - (distance / 20.0)); // Normalize distance
        double etaScore = Math.max(0, 1 - (eta / 600.0)); // Normalize ETA (10 minutes max)
        
        return (distanceScore * 0.3) + (etaScore * 0.3) + (acceptanceProbability * 0.4);
    }
    
    private double calculateDistance(Location pickup, Location driver) {
        // Implementation same as other strategies
        return 0.0;
    }
    
    @Override
    public String getAlgorithmName() { return "ML-Enhanced"; }
}

/**
 * Factory for driver matching strategies
 */
public class DriverMatchingStrategyFactory {
    
    public static DriverMatchingStrategy createStrategy(MatchingContext context) {
        if (context.isPeakHours() && context.hasMLModel()) {
            return new MLEnhancedMatchingStrategy(context.getMLModel());
        } else if (context.isQualityFocused()) {
            return new RatingAwareMatchingStrategy();
        } else {
            return new ProximityMatchingStrategy(); // Default
        }
    }
}

/**
 * Main Driver Matching Service
 */
public class DriverMatchingService {
    private final DriverLocationService locationService;
    private final DriverAvailabilityService availabilityService;
    
    public DriverMatchingService(DriverLocationService locationService, 
                               DriverAvailabilityService availabilityService) {
        this.locationService = locationService;
        this.availabilityService = availabilityService;
    }
    
    public List<DriverMatch> findMatchingDrivers(RideRequest request) {
        // Get available drivers in the area
        List<Driver> availableDrivers = locationService.findNearbyDrivers(
            request.getPickupLocation(), 20.0); // 20km radius
        
        // Filter by availability
        availableDrivers = availabilityService.filterAvailable(availableDrivers);
        
        // Create matching context
        MatchingContext context = MatchingContext.builder()
                .isPeakHours(isPeakHours())
                .isQualityFocused(request.getRiderTier().isPreferred())
                .hasMLModel(true)
                .mlModel(getMLModel())
                .build();
        
        // Get appropriate strategy
        DriverMatchingStrategy strategy = DriverMatchingStrategyFactory.createStrategy(context);
        
        // Find matches
        List<DriverMatch> matches = strategy.findBestMatches(request, availableDrivers, 5);
        
        System.out.println("Found " + matches.size() + " matches using " + strategy.getAlgorithmName());
        return matches;
    }
    
    private boolean isPeakHours() {
        LocalTime now = LocalTime.now();
        return (now.isAfter(LocalTime.of(7, 0)) && now.isBefore(LocalTime.of(10, 0))) ||
               (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(20, 0)));
    }
    
    private MLModel getMLModel() {
        // Return trained ML model
        return null; // Simplified
    }
}
```

---

## Pattern 3: State Machine for Trip Lifecycle (Uber)

### Problem
Uber trips go through complex state transitions that need to be managed consistently and handle edge cases.

### Solution: State Pattern with Event-Driven Transitions

```java
/**
 * Trip State Interface
 */
public interface TripState {
    void handleEvent(TripContext context, TripEvent event);
    TripStatus getStatus();
    boolean canTransitionTo(TripStatus newStatus);
    void onEntry(TripContext context);
    void onExit(TripContext context);
}

/**
 * Abstract base state with common functionality
 */
public abstract class AbstractTripState implements TripState {
    @Override
    public void onEntry(TripContext context) {
        context.logStateTransition(getStatus());
    }
    
    @Override
    public void onExit(TripContext context) {
        // Default implementation - can be overridden
    }
    
    protected void transitionTo(TripContext context, TripState newState) {
        if (canTransitionTo(newState.getStatus())) {
            this.onExit(context);
            context.setState(newState);
            newState.onEntry(context);
        } else {
            throw new IllegalStateTransitionException(
                "Cannot transition from " + getStatus() + " to " + newState.getStatus());
        }
    }
}

/**
 * Requested State - Initial state when trip is requested
 */
public class RequestedState extends AbstractTripState {
    @Override
    public void handleEvent(TripContext context, TripEvent event) {
        switch (event.getType()) {
            case DRIVER_MATCHED:
                transitionTo(context, new MatchedState());
                context.notifyRider("Driver found: " + event.getDriverInfo());
                break;
            case TRIP_CANCELLED:
                transitionTo(context, new CancelledState());
                context.processCancellation(event.getCancellationReason());
                break;
            case MATCHING_TIMEOUT:
                context.retryMatching();
                break;
            default:
                context.logInvalidEvent(event, getStatus());
        }
    }
    
    @Override
    public TripStatus getStatus() { return TripStatus.REQUESTED; }
    
    @Override
    public boolean canTransitionTo(TripStatus newStatus) {
        return newStatus == TripStatus.MATCHED || newStatus == TripStatus.CANCELLED;
    }
}

/**
 * Matched State - Driver has been assigned
 */
public class MatchedState extends AbstractTripState {
    @Override
    public void handleEvent(TripContext context, TripEvent event) {
        switch (event.getType()) {
            case DRIVER_ARRIVING:
                transitionTo(context, new ArrivingState());
                context.startETATracking();
                break;
            case DRIVER_CANCELLED:
                transitionTo(context, new RequestedState());
                context.findNewDriver();
                break;
            case TRIP_CANCELLED:
                transitionTo(context, new CancelledState());
                context.processCancellation(event.getCancellationReason());
                break;
            default:
                context.logInvalidEvent(event, getStatus());
        }
    }
    
    @Override
    public TripStatus getStatus() { return TripStatus.MATCHED; }
    
    @Override
    public boolean canTransitionTo(TripStatus newStatus) {
        return newStatus == TripStatus.ARRIVING || 
               newStatus == TripStatus.CANCELLED || 
               newStatus == TripStatus.REQUESTED;
    }
}

/**
 * Arriving State - Driver is en route to pickup
 */
public class ArrivingState extends AbstractTripState {
    @Override
    public void handleEvent(TripContext context, TripEvent event) {
        switch (event.getType()) {
            case DRIVER_ARRIVED:
                transitionTo(context, new WaitingState());
                context.notifyRider("Driver has arrived");
                break;
            case TRIP_STARTED:
                transitionTo(context, new InProgressState());
                context.startTripTimer();
                break;
            case TRIP_CANCELLED:
                transitionTo(context, new CancelledState());
                context.processCancellation(event.getCancellationReason());
                break;
            default:
                context.logInvalidEvent(event, getStatus());
        }
    }
    
    @Override
    public TripStatus getStatus() { return TripStatus.ARRIVING; }
    
    @Override
    public boolean canTransitionTo(TripStatus newStatus) {
        return newStatus == TripStatus.WAITING || 
               newStatus == TripStatus.IN_PROGRESS || 
               newStatus == TripStatus.CANCELLED;
    }
}

/**
 * Trip Context - Manages state and provides services
 */
public class TripContext {
    private TripState currentState;
    private final Trip trip;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final LocationService locationService;
    private final EventLogger eventLogger;
    
    public TripContext(Trip trip, NotificationService notificationService, 
                      PaymentService paymentService, LocationService locationService,
                      EventLogger eventLogger) {
        this.trip = trip;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.locationService = locationService;
        this.eventLogger = eventLogger;
        this.currentState = new RequestedState();
    }
    
    public void handleEvent(TripEvent event) {
        eventLogger.logEvent(trip.getTripId(), event);
        currentState.handleEvent(this, event);
    }
    
    public void setState(TripState newState) {
        this.currentState = newState;
        trip.setStatus(newState.getStatus());
    }
    
    public TripStatus getCurrentStatus() {
        return currentState.getStatus();
    }
    
    // Context services
    public void notifyRider(String message) {
        notificationService.sendToRider(trip.getRiderId(), message);
    }
    
    public void notifyDriver(String message) {
        notificationService.sendToDriver(trip.getDriverId(), message);
    }
    
    public void logStateTransition(TripStatus newStatus) {
        eventLogger.logStateTransition(trip.getTripId(), newStatus);
    }
    
    public void processCancellation(String reason) {
        paymentService.processCancellation(trip);
        eventLogger.logCancellation(trip.getTripId(), reason);
    }
    
    public void startETATracking() {
        locationService.startETATracking(trip.getTripId());
    }
    
    public void retryMatching() {
        // Logic to retry driver matching
    }
    
    public void findNewDriver() {
        // Logic to find a new driver
    }
    
    public void startTripTimer() {
        trip.setStartTime(LocalDateTime.now());
    }
    
    public void logInvalidEvent(TripEvent event, TripStatus currentStatus) {
        eventLogger.logError("Invalid event " + event.getType() + " for status " + currentStatus);
    }
}

/**
 * Trip State Machine Manager
 */
public class TripStateMachine {
    private final Map<String, TripContext> activeTrips = new ConcurrentHashMap<>();
    private final TripRepository tripRepository;
    
    public TripStateMachine(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }
    
    public void createTrip(Trip trip) {
        TripContext context = new TripContext(
            trip,
            getNotificationService(),
            getPaymentService(),
            getLocationService(),
            getEventLogger()
        );
        
        activeTrips.put(trip.getTripId(), context);
        tripRepository.save(trip);
    }
    
    public void processEvent(String tripId, TripEvent event) {
        TripContext context = activeTrips.get(tripId);
        if (context != null) {
            context.handleEvent(event);
            
            // Save state changes
            tripRepository.updateStatus(tripId, context.getCurrentStatus());
            
            // Remove from active trips if completed or cancelled
            if (isTerminalState(context.getCurrentStatus())) {
                activeTrips.remove(tripId);
            }
        }
    }
    
    private boolean isTerminalState(TripStatus status) {
        return status == TripStatus.COMPLETED || status == TripStatus.CANCELLED;
    }
    
    // Inject dependencies
    private NotificationService getNotificationService() { return null; }
    private PaymentService getPaymentService() { return null; }
    private LocationService getLocationService() { return null; }
    private EventLogger getEventLogger() { return null; }
}
```

---

## Pattern 4: Message Routing System (WhatsApp)

### Problem
WhatsApp needs to efficiently route messages to users who may be online on multiple devices or offline entirely.

### Solution: Observer Pattern with Chain of Responsibility

```java
/**
 * Message routing strategy interface
 */
public interface MessageRouter {
    boolean route(Message message, UserSession targetSession);
    MessageRouter setNext(MessageRouter next);
}

/**
 * Abstract base router implementing chain pattern
 */
public abstract class AbstractMessageRouter implements MessageRouter {
    private MessageRouter nextRouter;
    
    @Override
    public MessageRouter setNext(MessageRouter next) {
        this.nextRouter = next;
        return next;
    }
    
    protected boolean passToNext(Message message, UserSession targetSession) {
        if (nextRouter != null) {
            return nextRouter.route(message, targetSession);
        }
        return false;
    }
}

/**
 * Online Device Router - Routes to active device connections
 */
public class OnlineDeviceRouter extends AbstractMessageRouter {
    private final ConnectionManager connectionManager;
    
    public OnlineDeviceRouter(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    @Override
    public boolean route(Message message, UserSession targetSession) {
        List<DeviceConnection> activeConnections = connectionManager
                .getActiveConnections(targetSession.getUserId());
        
        if (!activeConnections.isEmpty()) {
            // Route to all active devices
            for (DeviceConnection connection : activeConnections) {
                try {
                    connection.sendMessage(message);
                    markAsDelivered(message, connection.getDeviceId());
                } catch (ConnectionException e) {
                    // Log error but continue to other devices
                    System.err.println("Failed to deliver to device: " + connection.getDeviceId());
                }
            }
            return true;
        }
        
        // No active connections, pass to next router
        return passToNext(message, targetSession);
    }
    
    private void markAsDelivered(Message message, String deviceId) {
        message.addDeliveryReceipt(deviceId, LocalDateTime.now());
    }
}

/**
 * Push Notification Router - For offline users
 */
public class PushNotificationRouter extends AbstractMessageRouter {
    private final PushNotificationService pushService;
    
    public PushNotificationRouter(PushNotificationService pushService) {
        this.pushService = pushService;
    }
    
    @Override
    public boolean route(Message message, UserSession targetSession) {
        if (!targetSession.isOnline()) {
            // Store message for when user comes online
            storeOfflineMessage(message, targetSession.getUserId());
            
            // Send push notification
            PushNotification notification = createPushNotification(message);
            boolean sent = pushService.sendNotification(targetSession.getUserId(), notification);
            
            if (sent) {
                message.setStatus(MessageStatus.SENT_PUSH);
                return true;
            }
        }
        
        return passToNext(message, targetSession);
    }
    
    private void storeOfflineMessage(Message message, String userId) {
        // Store in offline message queue
    }
    
    private PushNotification createPushNotification(Message message) {
        return PushNotification.builder()
                .title(message.getSenderName())
                .body(message.isMediaMessage() ? "📎 Media" : message.getContent())
                .userId(message.getRecipientId())
                .messageId(message.getMessageId())
                .build();
    }
}

/**
 * Fallback Router - Last resort storage
 */
public class FallbackStorageRouter extends AbstractMessageRouter {
    private final MessageStorage messageStorage;
    
    public FallbackStorageRouter(MessageStorage messageStorage) {
        this.messageStorage = messageStorage;
    }
    
    @Override
    public boolean route(Message message, UserSession targetSession) {
        // Always store message as fallback
        messageStorage.storeMessage(message);
        message.setStatus(MessageStatus.STORED);
        
        System.out.println("Message stored for offline delivery: " + message.getMessageId());
        return true; // Always succeeds
    }
}

/**
 * Message Delivery Observer Pattern
 */
public interface MessageDeliveryObserver {
    void onMessageSent(Message message);
    void onMessageDelivered(Message message, String deviceId);
    void onMessageRead(Message message, String deviceId);
    void onDeliveryFailed(Message message, String reason);
}

/**
 * Analytics Observer - Tracks message delivery metrics
 */
public class MessageAnalyticsObserver implements MessageDeliveryObserver {
    private final AnalyticsService analyticsService;
    
    public MessageAnalyticsObserver(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    @Override
    public void onMessageSent(Message message) {
        analyticsService.recordEvent("message_sent", message.getMetadata());
    }
    
    @Override
    public void onMessageDelivered(Message message, String deviceId) {
        Map<String, Object> metadata = new HashMap<>(message.getMetadata());
        metadata.put("device_id", deviceId);
        metadata.put("delivery_time", LocalDateTime.now());
        analyticsService.recordEvent("message_delivered", metadata);
    }
    
    @Override
    public void onMessageRead(Message message, String deviceId) {
        long deliveryToReadTime = calculateReadLatency(message);
        Map<String, Object> metadata = new HashMap<>(message.getMetadata());
        metadata.put("read_latency_ms", deliveryToReadTime);
        analyticsService.recordEvent("message_read", metadata);
    }
    
    @Override
    public void onDeliveryFailed(Message message, String reason) {
        Map<String, Object> metadata = new HashMap<>(message.getMetadata());
        metadata.put("failure_reason", reason);
        analyticsService.recordEvent("message_delivery_failed", metadata);
    }
    
    private long calculateReadLatency(Message message) {
        return message.getDeliveryReceipts().values().stream()
                .findFirst()
                .map(deliveryTime -> Duration.between(deliveryTime, LocalDateTime.now()).toMillis())
                .orElse(0L);
    }
}

/**
 * Main Message Routing Service
 */
public class MessageRoutingService {
    private final MessageRouter routerChain;
    private final List<MessageDeliveryObserver> observers = new CopyOnWriteArrayList<>();
    private final UserSessionManager sessionManager;
    
    public MessageRoutingService(ConnectionManager connectionManager,
                               PushNotificationService pushService,
                               MessageStorage messageStorage,
                               UserSessionManager sessionManager) {
        
        this.sessionManager = sessionManager;
        
        // Build router chain
        this.routerChain = new OnlineDeviceRouter(connectionManager);
        routerChain.setNext(new PushNotificationRouter(pushService))
                  .setNext(new FallbackStorageRouter(messageStorage));
        
        // Add default observers
        addObserver(new MessageAnalyticsObserver(new AnalyticsService()));
    }
    
    public void addObserver(MessageDeliveryObserver observer) {
        observers.add(observer);
    }
    
    public void removeObserver(MessageDeliveryObserver observer) {
        observers.remove(observer);
    }
    
    public void routeMessage(Message message) {
        try {
            // Notify observers
            notifyMessageSent(message);
            
            // Get target user session
            UserSession targetSession = sessionManager.getUserSession(message.getRecipientId());
            
            // Route through chain
            boolean routed = routerChain.route(message, targetSession);
            
            if (routed) {
                System.out.println("Message routed successfully: " + message.getMessageId());
            } else {
                notifyDeliveryFailed(message, "All routing attempts failed");
            }
            
        } catch (Exception e) {
            notifyDeliveryFailed(message, e.getMessage());
        }
    }
    
    public void handleDeliveryReceipt(String messageId, String deviceId) {
        // Find message and notify observers
        Message message = findMessage(messageId);
        if (message != null) {
            notifyMessageDelivered(message, deviceId);
        }
    }
    
    public void handleReadReceipt(String messageId, String deviceId) {
        Message message = findMessage(messageId);
        if (message != null) {
            notifyMessageRead(message, deviceId);
        }
    }
    
    private void notifyMessageSent(Message message) {
        observers.forEach(observer -> {
            try {
                observer.onMessageSent(message);
            } catch (Exception e) {
                System.err.println("Observer notification failed: " + e.getMessage());
            }
        });
    }
    
    private void notifyMessageDelivered(Message message, String deviceId) {
        observers.forEach(observer -> observer.onMessageDelivered(message, deviceId));
    }
    
    private void notifyMessageRead(Message message, String deviceId) {
        observers.forEach(observer -> observer.onMessageRead(message, deviceId));
    }
    
    private void notifyDeliveryFailed(Message message, String reason) {
        observers.forEach(observer -> observer.onDeliveryFailed(message, reason));
    }
    
    private Message findMessage(String messageId) {
        // Implementation to find message by ID
        return null;
    }
}
```

---

## Pattern 5: Circuit Breaker for Service Resilience (Netflix)

### Problem
Netflix microservices need to handle failures gracefully and prevent cascading failures when dependent services are unavailable.

### Solution: Circuit Breaker with Bulkhead Pattern

```java
/**
 * Enhanced Circuit Breaker with metrics and adaptive thresholds
 */
public class NetflixStyleCircuitBreaker {
    private final String serviceName;
    private final CircuitBreakerConfig config;
    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final CircuitBreakerMetrics metrics = new CircuitBreakerMetrics();
    
    public NetflixStyleCircuitBreaker(String serviceName, CircuitBreakerConfig config) {
        this.serviceName = serviceName;
        this.config = config;
    }
    
    public <T> T execute(Supplier<T> operation, Supplier<T> fallback) {
        if (shouldAllowRequest()) {
            try {
                long startTime = System.currentTimeMillis();
                T result = operation.get();
                long duration = System.currentTimeMillis() - startTime;
                
                recordSuccess(duration);
                return result;
                
            } catch (Exception e) {
                recordFailure();
                return executeFallback(fallback, e);
            }
        } else {
            return executeFallback(fallback, new CircuitBreakerOpenException(serviceName));
        }
    }
    
    private boolean shouldAllowRequest() {
        CircuitState currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                if (shouldAttemptReset()) {
                    state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN);
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                return successCount.get() < config.getHalfOpenMaxRequests();
                
            default:
                return false;
        }
    }
    
    private boolean shouldAttemptReset() {
        return System.currentTimeMillis() - lastFailureTime.get() >= config.getSleepWindow();
    }
    
    private void recordSuccess(long duration) {
        metrics.recordSuccess(duration);
        failureCount.set(0);
        
        if (state.get() == CircuitState.HALF_OPEN) {
            int successCounter = successCount.incrementAndGet();
            if (successCounter >= config.getHalfOpenSuccessThreshold()) {
                state.set(CircuitState.CLOSED);
                successCount.set(0);
                System.out.println("Circuit breaker CLOSED for " + serviceName);
            }
        }
    }
    
    private void recordFailure() {
        metrics.recordFailure();
        lastFailureTime.set(System.currentTimeMillis());
        
        int failures = failureCount.incrementAndGet();
        
        if (state.get() == CircuitState.CLOSED && 
            failures >= getAdaptiveFailureThreshold()) {
            state.set(CircuitState.OPEN);
            System.out.println("Circuit breaker OPENED for " + serviceName + " after " + failures + " failures");
        } else if (state.get() == CircuitState.HALF_OPEN) {
            state.set(CircuitState.OPEN);
            successCount.set(0);
        }
    }
    
    private int getAdaptiveFailureThreshold() {
        // Adaptive threshold based on recent success rate
        double recentSuccessRate = metrics.getRecentSuccessRate();
        if (recentSuccessRate < 0.5) {
            return Math.max(1, config.getFailureThreshold() / 2); // Lower threshold if already problematic
        }
        return config.getFailureThreshold();
    }
    
    private <T> T executeFallback(Supplier<T> fallback, Exception originalException) {
        if (fallback != null) {
            try {
                T result = fallback.get();
                metrics.recordFallbackSuccess();
                return result;
            } catch (Exception e) {
                metrics.recordFallbackFailure();
                throw new RuntimeException("Both primary and fallback failed", originalException);
            }
        } else {
            throw new RuntimeException("Service unavailable: " + serviceName, originalException);
        }
    }
    
    public CircuitBreakerStats getStats() {
        return CircuitBreakerStats.builder()
                .serviceName(serviceName)
                .state(state.get())
                .failureCount(failureCount.get())
                .successRate(metrics.getSuccessRate())
                .averageResponseTime(metrics.getAverageResponseTime())
                .requestCount(metrics.getTotalRequests())
                .build();
    }
}

/**
 * Bulkhead pattern for resource isolation
 */
public class ServiceBulkhead {
    private final String serviceName;
    private final ExecutorService dedicatedThreadPool;
    private final Semaphore requestSemaphore;
    private final NetflixStyleCircuitBreaker circuitBreaker;
    
    public ServiceBulkhead(String serviceName, BulkheadConfig config) {
        this.serviceName = serviceName;
        this.dedicatedThreadPool = Executors.newFixedThreadPool(
            config.getMaxThreads(),
            new ThreadFactoryBuilder()
                .setNameFormat(serviceName + "-pool-%d")
                .build()
        );
        this.requestSemaphore = new Semaphore(config.getMaxConcurrentRequests());
        this.circuitBreaker = new NetflixStyleCircuitBreaker(serviceName, config.getCircuitBreakerConfig());
    }
    
    public <T> CompletableFuture<T> executeAsync(Supplier<T> operation, Supplier<T> fallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                requestSemaphore.acquire();
                return circuitBreaker.execute(operation, fallback);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Request interrupted for " + serviceName, e);
            } finally {
                requestSemaphore.release();
            }
        }, dedicatedThreadPool);
    }
    
    public <T> T execute(Supplier<T> operation, Supplier<T> fallback, Duration timeout) {
        try {
            return executeAsync(operation, fallback)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Request timeout for " + serviceName, e);
        } catch (Exception e) {
            throw new RuntimeException("Request failed for " + serviceName, e);
        }
    }
    
    public void shutdown() {
        dedicatedThreadPool.shutdown();
        try {
            if (!dedicatedThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                dedicatedThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            dedicatedThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Service Discovery with Circuit Breaker Integration
 */
public class ResilientServiceClient {
    private final Map<String, ServiceBulkhead> serviceBulkheads = new ConcurrentHashMap<>();
    private final ServiceRegistry serviceRegistry;
    private final MetricsCollector metricsCollector;
    
    public ResilientServiceClient(ServiceRegistry serviceRegistry, MetricsCollector metricsCollector) {
        this.serviceRegistry = serviceRegistry;
        this.metricsCollector = metricsCollector;
    }
    
    public <T> T callService(String serviceName, Function<String, T> serviceCall, T fallbackValue) {
        ServiceBulkhead bulkhead = getOrCreateBulkhead(serviceName);
        
        return bulkhead.execute(
            () -> {
                String serviceUrl = serviceRegistry.getHealthyInstance(serviceName);
                return serviceCall.apply(serviceUrl);
            },
            () -> {
                metricsCollector.incrementFallbackCounter(serviceName);
                return fallbackValue;
            },
            Duration.ofSeconds(5)
        );
    }
    
    private ServiceBulkhead getOrCreateBulkhead(String serviceName) {
        return serviceBulkheads.computeIfAbsent(serviceName, name -> {
            BulkheadConfig config = BulkheadConfig.builder()
                    .maxThreads(10)
                    .maxConcurrentRequests(20)
                    .circuitBreakerConfig(CircuitBreakerConfig.builder()
                            .failureThreshold(5)
                            .sleepWindow(30000)
                            .halfOpenMaxRequests(3)
                            .halfOpenSuccessThreshold(2)
                            .build())
                    .build();
            return new ServiceBulkhead(name, config);
        });
    }
    
    public Map<String, CircuitBreakerStats> getAllServiceStats() {
        return serviceBulkheads.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().circuitBreaker.getStats()
                ));
    }
}
```

---

## Supporting Classes and Configuration

```java
/**
 * Configuration classes for the patterns
 */
public class StreamingConfig {
    private final String resolution;
    private final int bitrate;
    private final String codec;
    private final int bufferSize;
    
    private StreamingConfig(Builder builder) {
        this.resolution = builder.resolution;
        this.bitrate = builder.bitrate;
        this.codec = builder.codec;
        this.bufferSize = builder.bufferSize;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String resolution;
        private int bitrate;
        private String codec;
        private int bufferSize;
        
        public Builder resolution(String resolution) { this.resolution = resolution; return this; }
        public Builder bitrate(int bitrate) { this.bitrate = bitrate; return this; }
        public Builder codec(String codec) { this.codec = codec; return this; }
        public Builder bufferSize(int bufferSize) { this.bufferSize = bufferSize; return this; }
        
        public StreamingConfig build() {
            return new StreamingConfig(this);
        }
    }
    
    // Getters...
}

/**
 * Trip related enums and classes
 */
public enum TripStatus {
    REQUESTED, MATCHED, ARRIVING, WAITING, IN_PROGRESS, COMPLETED, CANCELLED
}

public enum TripEventType {
    DRIVER_MATCHED, DRIVER_ARRIVING, DRIVER_ARRIVED, TRIP_STARTED, 
    TRIP_COMPLETED, TRIP_CANCELLED, DRIVER_CANCELLED, MATCHING_TIMEOUT
}

public class TripEvent {
    private final TripEventType type;
    private final Map<String, Object> data;
    private final LocalDateTime timestamp;
    
    public TripEvent(TripEventType type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and convenience methods
    public String getDriverInfo() { return (String) data.get("driverInfo"); }
    public String getCancellationReason() { return (String) data.get("reason"); }
}

/**
 * Message related classes
 */
public enum MessageStatus {
    SENT, DELIVERED, READ, SENT_PUSH, STORED, FAILED
}

public class Message {
    private final String messageId;
    private final String senderId;
    private final String recipientId;
    private final String content;
    private final boolean isMediaMessage;
    private final LocalDateTime timestamp;
    private final Map<String, LocalDateTime> deliveryReceipts = new ConcurrentHashMap<>();
    private MessageStatus status;
    
    // Constructor and methods...
}

/**
 * Circuit Breaker related classes
 */
public enum CircuitState {
    CLOSED, OPEN, HALF_OPEN
}

public class CircuitBreakerConfig {
    private final int failureThreshold;
    private final long sleepWindow;
    private final int halfOpenMaxRequests;
    private final int halfOpenSuccessThreshold;
    
    // Builder pattern implementation...
}

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String serviceName) {
        super("Circuit breaker is open for service: " + serviceName);
    }
}
```

---

## Key Design Principles Applied

### 1. Netflix Patterns
- **Strategy Pattern**: Adaptive streaming based on network conditions
- **Circuit Breaker**: Microservice resilience with fallback strategies
- **Observer Pattern**: Event-driven analytics and monitoring

### 2. Uber Patterns
- **Strategy Pattern**: Multiple driver matching algorithms
- **State Pattern**: Complex trip lifecycle management
- **Factory Pattern**: Algorithm selection based on context

### 3. WhatsApp Patterns
- **Chain of Responsibility**: Message routing through multiple channels
- **Observer Pattern**: Message delivery tracking and analytics
- **Template Method**: Common message processing workflow

### 4. Cross-Cutting Concerns
- **Builder Pattern**: Complex configuration objects
- **Singleton Pattern**: Resource managers and registries
- **Decorator Pattern**: Enhanced functionality without modifying core classes

---

## Best Practices Demonstrated

1. **Separation of Concerns**: Each pattern addresses a specific responsibility
2. **Extensibility**: Easy to add new strategies, states, or handlers
3. **Testability**: Clear interfaces enable easy mocking and testing
4. **Performance**: Patterns optimized for high-throughput scenarios
5. **Resilience**: Built-in error handling and graceful degradation
6. **Monitoring**: Comprehensive metrics and logging integration

This implementation provides production-ready patterns that can handle the scale and complexity requirements of modern distributed systems like Netflix, Uber, and WhatsApp.
