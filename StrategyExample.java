import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Strategy Pattern Implementation for Uber-like Ride-sharing Platform
 * 
 * Real-world Use Case: Dynamic pricing strategies based on different factors
 * - Peak hour pricing during rush hours
 * - Weather-based pricing during bad weather conditions
 * - Event-based pricing during special events (concerts, sports games)
 * - Distance-based pricing for long trips
 * - Supply-demand pricing based on driver availability
 * 
 * This demonstrates how different pricing algorithms can be swapped at runtime
 * without changing the core pricing engine code.
 */

// Strategy interface - defines the contract for all pricing strategies
interface PricingStrategy {
    double calculatePrice(RideRequest rideRequest, MarketConditions conditions);
    String getStrategyName();
    String getDescription();
}

// Context data classes
class RideRequest {
    private final String rideId;
    private final String userId;
    private final Location pickupLocation;
    private final Location dropoffLocation;
    private final String rideType; // STANDARD, PREMIUM, SHARED
    private final LocalDateTime requestTime;
    private final double baseDistance; // in kilometers

    public RideRequest(String rideId, String userId, Location pickupLocation, 
                      Location dropoffLocation, String rideType, double baseDistance) {
        this.rideId = rideId;
        this.userId = userId;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.rideType = rideType;
        this.baseDistance = baseDistance;
        this.requestTime = LocalDateTime.now();
    }

    // Getters
    public String getRideId() { return rideId; }
    public String getUserId() { return userId; }
    public Location getPickupLocation() { return pickupLocation; }
    public Location getDropoffLocation() { return dropoffLocation; }
    public String getRideType() { return rideType; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public double getBaseDistance() { return baseDistance; }
}

class Location {
    private final double latitude;
    private final double longitude;
    private final String name;

    public Location(double latitude, double longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name + " (" + latitude + ", " + longitude + ")";
    }
}

class MarketConditions {
    private final int availableDrivers;
    private final int activeRideRequests;
    private final String weatherCondition; // CLEAR, RAIN, SNOW, STORM
    private final boolean isSpecialEvent;
    private final String eventType; // CONCERT, SPORTS, CONFERENCE, etc.
    private final double trafficMultiplier; // 1.0 = normal, >1.0 = heavy traffic

    public MarketConditions(int availableDrivers, int activeRideRequests, 
                          String weatherCondition, boolean isSpecialEvent, 
                          String eventType, double trafficMultiplier) {
        this.availableDrivers = availableDrivers;
        this.activeRideRequests = activeRideRequests;
        this.weatherCondition = weatherCondition;
        this.isSpecialEvent = isSpecialEvent;
        this.eventType = eventType;
        this.trafficMultiplier = trafficMultiplier;
    }

    // Getters
    public int getAvailableDrivers() { return availableDrivers; }
    public int getActiveRideRequests() { return activeRideRequests; }
    public String getWeatherCondition() { return weatherCondition; }
    public boolean isSpecialEvent() { return isSpecialEvent; }
    public String getEventType() { return eventType; }
    public double getTrafficMultiplier() { return trafficMultiplier; }

    public double getSupplyDemandRatio() {
        if (activeRideRequests == 0) return Double.MAX_VALUE;
        return (double) availableDrivers / activeRideRequests;
    }
}

// Concrete Strategy - Base pricing with standard rates
class BasePricingStrategy implements PricingStrategy {
    private static final double BASE_RATE = 2.0; // Base fare
    private static final double PER_KM_RATE = 1.5; // Rate per kilometer
    private static final double PER_MINUTE_RATE = 0.3; // Rate per minute
    
    @Override
    public double calculatePrice(RideRequest request, MarketConditions conditions) {
        double basePrice = BASE_RATE;
        double distancePrice = request.getBaseDistance() * PER_KM_RATE;
        
        // Estimate time based on distance and traffic
        double estimatedMinutes = (request.getBaseDistance() / 30.0) * 60 * conditions.getTrafficMultiplier();
        double timePrice = estimatedMinutes * PER_MINUTE_RATE;
        
        // Apply ride type multiplier
        double typeMultiplier = getRideTypeMultiplier(request.getRideType());
        
        return (basePrice + distancePrice + timePrice) * typeMultiplier;
    }

