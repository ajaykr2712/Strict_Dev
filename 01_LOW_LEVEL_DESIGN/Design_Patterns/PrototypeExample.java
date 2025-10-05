import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prototype Pattern Implementation
 * 
 * Real-world Use Case: Uber Ride Request Templates
 * Creating different types of ride requests by cloning existing templates
 */

public class PrototypeExample {
    
    // Prototype interface
    interface RideRequestPrototype extends Cloneable {
        RideRequestPrototype clone();
        void customize(Map<String, Object> parameters);
        String getRequestType();
    }
    
    // Abstract base class for ride requests
    abstract static class BaseRideRequest implements RideRequestPrototype {
        protected String requestId;
        protected String userId;
        protected String pickupLocation;
        protected String dropoffLocation;
        protected String rideType;
        protected double estimatedPrice;
        protected Map<String, Object> preferences;
        protected boolean isScheduled;
        
        public BaseRideRequest() {
            this.preferences = new HashMap<>();
            this.isScheduled = false;
        }
        
        @Override
        public BaseRideRequest clone() {
            try {
                BaseRideRequest cloned = (BaseRideRequest) super.clone();
                cloned.preferences = new HashMap<>(this.preferences);
                cloned.requestId = generateNewId();
                return cloned;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Clone not supported", e);
            }
        }
        
        private String generateNewId() {
            return "req_" + System.currentTimeMillis() + "_" + Math.random();
        }
        
        public void setUserId(String userId) { this.userId = userId; }
        public void setPickupLocation(String pickup) { this.pickupLocation = pickup; }
        public void setDropoffLocation(String dropoff) { this.dropoffLocation = dropoff; }
        public void setEstimatedPrice(double price) { this.estimatedPrice = price; }
        public void setScheduled(boolean scheduled) { this.isScheduled = scheduled; }
        
        @Override
        public String toString() {
            return String.format("%s{id='%s', user='%s', from='%s', to='%s', price=$%.2f, scheduled=%s}",
                    getRequestType(), requestId, userId, pickupLocation, dropoffLocation, estimatedPrice, isScheduled);
        }
    }
    
    // Concrete prototype - Standard ride
    static class StandardRideRequest extends BaseRideRequest {
        
        public StandardRideRequest() {
            super();
            this.rideType = "STANDARD";
            this.estimatedPrice = 15.0;
            this.preferences.put("vehicle_type", "sedan");
            this.preferences.put("max_wait_time", 5);
        }
        
        @Override
        public void customize(Map<String, Object> parameters) {
            if (parameters.containsKey("pickup")) {
                setPickupLocation((String) parameters.get("pickup"));
            }
            if (parameters.containsKey("dropoff")) {
                setDropoffLocation((String) parameters.get("dropoff"));
            }
            if (parameters.containsKey("user_id")) {
                setUserId((String) parameters.get("user_id"));
            }
            if (parameters.containsKey("price_multiplier")) {
                double multiplier = (Double) parameters.get("price_multiplier");
                setEstimatedPrice(estimatedPrice * multiplier);
            }
        }
        
        @Override
        public String getRequestType() {
            return "StandardRide";
        }
    }
    
    // Concrete prototype - Premium ride
    static class PremiumRideRequest extends BaseRideRequest {
        
        public PremiumRideRequest() {
            super();
            this.rideType = "PREMIUM";
            this.estimatedPrice = 25.0;
            this.preferences.put("vehicle_type", "luxury");
            this.preferences.put("max_wait_time", 3);
            this.preferences.put("amenities", Arrays.asList("wifi", "water", "phone_charger"));
        }
        
        @Override
        public void customize(Map<String, Object> parameters) {
            if (parameters.containsKey("pickup")) {
                setPickupLocation((String) parameters.get("pickup"));
            }
            if (parameters.containsKey("dropoff")) {
                setDropoffLocation((String) parameters.get("dropoff"));
            }
            if (parameters.containsKey("user_id")) {
                setUserId((String) parameters.get("user_id"));
            }
            if (parameters.containsKey("luxury_level")) {
                String level = (String) parameters.get("luxury_level");
                switch (level) {
                    case "EXECUTIVE":
                        setEstimatedPrice(estimatedPrice * 1.5);
                        preferences.put("vehicle_type", "executive");
                        break;
                    case "BLACK":
                        setEstimatedPrice(estimatedPrice * 2.0);
                        preferences.put("vehicle_type", "black_car");
                        break;
                }
            }
        }
        
        @Override
        public String getRequestType() {
            return "PremiumRide";
        }
    }
    
    // Concrete prototype - Shared ride
    static class SharedRideRequest extends BaseRideRequest {
        
        public SharedRideRequest() {
            super();
            this.rideType = "SHARED";
            this.estimatedPrice = 8.0;
            this.preferences.put("vehicle_type", "compact");
            this.preferences.put("max_wait_time", 8);
            this.preferences.put("max_detour_time", 10);
            this.preferences.put("max_passengers", 4);
        }
        
        @Override
        public void customize(Map<String, Object> parameters) {
            if (parameters.containsKey("pickup")) {
                setPickupLocation((String) parameters.get("pickup"));
            }
            if (parameters.containsKey("dropoff")) {
                setDropoffLocation((String) parameters.get("dropoff"));
            }
            if (parameters.containsKey("user_id")) {
                setUserId((String) parameters.get("user_id"));
            }
            if (parameters.containsKey("max_passengers")) {
                int maxPassengers = (Integer) parameters.get("max_passengers");
                preferences.put("max_passengers", maxPassengers);
                // Adjust price based on expected sharing
                setEstimatedPrice(estimatedPrice * (1.0 - (maxPassengers - 1) * 0.15));
            }
        }
        
