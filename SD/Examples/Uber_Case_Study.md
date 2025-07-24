# Uber System Design Case Study

## Overview

Uber is a ride-sharing platform that connects riders with drivers in real-time. This case study explores the system design challenges and solutions implemented by Uber to handle millions of rides daily across the globe.

## Business Requirements

### Functional Requirements
1. **User Management**: Registration and authentication for riders and drivers
2. **Location Services**: Real-time location tracking and updates
3. **Matching Algorithm**: Efficient rider-driver matching
4. **Trip Management**: Trip lifecycle from request to completion
5. **Payment Processing**: Secure payment handling
6. **Pricing**: Dynamic pricing based on demand and supply
7. **ETA Calculation**: Accurate time estimates
8. **Notifications**: Real-time updates to users

### Non-Functional Requirements
1. **Scale**: Handle millions of users and trips daily
2. **Availability**: 99.9% uptime
3. **Latency**: Sub-second response for critical operations
4. **Consistency**: Eventually consistent data
5. **Reliability**: No data loss for critical information
6. **Security**: Secure user data and payments

## High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐
│   Mobile App    │    │   Web Client    │
│   (iOS/Android) │    │                 │
└─────────────────┘    └─────────────────┘
         │                       │
         └───────────┬───────────┘
                     │
         ┌─────────────────────────┐
         │     Load Balancer       │
         │     (API Gateway)       │
         └─────────────────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
┌───▼───┐    ┌──────▼──────┐    ┌───▼───┐
│User   │    │   Trip      │    │ Driver│
│Service│    │   Service   │    │Service│
└───────┘    └─────────────┘    └───────┘
    │                │                │
    └────────────────┼────────────────┘
                     │
         ┌─────────────────────────┐
         │   Location Service      │
         │   (Real-time tracking)  │
         └─────────────────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
┌───▼───┐    ┌──────▼──────┐    ┌───▼───┐
│Payment│    │ Notification│    │ Maps  │
│Service│    │   Service   │    │Service│
└───────┘    └─────────────┘    └───────┘
```

## Core Components

### 1. User Service
**Responsibilities:**
- User registration and authentication
- Profile management
- User preferences and settings

**Technology Stack:**
- **Database**: PostgreSQL for user profiles
- **Cache**: Redis for session management
- **Authentication**: JWT tokens

**Design Decisions:**
```python
# User data model
class User:
    def __init__(self):
        self.user_id = None
        self.phone_number = None  # Primary identifier
        self.email = None
        self.name = None
        self.profile_picture = None
        self.rating = None
        self.created_at = None
        self.is_active = None

# Partitioning strategy
def get_user_shard(phone_number):
    return hash(phone_number) % NUM_SHARDS
```

### 2. Location Service
**Responsibilities:**
- Real-time location tracking
- Geospatial queries
- Driver availability management

**Technology Stack:**
- **Database**: Redis with geospatial support
- **Message Queue**: Apache Kafka for location updates
- **CDN**: For map tiles and static content

**Design Decisions:**
```python
# Location update event
class LocationUpdate:
    def __init__(self, driver_id, latitude, longitude, timestamp):
        self.driver_id = driver_id
        self.latitude = latitude
        self.longitude = longitude
        self.timestamp = timestamp
        self.heading = None
        self.speed = None

# Geospatial indexing using Redis
def update_driver_location(driver_id, lat, lon):
    redis_client.geoadd("drivers", lon, lat, driver_id)

def find_nearby_drivers(lat, lon, radius_km):
    return redis_client.georadius("drivers", lon, lat, radius_km, unit="km")
```

### 3. Trip Service
**Responsibilities:**
- Trip creation and management
- State machine for trip lifecycle
- Trip history and analytics

**Technology Stack:**
- **Database**: Cassandra for trip data (time-series)
- **Event Streaming**: Kafka for trip events
- **State Management**: Redis for active trips

**Trip State Machine:**
```
[REQUESTED] → [MATCHED] → [ARRIVING] → [IN_PROGRESS] → [COMPLETED]
     ↓             ↓           ↓            ↓
