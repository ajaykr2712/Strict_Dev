# Spring Boot Fundamentals - Interview Guide

## Table of Contents
1. [Spring Boot Basics](#spring-boot-basics)
2. [Dependency Injection](#dependency-injection)
3. [REST API Development](#rest-api-development)
4. [Data Access Layer](#data-access-layer)
5. [Security Implementation](#security-implementation)
6. [Testing Strategies](#testing-strategies)
7. [Deployment and Configuration](#deployment-and-configuration)

---

## Spring Boot Basics

### Q1: What is Spring Boot and its advantages over traditional Spring?

**Answer:**

Spring Boot is a framework that simplifies the development of Spring-based applications by providing:

**Key Advantages:**
1. **Auto-Configuration**: Automatically configures Spring beans based on classpath
2. **Embedded Servers**: Built-in Tomcat, Jetty, or Undertow
3. **Starter Dependencies**: Pre-configured dependency sets
4. **Production-Ready Features**: Metrics, health checks, externalized configuration
5. **Minimal Configuration**: Convention over configuration approach

**Traditional Spring vs Spring Boot:**

```java
// Traditional Spring - XML Configuration
// applicationContext.xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans">
    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource">
        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/testdb"/>
        <property name="username" value="root"/>
        <property name="password" value="password"/>
    </bean>
    
    <bean id="entityManagerFactory" 
          class="org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <property name="packagesToScan" value="com.example.entity"/>
    </bean>
</beans>

// Spring Boot - Auto Configuration
// application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/testdb
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update

// That's it! Spring Boot auto-configures everything else
```

### Q2: Explain the Spring Boot application structure and annotations

**Answer:**

#### Main Application Class
```java
@SpringBootApplication // Combines @Configuration, @EnableAutoConfiguration, @ComponentScan
public class UserManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }
}
```

#### Complete Application Structure
```java
// Entity Layer
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Constructors
    public User() {}
    
    public User(String email, String firstName, String lastName, String phoneNumber) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

// Repository Layer
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    List<User> findByFirstNameContaining(String firstName);
    
    List<User> findByLastNameContaining(String lastName);
    
    @Query("SELECT u FROM User u WHERE u.firstName LIKE %:name% OR u.lastName LIKE %:name%")
    List<User> findByNameContaining(@Param("name") String name);
    
    @Query(value = "SELECT * FROM users WHERE created_at > :date", nativeQuery = true)
    List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);
    
    boolean existsByEmail(String email);
    
    void deleteByEmail(String email);
}

// Service Layer
@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }
        return userRepository.save(user);
    }
    
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        
        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(userDetails.getEmail()) && 
            userRepository.existsByEmail(userDetails.getEmail())) {
            throw new UserAlreadyExistsException("Email " + userDetails.getEmail() + " is already in use");
        }
        
        user.setEmail(userDetails.getEmail());
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setPhoneNumber(userDetails.getPhoneNumber());
        
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
    
    public List<User> searchUsersByName(String name) {
        return userRepository.findByNameContaining(name);
    }
    
    public List<User> getRecentUsers(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return userRepository.findUsersCreatedAfter(cutoffDate);
    }
}

// Controller Layer
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Validated
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userService.getUserByEmail(email);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = new User(
            request.getEmail(),
            request.getFirstName(),
            request.getLastName(),
            request.getPhoneNumber()
        );
        
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, 
                                         @Valid @RequestBody UpdateUserRequest request) {
        try {
            User userDetails = new User();
            userDetails.setEmail(request.getEmail());
            userDetails.setFirstName(request.getFirstName());
            userDetails.setLastName(request.getLastName());
            userDetails.setPhoneNumber(request.getPhoneNumber());
            
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String name) {
        List<User> users = userService.searchUsersByName(name);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<User>> getRecentUsers(@RequestParam(defaultValue = "30") int days) {
        List<User> users = userService.getRecentUsers(days);
        return ResponseEntity.ok(users);
    }
}
```

---

## Dependency Injection

### Q3: Explain different types of Dependency Injection in Spring Boot

**Answer:**

#### 1. Constructor Injection (Recommended)
```java
@Service
public class EmailService {
    
    private final EmailRepository emailRepository;
    private final EmailValidator emailValidator;
    private final EmailSender emailSender;
    
    // Constructor injection - recommended approach
    public EmailService(EmailRepository emailRepository,
                       EmailValidator emailValidator,
                       EmailSender emailSender) {
        this.emailRepository = emailRepository;
        this.emailValidator = emailValidator;
        this.emailSender = emailSender;
    }
    
    public void sendEmail(String to, String subject, String body) {
        if (!emailValidator.isValid(to)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        
        Email email = new Email(to, subject, body);
        emailRepository.save(email);
        emailSender.send(email);
    }
}
```

#### 2. Field Injection (Not Recommended)
```java
@Service
public class NotificationService {
    
    @Autowired
    private EmailService emailService; // Field injection - avoid this
    
    @Autowired
    private SMSService smsService;
    
    // Problems with field injection:
    // 1. Cannot create immutable fields (final)
    // 2. Harder to test (reflection needed)
    // 3. Circular dependency issues harder to detect
    // 4. Tight coupling with Spring framework
}
```

#### 3. Setter Injection (Optional Dependencies)
```java
@Service
public class ReportService {
    
    private EmailService emailService;
    private Optional<AnalyticsService> analyticsService = Optional.empty();
    
    // Required dependency via constructor
    public ReportService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    // Optional dependency via setter
    @Autowired(required = false)
    public void setAnalyticsService(AnalyticsService analyticsService) {
        this.analyticsService = Optional.ofNullable(analyticsService);
    }
    
    public void generateReport(String reportType) {
        // Generate report logic
        String report = createReport(reportType);
        
        // Send email (required)
        emailService.sendEmail("admin@company.com", "Report Generated", report);
        
        // Track analytics (optional)
        analyticsService.ifPresent(service -> 
            service.trackEvent("report_generated", reportType));
    }
}
```

### Q4: Configuration and Bean Creation

**Answer:**

#### Configuration Classes
```java
@Configuration
@EnableConfigurationProperties({DatabaseProperties.class, EmailProperties.class})
public class ApplicationConfig {
    
    // Bean creation with method
    @Bean
    @ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
    public EmailSender emailSender(EmailProperties emailProperties) {
        return new SMTPEmailSender(
            emailProperties.getHost(),
            emailProperties.getPort(),
            emailProperties.getUsername(),
            emailProperties.getPassword()
        );
    }
    
    // Conditional bean creation
    @Bean
    @Profile("dev")
    public EmailSender devEmailSender() {
        return new ConsoleEmailSender(); // Prints to console in dev environment
    }
    
    // Bean with primary annotation
    @Bean
    @Primary
    public CacheManager primaryCacheManager() {
        return new RedisCacheManager();
    }
    
    @Bean
    @Qualifier("localCache")
    public CacheManager localCacheManager() {
        return new LocalCacheManager();
    }
    
    // Custom validator bean
    @Bean
    public EmailValidator emailValidator() {
        return new RegexEmailValidator("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}

// Configuration properties
@ConfigurationProperties(prefix = "app.database")
@Data // Lombok annotation for getters/setters
public class DatabaseProperties {
    private String url;
    private String username;
    private String password;
    private int maxConnections = 10;
    private boolean enableLogging = false;
}

@ConfigurationProperties(prefix = "app.email")
@Data
public class EmailProperties {
    private String host;
    private int port = 587;
    private String username;
    private String password;
    private boolean enableTls = true;
}
```

#### Using Configuration Properties
```properties
# application.properties
app.database.url=jdbc:mysql://localhost:3306/myapp
app.database.username=dbuser
app.database.password=dbpass
app.database.max-connections=20
app.database.enable-logging=true

app.email.host=smtp.gmail.com
app.email.port=587
app.email.username=noreply@company.com
app.email.password=${EMAIL_PASSWORD}
app.email.enable-tls=true

# Profile-specific properties
spring.profiles.active=dev

# Logging configuration
logging.level.com.example=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

---

## REST API Development

### Q5: Build a comprehensive REST API with proper error handling

**Answer:**

#### DTOs (Data Transfer Objects)
```java
// Request DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    @Email(message = "Email should be valid")
    private String email;
    
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
}

// Response DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String fullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phoneNumber = user.getPhoneNumber();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
```

#### Exception Handling
```java
// Custom Exceptions
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidUserDataException extends RuntimeException {
    public InvalidUserDataException(String message) {
        super(message);
    }
}

// Global Exception Handler
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                           .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                           .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation errors: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(new ApiResponse<>(false, "Validation failed", errors, LocalDateTime.now()));
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        String message = "Data integrity violation. Possible duplicate entry.";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                           .body(ApiResponse.error(message));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(ApiResponse.error("An unexpected error occurred"));
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                           .body(ApiResponse.error("HTTP method not supported: " + ex.getMethod()));
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ApiResponse.error("Missing required parameter: " + ex.getParameterName()));
    }
}
```

#### Enhanced Controller with OpenAPI Documentation
```java
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = {"http://localhost:3000", "https://myapp.com"})
@Validated
@Tag(name = "User Management", description = "APIs for managing users")
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.info("Fetching users - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        Pageable pageable = PageRequest.of(page, size, 
            sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy));
        
        Page<User> userPage = userService.getAllUsers(pageable);
        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", userResponses));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable @Positive Long id) {
        
        log.info("Fetching user with id: {}", id);
        
        User user = userService.getUserById(id);
        UserResponse userResponse = new UserResponse(user);
        
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userResponse));
    }
    
    @PostMapping
    @Operation(summary = "Create new user", description = "Create a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        
        log.info("Creating user with email: {}", request.getEmail());
        
        User user = new User(
            request.getEmail(),
            request.getFirstName(),
            request.getLastName(),
            request.getPhoneNumber()
        );
        
        User createdUser = userService.createUser(user);
        UserResponse userResponse = new UserResponse(createdUser);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                           .body(ApiResponse.success("User created successfully", userResponse));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user's information")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        
        log.info("Updating user with id: {}", id);
        
        User updatedUser = userService.updateUser(id, request);
        UserResponse userResponse = new UserResponse(updatedUser);
        
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userResponse));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable @Positive Long id) {
        log.info("Deleting user with id: {}", id);
        
        userService.deleteUser(id);
        
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by name or email")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        log.info("Searching users with query: {}", query);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userService.searchUsers(query, pageable);
        
        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", userResponses));
    }
}
```

---

## Data Access Layer

### Q6: JPA/Hibernate implementation with relationships

**Answer:**

#### Entity Relationships
```java
// User entity with relationships
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    // One-to-Many relationship
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Order> orders = new ArrayList<>();
    
    // One-to-One relationship
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private UserProfile profile;
    
    // Many-to-Many relationship
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Helper methods for bidirectional relationships
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }
    
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }
    
    public void setProfile(UserProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }
    
    public void addRole(Role role) {
        roles.add(role);
        role.getUsers().add(this);
    }
    
    public void removeRole(Role role) {
        roles.remove(role);
        role.getUsers().remove(this);
    }
}

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String orderNumber;
    
    @Column(nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    // Many-to-One relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;
    
    // One-to-Many relationship
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }
    
    // Helper methods
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
    
    public BigDecimal calculateTotal() {
        return items.stream()
                   .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                   .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

#### Advanced Repository Patterns
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    
    // Query methods
    Optional<User> findByEmail(String email);
    
    List<User> findByFirstNameContainingIgnoreCase(String firstName);
    
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Custom JPQL queries
    @Query("SELECT u FROM User u WHERE u.firstName LIKE %:name% OR u.lastName LIKE %:name%")
    List<User> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT u FROM User u JOIN u.orders o WHERE o.status = :status")
    List<User> findUsersWithOrderStatus(@Param("status") Order.OrderStatus status);
    
    @Query(value = "SELECT u.* FROM users u " +
                   "JOIN orders o ON u.id = o.user_id " +
                   "WHERE o.total_amount > :amount", nativeQuery = true)
    List<User> findUsersWithOrdersAboveAmount(@Param("amount") BigDecimal amount);
    
    // Modifying queries
    @Modifying
    @Query("UPDATE User u SET u.lastName = :lastName WHERE u.id = :id")
    int updateUserLastName(@Param("id") Long id, @Param("lastName") String lastName);
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.createdAt < :date")
    int deleteUsersCreatedBefore(@Param("date") LocalDateTime date);
    
    // Projection interfaces
    interface UserSummary {
        String getFirstName();
        String getLastName();
        String getEmail();
        LocalDateTime getCreatedAt();
    }
    
    @Query("SELECT u.firstName as firstName, u.lastName as lastName, " +
           "u.email as email, u.createdAt as createdAt FROM User u")
    List<UserSummary> findAllUserSummaries();
    
    // Dynamic projections
    <T> List<T> findByEmailContaining(String email, Class<T> type);
}