        @Override
        public String getRequestType() {
            return "SharedRide";
        }
    }
    
    // Prototype manager/registry
    static class RideRequestPrototypeManager {
        private final Map<String, RideRequestPrototype> prototypes;
        
        public RideRequestPrototypeManager() {
            this.prototypes = new ConcurrentHashMap<>();
            initializePrototypes();
        }
        
        private void initializePrototypes() {
            prototypes.put("STANDARD", new StandardRideRequest());
            prototypes.put("PREMIUM", new PremiumRideRequest());
            prototypes.put("SHARED", new SharedRideRequest());
        }
        
        public RideRequestPrototype createRideRequest(String type, Map<String, Object> parameters) {
            RideRequestPrototype prototype = prototypes.get(type.toUpperCase());
            if (prototype == null) {
                throw new IllegalArgumentException("Unknown ride request type: " + type);
            }
            
            RideRequestPrototype cloned = prototype.clone();
            cloned.customize(parameters);
            return cloned;
        }
        
        public void addPrototype(String type, RideRequestPrototype prototype) {
            prototypes.put(type.toUpperCase(), prototype);
        }
        
        public Set<String> getAvailableTypes() {
            return new HashSet<>(prototypes.keySet());
        }
    }
    
    // Custom prototype - Airport ride
    static class AirportRideRequest extends BaseRideRequest {
        
        public AirportRideRequest() {
            super();
            this.rideType = "AIRPORT";
            this.estimatedPrice = 35.0;
            this.preferences.put("vehicle_type", "spacious");
            this.preferences.put("max_wait_time", 5);
            this.preferences.put("luggage_assistance", true);
            this.preferences.put("flight_tracking", true);
        }
        
        @Override
        public void customize(Map<String, Object> parameters) {
            if (parameters.containsKey("pickup")) {
                setPickupLocation((String) parameters.get("pickup"));
            }
            if (parameters.containsKey("dropoff")) {
                setDropoffLocation((String) parameters.get("dropoff"));
            }
            if (parameters.containsKey("user_id")) {
                setUserId((String) parameters.get("user_id"));
            }
            if (parameters.containsKey("flight_number")) {
                preferences.put("flight_number", parameters.get("flight_number"));
            }
            if (parameters.containsKey("scheduled_time")) {
                setScheduled(true);
                preferences.put("scheduled_time", parameters.get("scheduled_time"));
            }
        }
        
        @Override
        public String getRequestType() {
            return "AirportRide";
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Prototype Pattern Demo: Uber Ride Request Templates ===\n");
        
        RideRequestPrototypeManager manager = new RideRequestPrototypeManager();
        
        System.out.println("Available ride types: " + manager.getAvailableTypes());
        System.out.println();
        
        // Create standard rides using prototypes
        Map<String, Object> params1 = new HashMap<>();
        params1.put("pickup", "Downtown");
        params1.put("dropoff", "Airport");
        params1.put("user_id", "user123");
        params1.put("price_multiplier", 1.2);
        
        RideRequestPrototype standardRide = manager.createRideRequest("STANDARD", params1);
        System.out.println("Created: " + standardRide);
        
        // Create premium ride
        Map<String, Object> params2 = new HashMap<>();
        params2.put("pickup", "Hotel District");
        params2.put("dropoff", "Business Center");
        params2.put("user_id", "user456");
        params2.put("luxury_level", "EXECUTIVE");
        
        RideRequestPrototype premiumRide = manager.createRideRequest("PREMIUM", params2);
        System.out.println("Created: " + premiumRide);
        
        // Create shared ride
        Map<String, Object> params3 = new HashMap<>();
        params3.put("pickup", "University");
        params3.put("dropoff", "Shopping Mall");
        params3.put("user_id", "user789");
        params3.put("max_passengers", 3);
        
        RideRequestPrototype sharedRide = manager.createRideRequest("SHARED", params3);
        System.out.println("Created: " + sharedRide);
        
        // Add custom prototype
        manager.addPrototype("AIRPORT", new AirportRideRequest());
        
        Map<String, Object> params4 = new HashMap<>();
        params4.put("pickup", "Home");
        params4.put("dropoff", "LAX Airport");
        params4.put("user_id", "user999");
        params4.put("flight_number", "AA1234");
        params4.put("scheduled_time", "2025-08-15T06:00:00");
        
        RideRequestPrototype airportRide = manager.createRideRequest("AIRPORT", params4);
        System.out.println("Created: " + airportRide);
        
        // Demonstrate cloning creates separate instances
        System.out.println("\n=== Demonstrating Clone Independence ===");
        RideRequestPrototype clone1 = manager.createRideRequest("STANDARD", params1);
        RideRequestPrototype clone2 = manager.createRideRequest("STANDARD", params1);
        
        System.out.println("Clone 1: " + clone1);
        System.out.println("Clone 2: " + clone2);
        System.out.println("Are they the same instance? " + (clone1 == clone2));
        System.out.println("Do they have different IDs? " + (!clone1.toString().equals(clone2.toString())));
        
        System.out.println("\n=== Prototype Pattern Benefits ===");
        System.out.println("✓ Avoids expensive object creation");
        System.out.println("✓ Creates objects without knowing their exact class");
        System.out.println("✓ Reduces subclassing");
        System.out.println("✓ Configures applications dynamically");
        System.out.println("✓ Provides template-based object creation");
    }
}