[CANCELLED]   [CANCELLED] [CANCELLED]  [CANCELLED]
```

**Implementation:**
```python
class TripStateMachine:
    def __init__(self, trip_id):
        self.trip_id = trip_id
        self.state = TripState.REQUESTED
        self.transitions = {
            TripState.REQUESTED: [TripState.MATCHED, TripState.CANCELLED],
            TripState.MATCHED: [TripState.ARRIVING, TripState.CANCELLED],
            TripState.ARRIVING: [TripState.IN_PROGRESS, TripState.CANCELLED],
            TripState.IN_PROGRESS: [TripState.COMPLETED, TripState.CANCELLED]
        }
    
    def transition_to(self, new_state):
        if new_state in self.transitions[self.state]:
            old_state = self.state
            self.state = new_state
            self.publish_state_change_event(old_state, new_state)
            return True
        return False
```

### 4. Matching Service
**Responsibilities:**
- Efficient rider-driver matching
- Supply-demand optimization
- ETA calculations

**Matching Algorithm:**
```python
class DriverMatcher:
    def find_best_driver(self, trip_request):
        # Step 1: Find nearby drivers
        nearby_drivers = self.location_service.find_nearby_drivers(
            trip_request.pickup_lat,
            trip_request.pickup_lon,
            radius_km=5
        )
        
        # Step 2: Filter available drivers
        available_drivers = self.filter_available_drivers(nearby_drivers)
        
        # Step 3: Score drivers based on multiple factors
        scored_drivers = []
        for driver in available_drivers:
            score = self.calculate_driver_score(driver, trip_request)
            scored_drivers.append((driver, score))
        
        # Step 4: Return best match
        if scored_drivers:
            return max(scored_drivers, key=lambda x: x[1])[0]
        return None
    
    def calculate_driver_score(self, driver, trip_request):
        # Factors: distance, rating, acceptance rate, direction
        distance_factor = 1 / (1 + driver.distance_to_pickup)
        rating_factor = driver.rating / 5.0
        acceptance_factor = driver.acceptance_rate
        
        return distance_factor * 0.4 + rating_factor * 0.3 + acceptance_factor * 0.3
```

### 5. Pricing Service
**Responsibilities:**
- Dynamic pricing (surge pricing)
- Fare calculation
- Promotions and discounts

**Surge Pricing Algorithm:**
```python
class SurgePricingEngine:
    def calculate_surge_multiplier(self, area_id, current_time):
        # Get supply and demand metrics
        demand = self.get_demand_metrics(area_id, current_time)
        supply = self.get_supply_metrics(area_id, current_time)
        
        # Calculate supply-demand ratio
        ratio = demand / max(supply, 1)
        
        # Apply surge multiplier based on ratio
        if ratio < 1.2:
            return 1.0  # No surge
        elif ratio < 2.0:
            return 1.2
        elif ratio < 3.0:
            return 1.5
        else:
            return min(ratio * 0.8, 3.0)  # Cap at 3x
    
    def calculate_fare(self, trip):
        base_fare = self.get_base_fare(trip.vehicle_type)
        distance_fare = trip.distance_km * self.get_distance_rate()
        time_fare = trip.duration_minutes * self.get_time_rate()
        surge_multiplier = self.calculate_surge_multiplier(trip.area_id, trip.start_time)
        
        total_fare = (base_fare + distance_fare + time_fare) * surge_multiplier
        return max(total_fare, self.get_minimum_fare())
