/**
 * Builder Pattern Implementation
 * 
 * The Builder pattern constructs complex objects step by step.
 * It allows you to produce different types and representations of an object
 * using the same construction code.
 * 
 * Real-world examples:
 * - SQL Query builders
 * - Configuration builders
 * - HTTP Request builders
 * - Document builders (PDF, HTML)
 */

import java.util.*;
import java.time.LocalDateTime;

// Product class - Complex object to be built
class Computer {
    // Required parameters
    private final String processor;
    private final String memory;
    
    // Optional parameters
    private final String storage;
    private final String graphicsCard;
    private final String motherboard;
    private final String powerSupply;
    private final boolean hasWifi;
    private final boolean hasBluetooth;
    private final List<String> additionalComponents;
    private final double price;
    
    // Private constructor - only accessible through Builder
    private Computer(ComputerBuilder builder) {
        this.processor = builder.processor;
        this.memory = builder.memory;
        this.storage = builder.storage;
        this.graphicsCard = builder.graphicsCard;
        this.motherboard = builder.motherboard;
        this.powerSupply = builder.powerSupply;
        this.hasWifi = builder.hasWifi;
        this.hasBluetooth = builder.hasBluetooth;
        this.additionalComponents = new ArrayList<>(builder.additionalComponents);
        this.price = builder.price;
    }
    
    // Getters
    public String getProcessor() { return processor; }
    public String getMemory() { return memory; }
    public String getStorage() { return storage; }
    public String getGraphicsCard() { return graphicsCard; }
    public String getMotherboard() { return motherboard; }
    public String getPowerSupply() { return powerSupply; }
    public boolean hasWifi() { return hasWifi; }
    public boolean hasBluetooth() { return hasBluetooth; }
    public List<String> getAdditionalComponents() { return new ArrayList<>(additionalComponents); }
    public double getPrice() { return price; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Computer Configuration:\n");
        sb.append("  Processor: ").append(processor).append("\n");
        sb.append("  Memory: ").append(memory).append("\n");
        sb.append("  Storage: ").append(storage != null ? storage : "Not specified").append("\n");
        sb.append("  Graphics Card: ").append(graphicsCard != null ? graphicsCard : "Integrated").append("\n");
        sb.append("  Motherboard: ").append(motherboard != null ? motherboard : "Standard").append("\n");
        sb.append("  Power Supply: ").append(powerSupply != null ? powerSupply : "Standard").append("\n");
        sb.append("  WiFi: ").append(hasWifi ? "Yes" : "No").append("\n");
        sb.append("  Bluetooth: ").append(hasBluetooth ? "Yes" : "No").append("\n");
        if (!additionalComponents.isEmpty()) {
            sb.append("  Additional Components: ").append(String.join(", ", additionalComponents)).append("\n");
        }
        sb.append("  Total Price: $").append(String.format("%.2f", price));
        return sb.toString();
    }
    
    // Builder class
    public static class ComputerBuilder {
        // Required parameters
        private final String processor;
        private final String memory;
        
        // Optional parameters - initialized to default values
        private String storage;
        private String graphicsCard;
        private String motherboard;
        private String powerSupply;
        private boolean hasWifi = false;
        private boolean hasBluetooth = false;
        private List<String> additionalComponents = new ArrayList<>();
        private double price = 0.0;
        
        // Constructor with required parameters
        public ComputerBuilder(String processor, String memory) {
            this.processor = processor;
            this.memory = memory;
        }
        
        // Methods for optional parameters - return Builder for chaining
        public ComputerBuilder storage(String storage) {
            this.storage = storage;
            return this;
        }
        
        public ComputerBuilder graphicsCard(String graphicsCard) {
            this.graphicsCard = graphicsCard;
            return this;
        }
        
        public ComputerBuilder motherboard(String motherboard) {
            this.motherboard = motherboard;
            return this;
        }
        
        public ComputerBuilder powerSupply(String powerSupply) {
            this.powerSupply = powerSupply;
            return this;
        }
        
        public ComputerBuilder enableWifi() {
            this.hasWifi = true;
            return this;
        }
        
        public ComputerBuilder enableBluetooth() {
            this.hasBluetooth = true;
            return this;
        }
        
        public ComputerBuilder addComponent(String component) {
            this.additionalComponents.add(component);
            return this;
        }
        
        public ComputerBuilder price(double price) {
            this.price = price;
            return this;
        }
        
        // Build method
        public Computer build() {
            // Validation logic can be added here
            if (processor == null || processor.trim().isEmpty()) {
                throw new IllegalArgumentException("Processor is required");
            }
            if (memory == null || memory.trim().isEmpty()) {
                throw new IllegalArgumentException("Memory is required");
            }
            
            return new Computer(this);
        }
    }
}

// SQL Query Builder Example
class SQLQueryBuilder {
    private StringBuilder query = new StringBuilder();
    private String tableName;
    private List<String> selectColumns = new ArrayList<>();
    private List<String> whereConditions = new ArrayList<>();
    private List<String> joinClauses = new ArrayList<>();
    private String orderBy;
    private String groupBy;
    private Integer limit;
    