    private double getRideTypeMultiplier(String rideType) {
        return switch (rideType) {
            case "PREMIUM" -> 1.5;
            case "SHARED" -> 0.8;
            default -> 1.0; // STANDARD
        };
    }

    @Override
    public String getStrategyName() {
        return "Base Pricing";
    }

    @Override
    public String getDescription() {
        return "Standard pricing based on distance, time, and ride type";
    }
}

// Concrete Strategy - Peak hour surge pricing
class PeakHourPricingStrategy implements PricingStrategy {
    private final PricingStrategy baseStrategy;
    
    public PeakHourPricingStrategy() {
        this.baseStrategy = new BasePricingStrategy();
    }

    @Override
    public double calculatePrice(RideRequest request, MarketConditions conditions) {
        double basePrice = baseStrategy.calculatePrice(request, conditions);
        double surgeMultiplier = calculatePeakHourMultiplier(request.getRequestTime().toLocalTime());
        
        return basePrice * surgeMultiplier;
    }

    private double calculatePeakHourMultiplier(LocalTime time) {
        int hour = time.getHour();
        
        // Morning rush: 7-10 AM
        if (hour >= 7 && hour <= 10) {
            return 1.8;
        }
        // Evening rush: 5-8 PM
        else if (hour >= 17 && hour <= 20) {
            return 2.0;
        }
        // Late night: 11 PM - 2 AM (weekend premium)
        else if (hour >= 23 || hour <= 2) {
            return 1.5;
        }
        // Regular hours
        else {
            return 1.0;
        }
    }

    @Override
    public String getStrategyName() {
        return "Peak Hour Pricing";
    }

    @Override
    public String getDescription() {
        return "Surge pricing during peak hours (rush hours and late nights)";
    }
}

// Concrete Strategy - Weather-based pricing
class WeatherBasedPricingStrategy implements PricingStrategy {
    private final PricingStrategy baseStrategy;
    
    public WeatherBasedPricingStrategy() {
        this.baseStrategy = new BasePricingStrategy();
    }

    @Override
    public double calculatePrice(RideRequest request, MarketConditions conditions) {
        double basePrice = baseStrategy.calculatePrice(request, conditions);
        double weatherMultiplier = getWeatherMultiplier(conditions.getWeatherCondition());
        
        return basePrice * weatherMultiplier;
    }

    private double getWeatherMultiplier(String weatherCondition) {
        return switch (weatherCondition) {
            case "STORM" -> 2.5;
            case "SNOW" -> 2.0;
            case "RAIN" -> 1.4;
            case "CLEAR" -> 1.0;
            default -> 1.2; // Unknown weather, slight premium
        };
    }

    @Override
    public String getStrategyName() {
        return "Weather-Based Pricing";
    }

    @Override
    public String getDescription() {
        return "Dynamic pricing based on weather conditions";
    }
}

// Concrete Strategy - Supply and demand pricing
class SupplyDemandPricingStrategy implements PricingStrategy {
    private final PricingStrategy baseStrategy;
    
    public SupplyDemandPricingStrategy() {
        this.baseStrategy = new BasePricingStrategy();
    }

    @Override
    public double calculatePrice(RideRequest request, MarketConditions conditions) {
        double basePrice = baseStrategy.calculatePrice(request, conditions);
        double demandMultiplier = calculateDemandMultiplier(conditions);
        
        return basePrice * demandMultiplier;
    }

    private double calculateDemandMultiplier(MarketConditions conditions) {
        double supplyDemandRatio = conditions.getSupplyDemandRatio();
        
        // High demand, low supply - significant surge
        if (supplyDemandRatio < 0.3) {
            return 3.0;
        }
        // Moderate demand - moderate surge
        else if (supplyDemandRatio < 0.6) {
            return 2.0;
        }
        // Balanced supply/demand - slight surge
        else if (supplyDemandRatio < 1.0) {
            return 1.3;
        }
        // High supply, low demand - base price
        else {
            return 1.0;
        }
    }

    @Override
    public String getStrategyName() {
        return "Supply-Demand Pricing";
    }

    @Override
    public String getDescription() {
        return "Dynamic pricing based on driver availability vs ride requests";
    }
}

// Concrete Strategy - Event-based pricing
class EventBasedPricingStrategy implements PricingStrategy {
    private final PricingStrategy baseStrategy;
    
    public EventBasedPricingStrategy() {
        this.baseStrategy = new BasePricingStrategy();
    }

