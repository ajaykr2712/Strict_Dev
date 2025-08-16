# Real-Time Data Streaming Architecture

This guide covers real-time data streaming patterns and architectures used by companies like Netflix, Uber, and WhatsApp to process millions of events per second.

## 1. Stream Processing Fundamentals

### Event-Driven Architecture
Real-time streaming systems are built around events that represent changes in system state. These events flow through pipelines for processing, transformation, and storage.

### Key Concepts
- **Event Streams**: Continuous flows of data records
- **Stream Processors**: Applications that consume, process, and produce streams
- **Event Time vs Processing Time**: When events occurred vs when they're processed
- **Windowing**: Grouping events by time intervals
- **Watermarks**: Handling late-arriving events

## 2. Netflix Real-Time Analytics

### User Engagement Streaming
```yaml
# Netflix streaming pipeline configuration
stream_pipeline:
  name: "user-engagement-analytics"
  input_topics:
    - "user-views"
    - "user-interactions"
    - "content-metadata"
  
  processors:
    - name: "view-aggregator"
      type: "tumbling-window"
      window_size: "5 minutes"
      operations:
        - count_by_content_id
        - sum_watch_duration
        - calculate_completion_rate
    
    - name: "real-time-recommendations"
      type: "stream-processor"
      operations:
        - enrich_with_user_profile
        - calculate_affinity_scores
        - generate_recommendations
    
  output_sinks:
    - type: "kafka"
      topic: "processed-analytics"
    - type: "elasticsearch"
      index: "user-engagement"
    - type: "redis"
      key_pattern: "rec:{user_id}"
```

### Implementation Example
```java
// Kafka Streams processor for Netflix-style analytics
public class UserEngagementProcessor {
    
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        
        // Input streams
        KStream<String, ViewEvent> viewEvents = builder.stream("user-views");
        KStream<String, InteractionEvent> interactions = builder.stream("user-interactions");
        KTable<String, ContentMetadata> contentMetadata = builder.table("content-metadata");
        
        // Process view events with windowing
        KTable<Windowed<String>, ViewAggregation> viewAggregations = viewEvents
            .groupBy((key, viewEvent) -> viewEvent.getContentId())
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
            .aggregate(
                ViewAggregation::new,
                (contentId, viewEvent, aggregation) -> {
                    aggregation.incrementViews();
                    aggregation.addWatchDuration(viewEvent.getDuration());
                    return aggregation;
                },
                Materialized.with(Serdes.String(), new ViewAggregationSerde())
            );
        
        // Enrich with content metadata
        KStream<String, EnrichedViewEvent> enrichedViews = viewEvents
            .join(contentMetadata,
                (viewEvent, metadata) -> new EnrichedViewEvent(viewEvent, metadata),
                Joined.with(Serdes.String(), new ViewEventSerde(), new ContentMetadataSerde()));
        
        // Real-time recommendation processing
        enrichedViews
            .filter((key, enrichedView) -> enrichedView.getWatchDuration() > 300) // 5+ minutes
            .groupByKey()
            .windowedBy(SessionWindows.with(Duration.ofMinutes(30)))
            .aggregate(
                UserSession::new,
                (userId, enrichedView, session) -> {
                    session.addView(enrichedView);
                    return session;
                },
                (userId, session1, session2) -> session1.merge(session2),
                Materialized.with(Serdes.String(), new UserSessionSerde())
            )
            .toStream()
            .foreach((windowedUserId, session) -> {
                generateRealTimeRecommendations(windowedUserId.key(), session);
            });
        
        // Output aggregated data
        viewAggregations
            .toStream()
            .map((windowed, aggregation) -> KeyValue.pair(
                windowed.key(),
                new AnalyticsOutput(windowed.window(), aggregation)
            ))
            .to("processed-analytics");
        
        return builder.build();
    }
    
    private void generateRealTimeRecommendations(String userId, UserSession session) {
        // ML-based recommendation generation
        List<String> recommendations = recommendationEngine.generateRecommendations(
            userId, 
            session.getViewedContent(),
            session.getGenrePreferences()
        );
        
        // Store in Redis for fast access
        redisTemplate.opsForValue().set(
            "rec:" + userId, 
            recommendations, 
            Duration.ofHours(2)
        );
    }
}
```

## 3. Uber Real-Time Location Tracking

### Driver Location Streaming
```yaml
# Uber driver tracking pipeline
location_pipeline:
  name: "driver-location-tracking"
  
  input_sources:
    - type: "kafka"
      topic: "driver-locations"
      partitions: 64
      replication_factor: 3
  
  stream_processors:
    - name: "location-validator"
      operations:
        - validate_coordinates
        - filter_invalid_locations
        - detect_anomalies
    
    - name: "geohash-processor"
      operations:
        - calculate_geohash
        - group_by_region
        - update_spatial_index
    
    - name: "eta-calculator"
      operations:
        - calculate_eta_to_requests
        - update_driver_availability
        - trigger_matching_algorithm
  
  output_destinations:
    - type: "redis-geo"
      key_pattern: "drivers:{region}"
    - type: "kafka"
      topic: "driver-availability-updates"
    - type: "websocket"
      endpoint: "/driver-updates"
```