    public SQLQueryBuilder select(String... columns) {
        selectColumns.addAll(Arrays.asList(columns));
        return this;
    }
    
    public SQLQueryBuilder from(String table) {
        this.tableName = table;
        return this;
    }
    
    public SQLQueryBuilder where(String condition) {
        whereConditions.add(condition);
        return this;
    }
    
    public SQLQueryBuilder join(String joinClause) {
        joinClauses.add(joinClause);
        return this;
    }
    
    public SQLQueryBuilder orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }
    
    public SQLQueryBuilder groupBy(String groupBy) {
        this.groupBy = groupBy;
        return this;
    }
    
    public SQLQueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }
    
    public String build() {
        if (tableName == null) {
            throw new IllegalStateException("Table name is required");
        }
        
        query = new StringBuilder();
        
        // SELECT clause
        query.append("SELECT ");
        if (selectColumns.isEmpty()) {
            query.append("*");
        } else {
            query.append(String.join(", ", selectColumns));
        }
        
        // FROM clause
        query.append(" FROM ").append(tableName);
        
        // JOIN clauses
        for (String join : joinClauses) {
            query.append(" ").append(join);
        }
        
        // WHERE clause
        if (!whereConditions.isEmpty()) {
            query.append(" WHERE ").append(String.join(" AND ", whereConditions));
        }
        
        // GROUP BY clause
        if (groupBy != null) {
            query.append(" GROUP BY ").append(groupBy);
        }
        
        // ORDER BY clause
        if (orderBy != null) {
            query.append(" ORDER BY ").append(orderBy);
        }
        
        // LIMIT clause
        if (limit != null) {
            query.append(" LIMIT ").append(limit);
        }
        
        return query.toString();
    }
    
    public void reset() {
        query = new StringBuilder();
        tableName = null;
        selectColumns.clear();
        whereConditions.clear();
        joinClauses.clear();
        orderBy = null;
        groupBy = null;
        limit = null;
    }
}

// HTTP Request Builder Example
class HttpRequestBuilder {
    private String method = "GET";
    private String url;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> queryParams = new HashMap<>();
    private String body;
    private int timeout = 30000; // 30 seconds default
    
    public HttpRequestBuilder method(String method) {
        this.method = method.toUpperCase();
        return this;
    }
    
    public HttpRequestBuilder url(String url) {
        this.url = url;
        return this;
    }
    
    public HttpRequestBuilder header(String key, String value) {
        headers.put(key, value);
        return this;
    }
    
    public HttpRequestBuilder queryParam(String key, String value) {
        queryParams.put(key, value);
        return this;
    }
    
    public HttpRequestBuilder body(String body) {
        this.body = body;
        return this;
    }
    
    public HttpRequestBuilder timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    
    public HttpRequest build() {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL is required");
        }
        
        return new HttpRequest(method, url, headers, queryParams, body, timeout);
    }
}

// HTTP Request class
class HttpRequest {
    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final String body;
    private final int timeout;
    
    public HttpRequest(String method, String url, Map<String, String> headers,
                      Map<String, String> queryParams, String body, int timeout) {
        this.method = method;
        this.url = url;
        this.headers = new HashMap<>(headers);
        this.queryParams = new HashMap<>(queryParams);
        this.body = body;
        this.timeout = timeout;
    }
    
    public String execute() {
        StringBuilder result = new StringBuilder();
        result.append("Executing HTTP Request:\n");
        result.append("Method: ").append(method).append("\n");
        result.append("URL: ").append(buildFullUrl()).append("\n");
        
        if (!headers.isEmpty()) {
            result.append("Headers:\n");
            headers.forEach((k, v) -> result.append("  ").append(k).append(": ").append(v).append("\n"));
        }
        
        if (body != null) {
            result.append("Body: ").append(body).append("\n");
        }
        
        result.append("Timeout: ").append(timeout).append("ms\n");
        result.append("Status: Request executed successfully");
        
        return result.toString();
    }
    
    private String buildFullUrl() {
        if (queryParams.isEmpty()) {
            return url;
        }
        
        StringBuilder fullUrl = new StringBuilder(url);
        fullUrl.append("?");
        
        queryParams.forEach((key, value) -> {
            fullUrl.append(key).append("=").append(value).append("&");
        });
        
        // Remove the last '&'
        if (fullUrl.charAt(fullUrl.length() - 1) == '&') {
            fullUrl.setLength(fullUrl.length() - 1);
        }
        
        return fullUrl.toString();
    }
}

// Director class for predefined computer configurations
class ComputerDirector {
    public static Computer buildGamingComputer() {
        return new Computer.ComputerBuilder("Intel i9-13900K", "32GB DDR5")
                .storage("2TB NVMe SSD")
                .graphicsCard("NVIDIA RTX 4080")
                .motherboard("ASUS ROG Strix Z790")
                .powerSupply("850W 80+ Gold")
                .enableWifi()
                .enableBluetooth()
                .addComponent("RGB Lighting Kit")
                .addComponent("Liquid Cooling System")
                .price(3500.00)
                .build();
    }
    