```

## Data Models

### Trip Data Model
```python
class Trip:
    def __init__(self):
        self.trip_id = None  # UUID
        self.rider_id = None
        self.driver_id = None
        
        # Location data
        self.pickup_location = None  # (lat, lon)
        self.dropoff_location = None  # (lat, lon)
        self.pickup_address = None
        self.dropoff_address = None
        
        # Timing
        self.requested_at = None
        self.matched_at = None
        self.pickup_at = None
        self.dropoff_at = None
        
        # Trip details
        self.distance_km = None
        self.duration_minutes = None
        self.vehicle_type = None
        self.fare = None
        self.surge_multiplier = None
        
        # Status
        self.status = None
        self.cancellation_reason = None
```

### Driver Data Model
```python
class Driver:
    def __init__(self):
        self.driver_id = None
        self.user_id = None  # Reference to User
        
        # Vehicle information
        self.vehicle_make = None
        self.vehicle_model = None
        self.vehicle_year = None
        self.license_plate = None
        
        # Current state
        self.is_online = False
        self.current_location = None
        self.heading = None
        self.is_available = True
        
        # Performance metrics
        self.rating = None
        self.acceptance_rate = None
        self.completion_rate = None
        self.total_trips = None
```

## Database Design

### Sharding Strategy
```python
# User and Driver data sharded by user_id
def get_user_shard(user_id):
    return hash(user_id) % NUM_USER_SHARDS

# Trip data sharded by trip_id and partitioned by time
def get_trip_shard(trip_id, timestamp):
    time_partition = timestamp.strftime("%Y%m")
    shard_id = hash(trip_id) % NUM_TRIP_SHARDS
    return f"trips_{time_partition}_{shard_id}"

# Location data in Redis clusters by geographic region
def get_location_cluster(lat, lon):
    # Divide world into geographic regions
    region_lat = int(lat / REGION_SIZE)
    region_lon = int(lon / REGION_SIZE)
    return f"location_{region_lat}_{region_lon}"
```

### Replication Strategy
- **Master-Slave**: For user and driver data
- **Multi-Master**: For trip data across regions
- **Redis Cluster**: For real-time location data

## Scalability Solutions

### 1. Horizontal Scaling
```python
# Load balancing strategies
class LoadBalancer:
    def route_request(self, request):
        if request.type == "location_update":
            return self.route_to_location_service(request)
        elif request.type == "trip_request":
            return self.route_to_trip_service(request)
        else:
            return self.route_round_robin(request)
    
    def route_to_location_service(self, request):
        # Route based on geographic region
        region = self.get_region(request.lat, request.lon)
        return self.location_services[region]
```

### 2. Caching Strategy
```python
# Multi-level caching
class CacheStrategy:
    def get_driver_info(self, driver_id):
        # L1: Application cache
        if driver_id in self.app_cache:
            return self.app_cache[driver_id]
        
        # L2: Redis cache
        cached_data = self.redis.get(f"driver:{driver_id}")
        if cached_data:
            driver_info = json.loads(cached_data)
            self.app_cache[driver_id] = driver_info
            return driver_info
        
        # L3: Database
        driver_info = self.database.get_driver(driver_id)
        self.redis.setex(f"driver:{driver_id}", 300, json.dumps(driver_info))
        self.app_cache[driver_id] = driver_info
        return driver_info
```

### 3. Real-time Communication
```python
# WebSocket connections for real-time updates
class RealtimeService:
    def __init__(self):
        self.connections = {}  # user_id -> websocket_connection
        self.kafka_consumer = KafkaConsumer('location_updates', 'trip_updates')
    
    def handle_location_update(self, update):
        # Send to connected riders and drivers
        affected_users = self.get_users_in_area(update.lat, update.lon)
        for user_id in affected_users:
            if user_id in self.connections:
                self.send_update(user_id, update)
    
    def handle_trip_update(self, trip_update):
        # Send to rider and driver
        self.send_update(trip_update.rider_id, trip_update)
        self.send_update(trip_update.driver_id, trip_update)