### Real-Time ETA Calculation
```java
@Component
public class RealTimeETAProcessor {
    
    @KafkaListener(topics = "driver-locations")
    public void processLocationUpdate(DriverLocationEvent locationEvent) {
        
        // Validate location data
        if (!isValidLocation(locationEvent)) {
            return;
        }
        
        // Update driver's current location
        updateDriverLocation(locationEvent);
        
        // Calculate ETA to nearby ride requests
        List<RideRequest> nearbyRequests = findNearbyRideRequests(
            locationEvent.getLatitude(), 
            locationEvent.getLongitude(),
            5.0 // 5km radius
        );
        
        for (RideRequest request : nearbyRequests) {
            double eta = calculateETA(locationEvent, request);
            
            // Update ETA in real-time
            ETAUpdate etaUpdate = new ETAUpdate(
                locationEvent.getDriverId(),
                request.getRequestId(),
                eta,
                System.currentTimeMillis()
            );
            
            kafkaTemplate.send("eta-updates", etaUpdate);
            
            // Check if this driver should be matched
            if (eta <= 5.0 && isDriverAvailable(locationEvent.getDriverId())) {
                triggerMatchingAlgorithm(locationEvent.getDriverId(), request);
            }
        }
        
        // Update geospatial index
        updateGeoIndex(locationEvent);
    }
    
    private double calculateETA(DriverLocationEvent driverLocation, RideRequest request) {
        // Use external routing service or pre-computed travel times
        double distance = calculateDistance(
            driverLocation.getLatitude(), driverLocation.getLongitude(),
            request.getPickupLatitude(), request.getPickupLongitude()
        );
        
        // Factor in traffic conditions
        double trafficMultiplier = getTrafficMultiplier(
            driverLocation.getLatitude(), driverLocation.getLongitude(),
            request.getPickupLatitude(), request.getPickupLongitude()
        );
        
        // Base speed assumption: 30 km/h in city traffic
        double baseSpeedKmh = 30.0;
        double adjustedSpeed = baseSpeedKmh / trafficMultiplier;
        
        return distance / adjustedSpeed * 60; // Return ETA in minutes
    }
    
    private void updateGeoIndex(DriverLocationEvent locationEvent) {
        // Update Redis geospatial index
        redisTemplate.opsForGeo().add(
            "drivers:active",
            new Point(locationEvent.getLongitude(), locationEvent.getLatitude()),
            locationEvent.getDriverId()
        );
        
        // Set expiration for location data
        redisTemplate.expire("drivers:active", Duration.ofMinutes(5));
    }
}
```

## 4. WhatsApp Message Delivery Pipeline

### Message Streaming Architecture
```yaml
# WhatsApp message processing pipeline
message_pipeline:
  name: "message-delivery-system"
  
  ingestion:
    - type: "kafka"
      topic: "incoming-messages"
      partitions: 128
      partition_strategy: "hash_by_chat_id"
  
  processing_stages:
    - name: "message-validation"
      operations:
        - validate_sender_permissions
        - check_spam_filters
        - encrypt_content
    
    - name: "delivery-routing"
      operations:
        - determine_recipient_servers
        - check_recipient_online_status
        - route_to_appropriate_delivery_pipeline
    
    - name: "delivery-tracking"
      operations:
        - track_delivery_status
        - handle_delivery_confirmations
        - manage_retry_logic
  
  delivery_channels:
    - type: "websocket"
      for: "online_users"
    - type: "push_notification"
      for: "offline_users"
    - type: "sms_fallback"
      for: "delivery_failures"
```

