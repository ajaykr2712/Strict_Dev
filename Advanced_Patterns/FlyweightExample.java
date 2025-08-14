package com.systemdesign.patterns;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flyweight Pattern Implementation for Uber
 * 
 * The Flyweight pattern minimizes memory usage by sharing efficiently
 * among similar objects. Perfect for Uber's driver and location management
 * where thousands of objects need to be managed efficiently.
 * 
 * Scenario: Managing driver icons on Uber's map interface where multiple
 * drivers share the same visual representation but have different locations.
 */

// Flyweight interface
interface DriverIcon {
    void render(int x, int y, String driverId, String status);
}

// Concrete flyweight - intrinsic state (shared)
class ConcreteDriverIcon implements DriverIcon {
    private final String iconType;  // Car, bike, truck
    private final String color;     // Vehicle color category
    private final byte[] iconData;  // Actual icon image data
    
    public ConcreteDriverIcon(String iconType, String color) {
        this.iconType = iconType;
        this.color = color;
        this.iconData = loadIconData(iconType, color);
        System.out.println("Created flyweight for: " + iconType + " (" + color + ")");
    }
    
    @Override
    public void render(int x, int y, String driverId, String status) {
        // Extrinsic state (position, driver ID, status) passed as parameters
        System.out.println("Rendering " + iconType + " (" + color + ") " +
                         "at (" + x + "," + y + ") " +
                         "for driver " + driverId + " - " + status);
    }
    
    private byte[] loadIconData(String type, String color) {
        // Simulate loading icon data (would be actual image in real app)
        return new byte[1024]; // 1KB icon data
    }
    
    // Getters for intrinsic state
    public String getIconType() { return iconType; }
    public String getColor() { return color; }
}

// Flyweight factory
class DriverIconFactory {
    private static final Map<String, DriverIcon> iconPool = new ConcurrentHashMap<>();
    
    public static DriverIcon getDriverIcon(String vehicleType, String color) {
        String key = vehicleType + "_" + color;
        
        return iconPool.computeIfAbsent(key, k -> {
            return new ConcreteDriverIcon(vehicleType, color);
        });
    }
    
    public static int getCreatedIconsCount() {
        return iconPool.size();
    }
    
    public static void printIconStats() {
        System.out.println("\n=== Icon Pool Statistics ===");
        System.out.println("Total flyweight instances created: " + iconPool.size());
        iconPool.forEach((key, icon) -> {
            ConcreteDriverIcon concrete = (ConcreteDriverIcon) icon;
            System.out.println("- " + key + ": " + concrete.getIconType() + 
                             " (" + concrete.getColor() + ")");
        });
    }
}

// Context class - holds extrinsic state
class Driver {
    private final String driverId;
    private int x, y;  // Current position (extrinsic state)
    private String status;  // Online, busy, offline (extrinsic state)
    private final DriverIcon icon;  // Reference to flyweight (intrinsic state)
    
    public Driver(String driverId, String vehicleType, String color, 
                  int x, int y, String status) {
        this.driverId = driverId;
        this.x = x;
        this.y = y;
        this.status = status;
        this.icon = DriverIconFactory.getDriverIcon(vehicleType, color);
    }
    
    public void updatePosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }
    
    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }
    
    public void render() {
        icon.render(x, y, driverId, status);
    }
    
    // Getters
    public String getDriverId() { return driverId; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getStatus() { return status; }
}

// Uber map simulation
class UberMapSystem {
    private final List<Driver> activeDrivers;
    private final Random random;
    
    public UberMapSystem() {
        this.activeDrivers = new ArrayList<>();
        this.random = new Random();
    }
    
    public void addDriver(String driverId, String vehicleType, String color) {
        int x = random.nextInt(1000);
        int y = random.nextInt(1000);
        String status = random.nextBoolean() ? "available" : "busy";
        
        Driver driver = new Driver(driverId, vehicleType, color, x, y, status);
        activeDrivers.add(driver);
    }
    
    public void simulateDriverMovement() {
        activeDrivers.forEach(driver -> {
            int newX = Math.max(0, Math.min(999, driver.getX() + random.nextInt(21) - 10));
            int newY = Math.max(0, Math.min(999, driver.getY() + random.nextInt(21) - 10));
            driver.updatePosition(newX, newY);
            
            // Randomly change status
            if (random.nextDouble() < 0.1) {
                String newStatus = random.nextBoolean() ? "available" : "busy";
                driver.updateStatus(newStatus);
            }
        });
    }
    
    public void renderMap() {
        System.out.println("\n=== Rendering Uber Map ===");
        activeDrivers.forEach(Driver::render);
    }
    
    public void getSystemStats() {
        System.out.println("\n=== System Statistics ===");
        System.out.println("Total drivers: " + activeDrivers.size());
        
        Map<String, Long> statusCount = new HashMap<>();
        activeDrivers.forEach(driver -> {
            statusCount.merge(driver.getStatus(), 1L, Long::sum);
        });
        
        statusCount.forEach((status, count) -> {
            System.out.println("- " + status + ": " + count + " drivers");
        });
    }
}

public class FlyweightExample {
    public static void main(String[] args) {
        System.out.println("=== Uber Flyweight Pattern Demo ===\n");
        
        UberMapSystem mapSystem = new UberMapSystem();
        
        // Add many drivers with various vehicle types
        String[] vehicleTypes = {"car", "bike", "truck", "suv"};
        String[] colors = {"blue", "red", "white", "black", "silver"};
        
        // Simulate 1000 drivers
        for (int i = 0; i < 1000; i++) {
            String vehicleType = vehicleTypes[i % vehicleTypes.length];
            String color = colors[i % colors.length];
            mapSystem.addDriver("DRIVER_" + (i + 1), vehicleType, color);
        }
        
        // Show how flyweight pattern saves memory
        DriverIconFactory.printIconStats();
        
        // Simulate some driver activity
        mapSystem.getSystemStats();
        
        // Show a sample of drivers on the map
        System.out.println("\n=== Sample Driver Positions ===");
        for (int i = 0; i < 5; i++) {
            mapSystem.simulateDriverMovement();
        }
        
        // Render a small sample
        UberMapSystem sampleSystem = new UberMapSystem();
        for (int i = 0; i < 10; i++) {
            String vehicleType = vehicleTypes[i % vehicleTypes.length];
            String color = colors[i % colors.length];
            sampleSystem.addDriver("SAMPLE_" + (i + 1), vehicleType, color);
        }
        sampleSystem.renderMap();
        
        demonstrateMemoryEfficiency();
    }
    
    private static void demonstrateMemoryEfficiency() {
        System.out.println("\n=== Memory Efficiency Demonstration ===");
        
        // Without flyweight: Each driver would have its own icon data
        int driversCount = 1000;
        int iconDataSize = 1024; // bytes per icon
        int withoutFlyweight = driversCount * iconDataSize;
        
        // With flyweight: Shared icon data
        int flyweightInstances = DriverIconFactory.getCreatedIconsCount();
        int withFlyweight = flyweightInstances * iconDataSize;
        
        System.out.println("Memory usage without Flyweight: " + 
                         (withoutFlyweight / 1024) + " KB");
        System.out.println("Memory usage with Flyweight: " + 
                         (withFlyweight / 1024) + " KB");
        System.out.println("Memory saved: " + 
                         ((withoutFlyweight - withFlyweight) / 1024) + " KB");
        System.out.println("Efficiency: " + 
                         String.format("%.1f%%", 
                         (100.0 * (withoutFlyweight - withFlyweight) / withoutFlyweight)));
    }
}