    @Override
    public double calculatePrice(RideRequest request, MarketConditions conditions) {
        double basePrice = baseStrategy.calculatePrice(request, conditions);
        
        if (!conditions.isSpecialEvent()) {
            return basePrice;
        }
        
        double eventMultiplier = getEventMultiplier(conditions.getEventType());
        return basePrice * eventMultiplier;
    }

    private double getEventMultiplier(String eventType) {
        return switch (eventType) {
            case "CONCERT" -> 2.2;
            case "SPORTS" -> 1.8;
            case "CONFERENCE" -> 1.3;
            case "FESTIVAL" -> 2.5;
            default -> 1.5; // General events
        };
    }

    @Override
    public String getStrategyName() {
        return "Event-Based Pricing";
    }

    @Override
    public String getDescription() {
        return "Premium pricing during special events";
    }
}

// Context class - Uber Pricing Engine
class UberPricingEngine {
    private PricingStrategy currentStrategy;
    private final Map<String, PricingStrategy> availableStrategies;

    public UberPricingEngine() {
        this.availableStrategies = new HashMap<>();
        initializeStrategies();
        this.currentStrategy = availableStrategies.get("BASE");
    }

    private void initializeStrategies() {
        availableStrategies.put("BASE", new BasePricingStrategy());
        availableStrategies.put("PEAK_HOUR", new PeakHourPricingStrategy());
        availableStrategies.put("WEATHER", new WeatherBasedPricingStrategy());
        availableStrategies.put("SUPPLY_DEMAND", new SupplyDemandPricingStrategy());
        availableStrategies.put("EVENT", new EventBasedPricingStrategy());
    }

    public void setPricingStrategy(String strategyName) {
        PricingStrategy strategy = availableStrategies.get(strategyName.toUpperCase());
        if (strategy != null) {
            this.currentStrategy = strategy;
            System.out.println("[PRICING-ENGINE] Switched to: " + strategy.getStrategyName());
        } else {
            System.out.println("[PRICING-ENGINE] Strategy not found: " + strategyName);
        }
    }

    public void setPricingStrategy(PricingStrategy strategy) {
        this.currentStrategy = strategy;
        System.out.println("[PRICING-ENGINE] Switched to: " + strategy.getStrategyName());
    }

    public double calculatePrice(RideRequest request, MarketConditions conditions) {
        double price = currentStrategy.calculatePrice(request, conditions);
        
        System.out.printf("[PRICING-ENGINE] %s calculated price: $%.2f for ride %s%n",
                currentStrategy.getStrategyName(), price, request.getRideId());
        
        return price;
    }

    public PricingStrategy getCurrentStrategy() {
        return currentStrategy;
    }

    public void printAvailableStrategies() {
        System.out.println("\n[PRICING-ENGINE] Available Pricing Strategies:");
        availableStrategies.values().forEach(strategy -> 
            System.out.println("  - " + strategy.getStrategyName() + ": " + strategy.getDescription())
        );
    }