### Message Processing Implementation
```java
@Service
public class MessageDeliveryProcessor {
    
    @StreamListener("incoming-messages")
    public void processIncomingMessage(Message message) {
        
        try {
            // Stage 1: Validate message
            ValidationResult validation = validateMessage(message);
            if (!validation.isValid()) {
                sendDeliveryStatus(message.getSenderId(), message.getMessageId(), 
                                 DeliveryStatus.FAILED, validation.getErrorReason());
                return;
            }
            
            // Stage 2: Encrypt content
            EncryptedMessage encryptedMessage = encryptionService.encrypt(message);
            
            // Stage 3: Determine delivery strategy
            List<String> recipients = getMessageRecipients(message);
            
            for (String recipientId : recipients) {
                DeliveryStrategy strategy = determineDeliveryStrategy(recipientId);
                
                switch (strategy) {
                    case REAL_TIME:
                        deliverRealTime(encryptedMessage, recipientId);
                        break;
                    case PUSH_NOTIFICATION:
                        sendPushNotification(encryptedMessage, recipientId);
                        break;
                    case STORE_AND_FORWARD:
                        storeForLaterDelivery(encryptedMessage, recipientId);
                        break;
                }
            }
            
            // Stage 4: Track delivery
            trackMessageDelivery(message.getMessageId(), recipients);
            
        } catch (Exception e) {
            handleDeliveryError(message, e);
        }
    }
    
    private DeliveryStrategy determineDeliveryStrategy(String recipientId) {
        // Check if recipient is online
        boolean isOnline = presenceService.isUserOnline(recipientId);
        
        if (isOnline) {
            return DeliveryStrategy.REAL_TIME;
        }
        
        // Check last seen timestamp
        long lastSeen = presenceService.getLastSeenTimestamp(recipientId);
        long timeSinceLastSeen = System.currentTimeMillis() - lastSeen;
        
        if (timeSinceLastSeen < Duration.ofHours(1).toMillis()) {
            return DeliveryStrategy.PUSH_NOTIFICATION;
        }
        
        return DeliveryStrategy.STORE_AND_FORWARD;
    }
    
    @EventListener
    public void handleDeliveryConfirmation(DeliveryConfirmationEvent event) {
        // Update message status
        messageStatusService.updateDeliveryStatus(
            event.getMessageId(),
            event.getRecipientId(),
            DeliveryStatus.DELIVERED,
            event.getTimestamp()
        );
        
        // Send read receipt to sender
        sendReadReceipt(event.getMessageId(), event.getSenderId());
        
        // Update analytics
        analyticsService.recordMessageDelivery(
            event.getMessageId(),
            event.getDeliveryTime() - event.getSentTime()
        );
    }
}
```

## 5. Stream Processing Patterns

### Windowing Strategies

#### Tumbling Windows
```java
// Netflix view count aggregation
public class TumblingWindowProcessor {
    
    public void configureTumblingWindow(StreamsBuilder builder) {
        KStream<String, ViewEvent> viewStream = builder.stream("view-events");
        
        viewStream
            .groupBy((key, viewEvent) -> viewEvent.getContentId())
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5))) // 5-minute tumbling windows
            .count()
            .toStream()
            .foreach((windowedContentId, count) -> {
                String contentId = windowedContentId.key();
                long windowStart = windowedContentId.window().start();
                long windowEnd = windowedContentId.window().end();
                
                // Update real-time dashboard
                dashboardService.updateViewCount(contentId, count, windowStart, windowEnd);
            });
    }
}
```

#### Sliding Windows
```java
// Uber surge pricing calculation
public class SlidingWindowProcessor {
    
    public void configureSlidingWindow(StreamsBuilder builder) {
        KStream<String, RideRequest> rideRequests = builder.stream("ride-requests");
        
        rideRequests
            .groupBy((key, request) -> request.getRegionId())
            .windowedBy(TimeWindows.of(Duration.ofMinutes(10)).advanceBy(Duration.ofMinutes(1)))
            .count()
            .toStream()
            .foreach((windowedRegion, requestCount) -> {
                String regionId = windowedRegion.key();
                
                // Calculate surge multiplier based on demand
                double surgeMultiplier = calculateSurgeMultiplier(requestCount);
                
                pricingService.updateSurgePricing(regionId, surgeMultiplier);
            });
    }
}
```

#### Session Windows
```java
// WhatsApp conversation session tracking
public class SessionWindowProcessor {
    
    public void configureSessionWindow(StreamsBuilder builder) {
        KStream<String, MessageEvent> messages = builder.stream("chat-messages");
        
        messages
            .groupBy((key, message) -> message.getChatId())
            .windowedBy(SessionWindows.with(Duration.ofMinutes(30))) // 30-minute inactivity gap
            .aggregate(
                ChatSession::new,
                (chatId, message, session) -> {
                    session.addMessage(message);
                    return session;
                },
                (chatId, session1, session2) -> session1.merge(session2)
            )
            .toStream()
            .foreach((windowedChatId, session) -> {
                // Analyze conversation patterns
                conversationAnalytics.analyzeChatSession(
                    windowedChatId.key(), 
                    session
                );
            });
    }
}
```

## 6. Exactly-Once Processing

