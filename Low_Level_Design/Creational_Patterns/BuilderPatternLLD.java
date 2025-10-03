// Builder Pattern - Low Level Design Implementation

/**
 * Builder Pattern Implementation
 * 
 * Real-world Use Case: Restaurant Order Management System
 * - Build complex meal orders with multiple options
 * - Handle optional ingredients and customizations
 * - Support different meal types (breakfast, lunch, dinner)
 * - Validate order constraints and dietary restrictions
 * - Generate order summaries and receipts
 * 
 * Key Components:
 * 1. Product - Complex object being built (Meal)
 * 2. Builder Interface - Common building steps
 * 3. Concrete Builders - Specific meal type builders
 * 4. Director - Orchestrates the building process
 * 5. Fluent Interface - Method chaining for ease of use
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// Enums for meal configuration
enum MealType {
    BREAKFAST("Breakfast", "08:00-11:00"),
    LUNCH("Lunch", "11:00-16:00"),
    DINNER("Dinner", "17:00-22:00"),
    SNACK("Snack", "All Day");
    
    private final String displayName;
    private final String availableTime;
    
    MealType(String displayName, String availableTime) {
        this.displayName = displayName;
        this.availableTime = availableTime;
    }
    
    public String getDisplayName() { return displayName; }
    public String getAvailableTime() { return availableTime; }
}

enum DietaryRestriction {
    VEGETARIAN, VEGAN, GLUTEN_FREE, NUT_FREE, DAIRY_FREE, LOW_SODIUM, KETO
}

enum SpiceLevel {
    MILD(0), MEDIUM(1), SPICY(2), VERY_SPICY(3);
    
    private final int level;
    
    SpiceLevel(int level) {
        this.level = level;
    }
    
    public int getLevel() { return level; }
}

enum ServingSize {
    SMALL(0.8), REGULAR(1.0), LARGE(1.3), EXTRA_LARGE(1.6);
    
    private final double multiplier;
    
    ServingSize(double multiplier) {
        this.multiplier = multiplier;
    }
    
    public double getMultiplier() { return multiplier; }
}

// Product classes
class MenuItem {
    private final String name;
    private final String description;
    private final BigDecimal basePrice;
    private final int prepTimeMinutes;
    private final Set<DietaryRestriction> dietaryInfo;
    private final boolean isCustomizable;
    
    public MenuItem(String name, String description, BigDecimal basePrice, 
                   int prepTimeMinutes, Set<DietaryRestriction> dietaryInfo, 
                   boolean isCustomizable) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.prepTimeMinutes = prepTimeMinutes;
        this.dietaryInfo = new HashSet<>(dietaryInfo);
        this.isCustomizable = isCustomizable;
    }
    
    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getBasePrice() { return basePrice; }
    public int getPrepTimeMinutes() { return prepTimeMinutes; }
    public Set<DietaryRestriction> getDietaryInfo() { return new HashSet<>(dietaryInfo); }
    public boolean isCustomizable() { return isCustomizable; }
}

class Customization {
    private final String name;
    private final String description;
    private final BigDecimal priceAdjustment;
    private final Set<DietaryRestriction> dietaryImpact;
    
    public Customization(String name, String description, BigDecimal priceAdjustment, 
                        Set<DietaryRestriction> dietaryImpact) {
        this.name = name;
        this.description = description;
        this.priceAdjustment = priceAdjustment;
        this.dietaryImpact = new HashSet<>(dietaryImpact);
    }
    
    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPriceAdjustment() { return priceAdjustment; }
    public Set<DietaryRestriction> getDietaryImpact() { return new HashSet<>(dietaryImpact); }
}

// Complex Product - The meal order
class Meal {
    // Required parameters
    private final String customerName;
    private final MealType mealType;
    private final LocalDateTime orderTime;
    
    // Optional parameters with defaults
    private final List<MenuItem> items;
    private final List<Customization> customizations;
    private final ServingSize servingSize;
    private final SpiceLevel spiceLevel;
    private final Set<DietaryRestriction> dietaryRestrictions;
    private final String specialInstructions;
    private final boolean isDelivery;
    private final String deliveryAddress;
    private final boolean rushOrder;
    
    // Calculated fields
    private final BigDecimal totalPrice;
    private final int estimatedPrepTime;
    private final String orderId;
    
    // Private constructor - only accessible through Builder
    private Meal(Builder builder) {
        // Required fields
        this.customerName = builder.customerName;
        this.mealType = builder.mealType;
        this.orderTime = builder.orderTime;
        
        // Optional fields
        this.items = new ArrayList<>(builder.items);
        this.customizations = new ArrayList<>(builder.customizations);
        this.servingSize = builder.servingSize;
        this.spiceLevel = builder.spiceLevel;
        this.dietaryRestrictions = new HashSet<>(builder.dietaryRestrictions);
        this.specialInstructions = builder.specialInstructions;
        this.isDelivery = builder.isDelivery;
        this.deliveryAddress = builder.deliveryAddress;
        this.rushOrder = builder.rushOrder;
        
        // Calculate derived values
        this.totalPrice = calculateTotalPrice();
        this.estimatedPrepTime = calculatePrepTime();
        this.orderId = generateOrderId();
    }
    
    private BigDecimal calculateTotalPrice() {
        BigDecimal baseTotal = items.stream()
            .map(MenuItem::getBasePrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal customizationCost = customizations.stream()
            .map(Customization::getPriceAdjustment)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal subtotal = baseTotal.add(customizationCost);
        
        // Apply serving size multiplier
        subtotal = subtotal.multiply(BigDecimal.valueOf(servingSize.getMultiplier()));
        
        // Rush order surcharge
        if (rushOrder) {
            subtotal = subtotal.multiply(BigDecimal.valueOf(1.25)); // 25% surcharge
        }
        
        // Delivery fee
        if (isDelivery) {
            subtotal = subtotal.add(BigDecimal.valueOf(3.99));
        }
        
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }
    
    private int calculatePrepTime() {
        int baseTime = items.stream()
            .mapToInt(MenuItem::getPrepTimeMinutes)
            .max()
            .orElse(0);
        
        // Add customization time
        int customizationTime = customizations.size() * 3; // 3 min per customization
        
        int total = baseTime + customizationTime;
        
        // Rush order reduces time by 25%
        if (rushOrder) {
            total = (int) (total * 0.75);
        }
        
        // Delivery adds travel time
        if (isDelivery) {
            total += 20; // Average delivery time
        }
        
        return total;
    }
    
    private String generateOrderId() {
        String timeStamp = orderTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String customerCode = customerName.replaceAll("[^A-Za-z]", "").substring(0, 
            Math.min(3, customerName.length())).toUpperCase();
        return mealType.name().substring(0, 1) + timeStamp.substring(8) + customerCode;
    }
    
    // Getters
    public String getCustomerName() { return customerName; }
    public MealType getMealType() { return mealType; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public List<MenuItem> getItems() { return new ArrayList<>(items); }
    public List<Customization> getCustomizations() { return new ArrayList<>(customizations); }
    public ServingSize getServingSize() { return servingSize; }
    public SpiceLevel getSpiceLevel() { return spiceLevel; }
    public Set<DietaryRestriction> getDietaryRestrictions() { return new HashSet<>(dietaryRestrictions); }
    public String getSpecialInstructions() { return specialInstructions; }
    public boolean isDelivery() { return isDelivery; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public boolean isRushOrder() { return rushOrder; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public int getEstimatedPrepTime() { return estimatedPrepTime; }
    public String getOrderId() { return orderId; }
    
    // Business methods
    public boolean isCompatibleWithDiet(DietaryRestriction restriction) {
        // Check if all items and customizations are compatible
        boolean itemsCompatible = items.stream()
            .allMatch(item -> item.getDietaryInfo().contains(restriction));
        
        boolean customizationsCompatible = customizations.stream()
            .noneMatch(custom -> custom.getDietaryImpact().contains(restriction) && 
                      custom.getPriceAdjustment().compareTo(BigDecimal.ZERO) > 0);
        
        return itemsCompatible && customizationsCompatible;
    }
    
    public LocalDateTime getEstimatedReadyTime() {
        return orderTime.plusMinutes(estimatedPrepTime);
    }
    
    public String generateReceipt() {
        StringBuilder receipt = new StringBuilder();
        receipt.append("\\n" + "=".repeat(40) + "\\n");
        receipt.append("         RESTAURANT ORDER RECEIPT\\n");
        receipt.append("=".repeat(40) + "\\n");
        receipt.append("Order ID: ").append(orderId).append("\\n");
        receipt.append("Customer: ").append(customerName).append("\\n");
        receipt.append("Order Time: ").append(orderTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\\n");
        receipt.append("Meal Type: ").append(mealType.getDisplayName()).append("\\n");
        receipt.append("-".repeat(40) + "\\n");
        
        // Items
        receipt.append("ITEMS:\\n");
        items.forEach(item -> {
            receipt.append("• ").append(item.getName()).append(" - $").append(item.getBasePrice()).append("\\n");
        });
        
        // Customizations
        if (!customizations.isEmpty()) {
            receipt.append("\\nCUSTOMIZATIONS:\\n");
            customizations.forEach(custom -> {
                receipt.append("• ").append(custom.getName()).append(" - $").append(custom.getPriceAdjustment()).append("\\n");
            });
        }
        
        receipt.append("-".repeat(40) + "\\n");
        receipt.append("Serving Size: ").append(servingSize).append("\\n");
        receipt.append("Spice Level: ").append(spiceLevel).append("\\n");
        
        if (!dietaryRestrictions.isEmpty()) {
            receipt.append("Dietary: ").append(
                dietaryRestrictions.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", "))
            ).append("\\n");
        }
        
        if (specialInstructions != null && !specialInstructions.trim().isEmpty()) {
            receipt.append("Special Instructions: ").append(specialInstructions).append("\\n");
        }
        
        receipt.append("-".repeat(40) + "\\n");
        receipt.append("Subtotal: $").append(totalPrice).append("\\n");
        
        if (rushOrder) {
            receipt.append("Rush Order Surcharge: 25%\\n");
        }
        
        if (isDelivery) {
            receipt.append("Delivery Fee: $3.99\\n");
            receipt.append("Delivery Address: ").append(deliveryAddress).append("\\n");
        }
        
        receipt.append("TOTAL: $").append(totalPrice).append("\\n");
        receipt.append("Estimated Ready Time: ").append(getEstimatedReadyTime().format(DateTimeFormatter.ofPattern("HH:mm"))).append("\\n");
        receipt.append("=".repeat(40) + "\\n");
        
        return receipt.toString();
    }
    
    // Static nested Builder class
    public static class Builder {
        // Required parameters
        private String customerName;
        private MealType mealType;
        private LocalDateTime orderTime;
        
        // Optional parameters with defaults
        private List<MenuItem> items = new ArrayList<>();
        private List<Customization> customizations = new ArrayList<>();
        private ServingSize servingSize = ServingSize.REGULAR;
        private SpiceLevel spiceLevel = SpiceLevel.MILD;
        private Set<DietaryRestriction> dietaryRestrictions = new HashSet<>();
        private String specialInstructions = "";
        private boolean isDelivery = false;
        private String deliveryAddress = "";
        private boolean rushOrder = false;
        
        // Constructor with required parameters
        public Builder(String customerName, MealType mealType) {
            this.customerName = customerName;
            this.mealType = mealType;
            this.orderTime = LocalDateTime.now();
        }
        
        // Fluent interface methods
        public Builder addItem(MenuItem item) {
            if (item != null) {
                this.items.add(item);
            }
            return this;
        }
        
        public Builder addItems(MenuItem... items) {
            Arrays.stream(items)
                  .filter(Objects::nonNull)
                  .forEach(this.items::add);
            return this;
        }
        
        public Builder addCustomization(Customization customization) {
            if (customization != null) {
                this.customizations.add(customization);
            }
            return this;
        }
        
        public Builder servingSize(ServingSize size) {
            this.servingSize = size;
            return this;
        }
        
        public Builder spiceLevel(SpiceLevel level) {
            this.spiceLevel = level;
            return this;
        }
        
        public Builder addDietaryRestriction(DietaryRestriction restriction) {
            this.dietaryRestrictions.add(restriction);
            return this;
        }
        
        public Builder addDietaryRestrictions(DietaryRestriction... restrictions) {
            Arrays.stream(restrictions).forEach(this.dietaryRestrictions::add);
            return this;
        }
        
        public Builder specialInstructions(String instructions) {
            this.specialInstructions = instructions != null ? instructions : "";
            return this;
        }
        
        public Builder delivery(String address) {
            this.isDelivery = true;
            this.deliveryAddress = address;
            return this;
        }
        
        public Builder rushOrder() {
            this.rushOrder = true;
            return this;
        }
        
        public Builder orderTime(LocalDateTime time) {
            this.orderTime = time;
            return this;
        }
        
        // Build method with validation
        public Meal build() {
            validate();
            return new Meal(this);
        }
        
        private void validate() {
            if (customerName == null || customerName.trim().isEmpty()) {
                throw new IllegalStateException("Customer name is required");
            }
            
            if (items.isEmpty()) {
                throw new IllegalStateException("At least one menu item is required");
            }
            
            if (isDelivery && (deliveryAddress == null || deliveryAddress.trim().isEmpty())) {
                throw new IllegalStateException("Delivery address is required for delivery orders");
            }
            
            // Validate dietary restrictions compatibility
            for (DietaryRestriction restriction : dietaryRestrictions) {
                boolean hasCompatibleItem = items.stream()
                    .anyMatch(item -> item.getDietaryInfo().contains(restriction));
                
                if (!hasCompatibleItem) {
                    throw new IllegalStateException(
                        "No items compatible with dietary restriction: " + restriction);
                }
            }
        }
    }
}

// Director classes for common meal patterns
class MealDirector {
    
    public static Meal createBreakfastCombo(String customerName) {
        MenuItem pancakes = new MenuItem("Buttermilk Pancakes", "Fluffy pancakes with syrup", 
            new BigDecimal("8.99"), 12, Set.of(DietaryRestriction.VEGETARIAN), true);
        MenuItem coffee = new MenuItem("Coffee", "Fresh brewed coffee", 
            new BigDecimal("2.49"), 3, Set.of(), false);
        MenuItem bacon = new MenuItem("Bacon", "Crispy bacon strips", 
            new BigDecimal("3.99"), 8, Set.of(), false);
        
        return new Meal.Builder(customerName, MealType.BREAKFAST)
            .addItems(pancakes, coffee, bacon)
            .addCustomization(new Customization("Extra Syrup", "Additional maple syrup", 
                new BigDecimal("0.50"), Set.of()))
            .spiceLevel(SpiceLevel.MILD)
            .build();
    }
    
    public static Meal createHealthyLunch(String customerName) {
        MenuItem salad = new MenuItem("Quinoa Salad", "Mixed greens with quinoa and vegetables", 
            new BigDecimal("12.99"), 8, Set.of(DietaryRestriction.VEGETARIAN, DietaryRestriction.VEGAN), true);
        MenuItem water = new MenuItem("Sparkling Water", "Lemon sparkling water", 
            new BigDecimal("2.99"), 1, Set.of(), false);
        
        return new Meal.Builder(customerName, MealType.LUNCH)
            .addItems(salad, water)
            .addDietaryRestrictions(DietaryRestriction.VEGETARIAN, DietaryRestriction.GLUTEN_FREE)
            .servingSize(ServingSize.REGULAR)
            .specialInstructions("Light dressing on the side")
            .build();
    }
    
    public static Meal createFamilyDinner(String customerName, String deliveryAddress) {
        MenuItem pizza = new MenuItem("Family Pizza", "Large pizza with multiple toppings", 
            new BigDecimal("18.99"), 25, Set.of(), true);
        MenuItem wings = new MenuItem("Buffalo Wings", "Spicy chicken wings", 
            new BigDecimal("12.99"), 20, Set.of(), true);
        MenuItem soda = new MenuItem("Soda", "2L bottle of soda", 
            new BigDecimal("3.99"), 1, Set.of(), false);
        
        return new Meal.Builder(customerName, MealType.DINNER)
            .addItems(pizza, wings, soda)
            .addCustomization(new Customization("Extra Cheese", "Additional cheese on pizza", 
                new BigDecimal("2.00"), Set.of()))
            .servingSize(ServingSize.LARGE)
            .spiceLevel(SpiceLevel.MEDIUM)
            .delivery(deliveryAddress)
            .specialInstructions("Ring doorbell twice")
            .build();
    }
    
    public static Meal createVeganMeal(String customerName) {
        MenuItem burger = new MenuItem("Vegan Burger", "Plant-based burger with vegetables", 
            new BigDecimal("13.99"), 15, Set.of(DietaryRestriction.VEGAN, DietaryRestriction.VEGETARIAN), true);
        MenuItem fries = new MenuItem("Sweet Potato Fries", "Baked sweet potato fries", 
            new BigDecimal("4.99"), 18, Set.of(DietaryRestriction.VEGAN, DietaryRestriction.VEGETARIAN), true);
        
        return new Meal.Builder(customerName, MealType.LUNCH)
            .addItems(burger, fries)
            .addDietaryRestrictions(DietaryRestriction.VEGAN, DietaryRestriction.DAIRY_FREE)
            .addCustomization(new Customization("Avocado", "Fresh avocado slices", 
                new BigDecimal("1.50"), Set.of(DietaryRestriction.VEGAN)))
            .spiceLevel(SpiceLevel.MILD)
            .build();
    }
}

// Restaurant Order Management System
class RestaurantOrderSystem {
    private List<Meal> orders;
    private Map<String, MenuItem> menu;
    private Map<String, Customization> customizations;
    
    public RestaurantOrderSystem() {
        this.orders = new ArrayList<>();
        this.menu = new HashMap<>();
        this.customizations = new HashMap<>();
        initializeMenu();
    }
    
    private void initializeMenu() {
        // Breakfast items
        menu.put("pancakes", new MenuItem("Buttermilk Pancakes", "Fluffy pancakes with syrup", 
            new BigDecimal("8.99"), 12, Set.of(DietaryRestriction.VEGETARIAN), true));
        menu.put("eggs", new MenuItem("Scrambled Eggs", "Fresh farm eggs", 
            new BigDecimal("6.99"), 8, Set.of(DietaryRestriction.VEGETARIAN), true));
        
        // Lunch items
        menu.put("burger", new MenuItem("Classic Burger", "Beef burger with lettuce and tomato", 
            new BigDecimal("11.99"), 15, Set.of(), true));
        menu.put("salad", new MenuItem("Caesar Salad", "Romaine lettuce with Caesar dressing", 
            new BigDecimal("9.99"), 8, Set.of(DietaryRestriction.VEGETARIAN), true));
        
        // Customizations
        customizations.put("cheese", new Customization("Extra Cheese", "Additional cheese", 
            new BigDecimal("1.50"), Set.of()));
        customizations.put("bacon", new Customization("Bacon", "Crispy bacon strips", 
            new BigDecimal("2.50"), Set.of()));
        customizations.put("avocado", new Customization("Avocado", "Fresh avocado slices", 
            new BigDecimal("2.00"), Set.of(DietaryRestriction.VEGAN)));
    }
    
    public MenuItem getMenuItem(String key) {
        return menu.get(key);
    }
    
    public Customization getCustomization(String key) {
        return customizations.get(key);
    }
    
    public void placeOrder(Meal meal) {
        orders.add(meal);
        System.out.println("✅ Order placed successfully!");
        System.out.println("Order ID: " + meal.getOrderId());
        System.out.println("Estimated ready time: " + 
            meal.getEstimatedReadyTime().format(DateTimeFormatter.ofPattern("HH:mm")));
    }
    
    public void printOrderSummary() {
        System.out.println("\\n📊 ORDER SUMMARY");
        System.out.println("=================");
        System.out.println("Total Orders: " + orders.size());
        
        BigDecimal totalRevenue = orders.stream()
            .map(Meal::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        System.out.println("Total Revenue: $" + totalRevenue);
        
        Map<MealType, Long> ordersByType = orders.stream()
            .collect(Collectors.groupingBy(Meal::getMealType, Collectors.counting()));
        
        ordersByType.forEach((type, count) -> 
            System.out.println(type.getDisplayName() + " orders: " + count));
    }
    
    public List<Meal> getOrders() {
        return new ArrayList<>(orders);
    }
}

// Main demonstration class
public class BuilderPatternLLD {
    public static void main(String[] args) {
        System.out.println("🍽️  RESTAURANT ORDER MANAGEMENT SYSTEM - Builder Pattern Demo");
        System.out.println("=============================================================\\n");
        
        RestaurantOrderSystem restaurant = new RestaurantOrderSystem();
        
        System.out.println("📋 TESTING DIRECTOR PATTERNS");
        System.out.println("============================");
        
        // Test predefined meal patterns using Director
        System.out.println("\\n--- Creating Breakfast Combo ---");
        Meal breakfast = MealDirector.createBreakfastCombo("Alice Johnson");
        restaurant.placeOrder(breakfast);
        System.out.println(breakfast.generateReceipt());
        
        System.out.println("\\n--- Creating Healthy Lunch ---");
        Meal healthyLunch = MealDirector.createHealthyLunch("Bob Smith");
        restaurant.placeOrder(healthyLunch);
        System.out.println(healthyLunch.generateReceipt());
        
        System.out.println("\\n--- Creating Family Dinner ---");
        Meal familyDinner = MealDirector.createFamilyDinner("Carol Williams", "123 Main St, Anytown");
        restaurant.placeOrder(familyDinner);
        System.out.println(familyDinner.generateReceipt());
        
        System.out.println("\\n🔧 TESTING CUSTOM BUILDER");
        System.out.println("==========================");
        
        // Test custom meal building with fluent interface
        System.out.println("\\n--- Creating Custom Order ---");
        
        try {
            Meal customMeal = new Meal.Builder("David Chen", MealType.LUNCH)
                .addItem(restaurant.getMenuItem("burger"))
                .addItem(restaurant.getMenuItem("salad"))
                .addCustomization(restaurant.getCustomization("cheese"))
                .addCustomization(restaurant.getCustomization("avocado"))
                .servingSize(ServingSize.LARGE)
                .spiceLevel(SpiceLevel.MEDIUM)
                .addDietaryRestriction(DietaryRestriction.GLUTEN_FREE)
                .specialInstructions("No onions please, extra pickles")
                .rushOrder()
                .build();
            
            restaurant.placeOrder(customMeal);
            System.out.println(customMeal.generateReceipt());
            
        } catch (Exception e) {
            System.err.println("❌ Error creating custom meal: " + e.getMessage());
        }
        
        System.out.println("\\n🚚 TESTING DELIVERY ORDER");
        System.out.println("=========================");
        
        // Test delivery order
        System.out.println("\\n--- Creating Delivery Order ---");
        Meal deliveryOrder = new Meal.Builder("Emma Davis", MealType.DINNER)
            .addItem(restaurant.getMenuItem("burger"))
            .addCustomization(restaurant.getCustomization("bacon"))
            .servingSize(ServingSize.REGULAR)
            .spiceLevel(SpiceLevel.MILD)
            .delivery("456 Oak Ave, Downtown")
            .specialInstructions("Call when arriving, apartment 3B")
            .build();
        
        restaurant.placeOrder(deliveryOrder);
        System.out.println(deliveryOrder.generateReceipt());
        
        System.out.println("\\n🌱 TESTING VEGAN MEAL");
        System.out.println("=====================");
        
        System.out.println("\\n--- Creating Vegan Meal ---");
        Meal veganMeal = MealDirector.createVeganMeal("Frank Green");
        restaurant.placeOrder(veganMeal);
        System.out.println(veganMeal.generateReceipt());
        
        System.out.println("\\n❌ TESTING VALIDATION");
        System.out.println("====================");
        
        // Test validation errors
        System.out.println("\\n--- Testing Invalid Orders ---");
        
        try {
            // Empty customer name
            new Meal.Builder("", MealType.BREAKFAST).build();
        } catch (IllegalStateException e) {
            System.out.println("✅ Caught expected error: " + e.getMessage());
        }
        
        try {
            // No items
            new Meal.Builder("Test Customer", MealType.LUNCH).build();
        } catch (IllegalStateException e) {
            System.out.println("✅ Caught expected error: " + e.getMessage());
        }
        
        try {
            // Delivery without address
            new Meal.Builder("Test Customer", MealType.DINNER)
                .addItem(restaurant.getMenuItem("burger"))
                .delivery("")
                .build();
        } catch (IllegalStateException e) {
            System.out.println("✅ Caught expected error: " + e.getMessage());
        }
        
        // Final summary
        restaurant.printOrderSummary();
        
        System.out.println("\\n✅ Builder Pattern Demo Complete!");
        
        System.out.println("\\n📚 KEY CONCEPTS DEMONSTRATED:");
        System.out.println("• Complex Object Construction - Multi-parameter meal orders");
        System.out.println("• Fluent Interface - Method chaining for ease of use");
        System.out.println("• Optional Parameters - Flexible meal customization");
        System.out.println("• Immutable Products - Meal objects cannot be modified after creation");
        System.out.println("• Validation - Business rules enforced during build");
        System.out.println("• Director Pattern - Predefined meal configurations");
        System.out.println("• Calculated Fields - Auto-computed price and prep time");
        System.out.println("• Builder Reusability - Same builder for different meal types");
    }
}