    // Smart strategy selection based on current conditions
    public void selectOptimalStrategy(MarketConditions conditions, LocalDateTime requestTime) {
        List<String> applicableStrategies = new ArrayList<>();
        
        // Check if it's peak hour
        LocalTime time = requestTime.toLocalTime();
        int hour = time.getHour();
        if ((hour >= 7 && hour <= 10) || (hour >= 17 && hour <= 20)) {
            applicableStrategies.add("PEAK_HOUR");
        }
        
        // Check weather conditions
        if (!"CLEAR".equals(conditions.getWeatherCondition())) {
            applicableStrategies.add("WEATHER");
        }
        
        // Check supply-demand ratio
        if (conditions.getSupplyDemandRatio() < 1.0) {
            applicableStrategies.add("SUPPLY_DEMAND");
        }
        
        // Check for special events
        if (conditions.isSpecialEvent()) {
            applicableStrategies.add("EVENT");
        }
        
        // Select the strategy that would yield highest price (most applicable to current conditions)
        if (!applicableStrategies.isEmpty()) {
            // For demo, just pick the first applicable strategy
            // In real implementation, you might want more sophisticated logic
            setPricingStrategy(applicableStrategies.get(0));
        } else {
            setPricingStrategy("BASE");
        }
    }
}

// Demonstration class
class StrategyPatternDemo {
    public static void main(String[] args) {
        System.out.println("=== Strategy Pattern Demo: Uber Dynamic Pricing ===\n");

        // Create pricing engine
        UberPricingEngine pricingEngine = new UberPricingEngine();
        pricingEngine.printAvailableStrategies();

        // Create sample locations
        Location downtown = new Location(37.7749, -122.4194, "Downtown San Francisco");
        Location airport = new Location(37.6213, -122.3790, "SFO Airport");
        Location stadium = new Location(37.7033, -122.4097, "Oracle Park");

        System.out.println("\n=== Scenario 1: Normal conditions ===");
        RideRequest normalRide = new RideRequest("ride001", "user123", downtown, airport, "STANDARD", 25.0);
        MarketConditions normalConditions = new MarketConditions(50, 30, "CLEAR", false, "", 1.0);
        
        pricingEngine.setPricingStrategy("BASE");
        double normalPrice = pricingEngine.calculatePrice(normalRide, normalConditions);

        System.out.println("\n=== Scenario 2: Peak hour conditions ===");
        RideRequest peakRide = new RideRequest("ride002", "user456", downtown, airport, "STANDARD", 25.0);
        
        pricingEngine.setPricingStrategy("PEAK_HOUR");
        double peakPrice = pricingEngine.calculatePrice(peakRide, normalConditions);

        System.out.println("\n=== Scenario 3: Bad weather conditions ===");
        MarketConditions stormyConditions = new MarketConditions(30, 25, "STORM", false, "", 1.3);
        
        pricingEngine.setPricingStrategy("WEATHER");
        double stormPrice = pricingEngine.calculatePrice(normalRide, stormyConditions);

        System.out.println("\n=== Scenario 4: High demand conditions ===");
        MarketConditions highDemandConditions = new MarketConditions(15, 60, "CLEAR", false, "", 1.0);
        
        pricingEngine.setPricingStrategy("SUPPLY_DEMAND");
        double surgePrice = pricingEngine.calculatePrice(normalRide, highDemandConditions);

        System.out.println("\n=== Scenario 5: Special event conditions ===");
        RideRequest eventRide = new RideRequest("ride003", "user789", stadium, downtown, "PREMIUM", 8.0);
        MarketConditions eventConditions = new MarketConditions(25, 45, "CLEAR", true, "CONCERT", 1.2);
        
        pricingEngine.setPricingStrategy("EVENT");
        double eventPrice = pricingEngine.calculatePrice(eventRide, eventConditions);

        System.out.println("\n=== Scenario 6: Smart strategy selection ===");
        RideRequest smartRide = new RideRequest("ride004", "user999", downtown, airport, "STANDARD", 25.0);
        MarketConditions complexConditions = new MarketConditions(20, 50, "RAIN", true, "SPORTS", 1.4);
        
        System.out.println("[DEMO] Using smart strategy selection based on conditions:");
        System.out.println("  - High demand (20 drivers, 50 requests)");
        System.out.println("  - Rainy weather");
        System.out.println("  - Sports event happening");
        
        pricingEngine.selectOptimalStrategy(complexConditions, smartRide.getRequestTime());
        double smartPrice = pricingEngine.calculatePrice(smartRide, complexConditions);

        // Summary
        System.out.println("\n=== Price Comparison Summary ===");
        System.out.printf("Normal conditions:      $%.2f%n", normalPrice);
        System.out.printf("Peak hour:             $%.2f (%.1fx)%n", peakPrice, peakPrice / normalPrice);
        System.out.printf("Storm weather:         $%.2f (%.1fx)%n", stormPrice, stormPrice / normalPrice);
        System.out.printf("High demand:           $%.2f (%.1fx)%n", surgePrice, surgePrice / normalPrice);
        System.out.printf("Event pricing:         $%.2f%n", eventPrice);
        System.out.printf("Smart selection:       $%.2f%n", smartPrice);

        System.out.println("\n=== Strategy Pattern Benefits Demonstrated ===");
        System.out.println("✓ Algorithmic flexibility: Different pricing strategies can be used");
        System.out.println("✓ Runtime strategy switching: Strategies change based on market conditions");
        System.out.println("✓ Open/Closed principle: New strategies can be added without modifying existing code");
        System.out.println("✓ Strategy composition: Base strategy can be enhanced by other strategies");
    }
}