### Kafka Transactions
```java
@Service
public class ExactlyOnceProcessor {
    
    @Autowired
    private KafkaTransactionManager transactionManager;
    
    @KafkaListener(topics = "payment-events")
    @Transactional
    public void processPayment(PaymentEvent paymentEvent) {
        
        try {
            // Process payment
            PaymentResult result = paymentService.processPayment(paymentEvent);
            
            // Update user balance
            userAccountService.updateBalance(
                paymentEvent.getUserId(), 
                paymentEvent.getAmount()
            );
            
            // Send confirmation
            PaymentConfirmation confirmation = new PaymentConfirmation(
                paymentEvent.getPaymentId(),
                result.getStatus(),
                System.currentTimeMillis()
            );
            
            kafkaTemplate.send("payment-confirmations", confirmation);
            
            // Update analytics
            analyticsService.recordPayment(paymentEvent, result);
            
        } catch (Exception e) {
            // Transaction will be rolled back automatically
            log.error("Payment processing failed", e);
            throw e;
        }
    }
}
```

## 7. Stream Joins

### Stream-Stream Joins
```java
// Join ride requests with driver locations
public class RideMatchingProcessor {
    
    public void configureStreamJoins(StreamsBuilder builder) {
        KStream<String, RideRequest> rideRequests = builder.stream("ride-requests");
        KStream<String, DriverLocation> driverLocations = builder.stream("driver-locations");
        
        // Join within 1-minute window
        KStream<String, RideMatch> matches = rideRequests
            .join(driverLocations,
                (request, location) -> new RideMatch(request, location),
                JoinWindows.of(Duration.ofMinutes(1)),
                StreamJoined.with(
                    Serdes.String(),
                    new RideRequestSerde(),
                    new DriverLocationSerde()
                )
            );
        
        matches
            .filter((key, match) -> isValidMatch(match))
            .to("ride-matches");
    }
}
```

### Stream-Table Joins
```java
// Enrich messages with user profiles
public class MessageEnrichmentProcessor {
    
    public void configureStreamTableJoin(StreamsBuilder builder) {
        KStream<String, Message> messages = builder.stream("messages");
        KTable<String, UserProfile> userProfiles = builder.table("user-profiles");
        
        KStream<String, EnrichedMessage> enrichedMessages = messages
            .join(userProfiles,
                (message, profile) -> new EnrichedMessage(message, profile),
                Joined.with(
                    Serdes.String(),
                    new MessageSerde(),
                    new UserProfileSerde()
                )
            );
        
        enrichedMessages.to("enriched-messages");
    }
}
```

## 8. Error Handling and Dead Letter Queues

### Dead Letter Queue Pattern
```java
@Component
public class StreamErrorHandler {
    
    @KafkaListener(topics = "failed-messages")
    public void handleFailedMessage(ConsumerRecord<String, String> record) {
        
        try {
            // Attempt to reprocess
            processMessage(record.value());
            
        } catch (RetryableException e) {
            // Send to retry topic
            kafkaTemplate.send("retry-messages", record.value());
            
        } catch (NonRetryableException e) {
            // Send to dead letter queue
            sendToDeadLetterQueue(record, e);
        }
    }
    
    private void sendToDeadLetterQueue(ConsumerRecord<String, String> record, Exception error) {
        FailedMessage failedMessage = new FailedMessage(
            record.key(),
            record.value(),
            error.getMessage(),
            System.currentTimeMillis(),
            record.topic(),
            record.partition(),
            record.offset()
        );
        
        kafkaTemplate.send("dead-letter-queue", failedMessage);
        
        // Alert monitoring system
        alertService.sendAlert("Message processing failed", failedMessage);
    }
}
```

## 9. Monitoring and Observability

### Stream Processing Metrics
```java
@Component
public class StreamMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public void recordProcessingLatency(String processorName, long latencyMs) {
        Timer.builder("stream.processing.latency")
            .tag("processor", processorName)
            .register(meterRegistry)
            .record(latencyMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordThroughput(String topicName, long messageCount) {
        Counter.builder("stream.messages.processed")
            .tag("topic", topicName)
            .register(meterRegistry)
            .increment(messageCount);
    }
    
    public void recordProcessingError(String processorName, String errorType) {
        Counter.builder("stream.processing.errors")
            .tag("processor", processorName)
            .tag("error_type", errorType)
            .register(meterRegistry)
            .increment();
    }
}
```

## Best Practices

1. **Design for Scale**: Partition data effectively for parallel processing
2. **Handle Late Data**: Implement watermarking and late data handling
3. **Ensure Exactly-Once**: Use transactions for critical data processing
4. **Monitor Performance**: Track latency, throughput, and error rates
5. **Plan for Failures**: Implement retry logic and dead letter queues
6. **Optimize Serialization**: Use efficient serialization formats
7. **Manage State**: Use appropriate state stores for stateful operations
8. **Version Schemas**: Plan for schema evolution in streaming data
9. **Test Thoroughly**: Include unit tests, integration tests, and chaos testing
10. **Implement Backpressure**: Handle varying load conditions gracefully