// Custom repository implementation
public interface UserRepositoryCustom {
    Page<User> findUsersWithCustomCriteria(UserSearchCriteria criteria, Pageable pageable);
    List<UserStatistics> getUserStatistics();
}

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Page<User> findUsersWithCustomCriteria(UserSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (criteria.getName() != null && !criteria.getName().isEmpty()) {
            Predicate firstNamePredicate = cb.like(
                cb.lower(user.get("firstName")), 
                "%" + criteria.getName().toLowerCase() + "%"
            );
            Predicate lastNamePredicate = cb.like(
                cb.lower(user.get("lastName")), 
                "%" + criteria.getName().toLowerCase() + "%"
            );
            predicates.add(cb.or(firstNamePredicate, lastNamePredicate));
        }
        
        if (criteria.getEmail() != null && !criteria.getEmail().isEmpty()) {
            predicates.add(cb.like(
                cb.lower(user.get("email")), 
                "%" + criteria.getEmail().toLowerCase() + "%"
            ));
        }
        
        if (criteria.getCreatedAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(user.get("createdAt"), criteria.getCreatedAfter()));
        }
        
        if (criteria.getCreatedBefore() != null) {
            predicates.add(cb.lessThanOrEqualTo(user.get("createdAt"), criteria.getCreatedBefore()));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            List<javax.persistence.criteria.Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(cb.asc(user.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(user.get(order.getProperty())));
                }
            });
            query.orderBy(orders);
        }
        
        TypedQuery<User> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<User> users = typedQuery.getResultList();
        
        // Count query for total elements
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<User> countRoot = countQuery.from(User.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        
        Long total = entityManager.createQuery(countQuery).getSingleResult();
        
        return new PageImpl<>(users, pageable, total);
    }
    
    @Override
    public List<UserStatistics> getUserStatistics() {
        String jpql = """
            SELECT new com.example.dto.UserStatistics(
                EXTRACT(YEAR FROM u.createdAt) as year,
                EXTRACT(MONTH FROM u.createdAt) as month,
                COUNT(u) as userCount,
                COUNT(o) as orderCount,
                COALESCE(SUM(o.totalAmount), 0) as totalRevenue
            )
            FROM User u
            LEFT JOIN u.orders o
            GROUP BY EXTRACT(YEAR FROM u.createdAt), EXTRACT(MONTH FROM u.createdAt)
            ORDER BY year DESC, month DESC
            """;
        
        return entityManager.createQuery(jpql, UserStatistics.class)
                          .getResultList();
    }
}
```

This Spring Boot guide covers the essential concepts you'll need for your interview. The examples demonstrate real-world application scenarios with proper error handling, validation, and best practices.

Would you like me to continue with the remaining sections (Security, Testing, and Deployment) or create additional files for the Coding Challenges section?