    public static Computer buildOfficeComputer() {
        return new Computer.ComputerBuilder("Intel i5-13600", "16GB DDR4")
                .storage("512GB SSD")
                .motherboard("ASUS Prime B660")
                .powerSupply("500W 80+ Bronze")
                .enableWifi()
                .price(800.00)
                .build();
    }
    
    public static Computer buildBudgetComputer() {
        return new Computer.ComputerBuilder("AMD Ryzen 5 5600", "8GB DDR4")
                .storage("256GB SSD")
                .price(500.00)
                .build();
    }
}

// Demo class
class BuilderPatternDemo {
    public static void main(String[] args) {
        System.out.println("=== Builder Pattern Demo ===\n");
        
        // 1. Custom Computer Build
        System.out.println("1. Custom Computer Build:");
        demonstrateCustomComputerBuild();
        
        // 2. Director Pattern
        System.out.println("\n2. Director Pattern - Predefined Configurations:");
        demonstrateDirectorPattern();
        
        // 3. SQL Query Builder
        System.out.println("\n3. SQL Query Builder:");
        demonstrateSQLQueryBuilder();
        
        // 4. HTTP Request Builder
        System.out.println("\n4. HTTP Request Builder:");
        demonstrateHttpRequestBuilder();
    }
    
    private static void demonstrateCustomComputerBuild() {
        Computer customComputer = new Computer.ComputerBuilder("AMD Ryzen 7 7700X", "16GB DDR5")
                .storage("1TB NVMe SSD")
                .graphicsCard("NVIDIA RTX 4060 Ti")
                .motherboard("MSI B650 Gaming Plus")
                .powerSupply("650W 80+ Gold")
                .enableWifi()
                .enableBluetooth()
                .addComponent("WiFi 6E Card")
                .addComponent("Sound Card")
                .price(1500.00)
                .build();
        
        System.out.println(customComputer);
    }
    
    private static void demonstrateDirectorPattern() {
        Computer gamingPC = ComputerDirector.buildGamingComputer();
        Computer officePC = ComputerDirector.buildOfficeComputer();
        Computer budgetPC = ComputerDirector.buildBudgetComputer();
        
        System.out.println("Gaming PC:");
        System.out.println(gamingPC);
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        System.out.println("Office PC:");
        System.out.println(officePC);
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        System.out.println("Budget PC:");
        System.out.println(budgetPC);
    }
    
    private static void demonstrateSQLQueryBuilder() {
        SQLQueryBuilder queryBuilder = new SQLQueryBuilder();
        
        // Simple query
        String simpleQuery = queryBuilder
                .select("name", "email")
                .from("users")
                .where("age > 18")
                .orderBy("name ASC")
                .build();
        
        System.out.println("Simple Query: " + simpleQuery);
        
        // Complex query with joins
        queryBuilder.reset();
        String complexQuery = queryBuilder
                .select("u.name", "u.email", "p.title", "c.name as category")
                .from("users u")
                .join("INNER JOIN posts p ON u.id = p.user_id")
                .join("LEFT JOIN categories c ON p.category_id = c.id")
                .where("u.active = 1")
                .where("p.published = 1")
                .orderBy("p.created_at DESC")
                .limit(10)
                .build();
        
        System.out.println("Complex Query: " + complexQuery);
    }
    
    private static void demonstrateHttpRequestBuilder() {
        // GET request
        HttpRequest getRequest = new HttpRequestBuilder()
                .method("GET")
                .url("https://api.example.com/users")
                .header("Authorization", "Bearer token123")
                .header("Accept", "application/json")
                .queryParam("page", "1")
                .queryParam("limit", "10")
                .timeout(5000)
                .build();
        
        System.out.println(getRequest.execute());
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // POST request
        HttpRequest postRequest = new HttpRequestBuilder()
                .method("POST")
                .url("https://api.example.com/users")
                .header("Authorization", "Bearer token123")
                .header("Content-Type", "application/json")
                .body("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}")
                .build();
        
        System.out.println(postRequest.execute());
    }
}

/*
 * Builder Pattern Benefits:
 * 
 * ✅ Advantages:
 * - Allows you to construct objects step-by-step
 * - Can produce different representations using the same construction code
 * - Single Responsibility Principle (construction code is isolated)
 * - Supports method chaining for fluent API
 * - Can enforce invariants during construction
 * - Makes code more readable and maintainable
 * 
 * ❌ Disadvantages:
 * - Overall complexity increases (more classes)
 * - Can be overkill for simple objects
 * - Requires more memory due to builder objects
 * 
 * When to use:
 * - When creating complex objects with many optional parameters
 * - When you want to avoid telescoping constructor anti-pattern
 * - When the construction process must allow different representations
 * - When you need to construct objects step by step
 * - When immutability is desired in the final object
 */