```

## Reliability and Fault Tolerance

### 1. Circuit Breaker Pattern
```python
class ExternalServiceCircuitBreaker:
    def call_maps_service(self, request):
        try:
            return self.maps_circuit_breaker.call(self.maps_api.get_directions, request)
        except CircuitBreakerOpenException:
            # Fallback to cached routes or simplified calculation
            return self.get_fallback_route(request)
```

### 2. Retry Mechanisms
```python
class RetryableOperations:
    @retry(max_attempts=3, backoff_factor=2)
    def send_push_notification(self, user_id, message):
        return self.notification_service.send(user_id, message)
    
    @retry(max_attempts=5, backoff_factor=1.5)
    def process_payment(self, payment_data):
        return self.payment_service.charge(payment_data)
```

### 3. Data Consistency
```python
# Eventual consistency with compensation
class TripCompensationHandler:
    def handle_driver_cancellation(self, trip_id):
        # Start saga for finding new driver
        saga = FindNewDriverSaga(trip_id)
        saga.start()
        
        # If no driver found within timeout, refund and notify
        self.schedule_compensation(trip_id, timeout=300)
    
    def schedule_compensation(self, trip_id, timeout):
        # Schedule compensation job
        self.scheduler.schedule_job(
            self.compensate_trip,
            run_date=datetime.now() + timedelta(seconds=timeout),
            args=[trip_id]
        )
```

## Monitoring and Observability

### Key Metrics
1. **Business Metrics**:
   - Trips per second
   - Match rate
   - Cancellation rate
   - Average ETA accuracy

2. **Technical Metrics**:
   - API response times
   - Database query performance
   - Cache hit rates
   - Error rates

3. **Real-time Dashboards**:
   - Driver availability heatmap
   - Demand prediction
   - System health status

### Implementation
```python
class MetricsCollector:
    def record_trip_request(self, trip_request):
        self.metrics.increment("trip.requested", tags={
            "city": trip_request.city,
            "vehicle_type": trip_request.vehicle_type
        })
    
    def record_match_time(self, match_time_seconds):
        self.metrics.histogram("trip.match_time", match_time_seconds)
    
    def record_api_latency(self, endpoint, latency_ms):
        self.metrics.histogram("api.latency", latency_ms, tags={
            "endpoint": endpoint
        })
```

## Security Considerations

### 1. Data Protection
- Encrypt sensitive data (PII, payment info)
- Use HTTPS for all communications
- Implement proper authentication and authorization

### 2. Location Privacy
- Anonymize location data after trip completion
- Implement location sharing controls
- Regular data purging policies

### 3. Fraud Prevention
```python
class FraudDetectionService:
    def detect_suspicious_activity(self, trip):
        risk_score = 0
        
        # Check for unusual patterns
        if self.is_unusual_route(trip):
            risk_score += 20
        
        if self.is_excessive_cancellations(trip.driver_id):
            risk_score += 30
        
        if self.is_location_spoofing(trip):
            risk_score += 50
        
        if risk_score > 70:
            self.flag_for_review(trip)
```

## Lessons Learned

### 1. Start Simple
- Begin with basic functionality
- Add complexity as scale demands
- Avoid premature optimization

### 2. Design for Failure
- Assume components will fail
- Implement graceful degradation
- Plan for disaster recovery

### 3. Data-Driven Decisions
- Measure everything
- A/B test new features
- Use metrics to guide architecture changes

### 4. Geographic Considerations
- Account for local regulations
- Handle different payment methods
- Consider cultural differences in UX

## Conclusion

Uber's system design demonstrates how to build a scalable, real-time platform that handles complex geographic and temporal challenges. Key architectural decisions include:

1. **Microservices architecture** for independent scaling
2. **Event-driven design** for real-time updates
3. **Geospatial indexing** for efficient location queries
4. **Dynamic pricing algorithms** for supply-demand balance
5. **Robust matching algorithms** for optimal rider-driver pairing

The system's success lies in its ability to handle massive scale while maintaining sub-second response times for critical operations like driver matching and location updates.
