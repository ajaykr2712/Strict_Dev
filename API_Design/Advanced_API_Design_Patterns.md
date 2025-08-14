# API Design Best Practices for Distributed Systems

This comprehensive guide covers API design patterns and best practices used by Netflix, Uber, WhatsApp, and other major tech companies to build scalable, maintainable APIs.

## 1. RESTful API Design Principles

### Resource-Oriented Design
```http
# Netflix Content API Design
GET    /api/v1/content                    # List all content
GET    /api/v1/content/{id}               # Get specific content
POST   /api/v1/content                    # Create new content
PUT    /api/v1/content/{id}               # Update entire content
PATCH  /api/v1/content/{id}               # Partial update
DELETE /api/v1/content/{id}               # Delete content

# Nested resources
GET    /api/v1/content/{id}/recommendations  # Content recommendations
GET    /api/v1/users/{id}/watchlist          # User's watchlist
POST   /api/v1/users/{id}/watchlist          # Add to watchlist
```

### HTTP Status Codes
```java
// Proper status code usage
@RestController
@RequestMapping("/api/v1/content")
public class ContentController {
    
    @GetMapping("/{id}")
    public ResponseEntity<Content> getContent(@PathVariable String id) {
        Content content = contentService.findById(id);
        if (content == null) {
            return ResponseEntity.notFound().build(); // 404
        }
        return ResponseEntity.ok(content); // 200
    }
    
    @PostMapping
    public ResponseEntity<Content> createContent(@Valid @RequestBody ContentRequest request) {
        Content created = contentService.create(request);
        URI location = URI.create("/api/v1/content/" + created.getId());
        return ResponseEntity.created(location).body(created); // 201
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Content> updateContent(@PathVariable String id, 
                                               @Valid @RequestBody ContentRequest request) {
        if (!contentService.exists(id)) {
            return ResponseEntity.notFound().build(); // 404
        }
        
        Content updated = contentService.update(id, request);
        return ResponseEntity.ok(updated); // 200
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable String id) {
        if (!contentService.exists(id)) {
            return ResponseEntity.notFound().build(); // 404
        }
        
        contentService.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
```

## 2. API Versioning Strategies

### URL Versioning (Netflix Approach)
```java
@RestController
@RequestMapping("/api/v1/movies")
public class MoviesV1Controller {
    
    @GetMapping
    public ResponseEntity<PagedResponse<MovieV1>> getMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // V1 implementation
    }
}

@RestController
@RequestMapping("/api/v2/movies")
public class MoviesV2Controller {
    
    @GetMapping
    public ResponseEntity<PagedResponse<MovieV2>> getMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false) String sortBy) {
        // V2 implementation with enhanced features
    }
}
```

### Header Versioning
```java
@RestController
@RequestMapping("/api/movies")
public class MoviesController {
    
    @GetMapping(headers = "API-Version=1")
    public ResponseEntity<PagedResponse<MovieV1>> getMoviesV1() {
        // Version 1 logic
    }
    
    @GetMapping(headers = "API-Version=2")
    public ResponseEntity<PagedResponse<MovieV2>> getMoviesV2() {
        // Version 2 logic
    }
}
```

### Content Negotiation Versioning
```java
@RestController
public class MoviesController {
    
    @GetMapping(value = "/api/movies", produces = "application/vnd.company.movie.v1+json")
    public ResponseEntity<MovieV1> getMovieV1() {
        // Version 1
    }
    
    @GetMapping(value = "/api/movies", produces = "application/vnd.company.movie.v2+json")
    public ResponseEntity<MovieV2> getMovieV2() {
        // Version 2
    }
}
```

## 3. Error Handling and Response Formats

### Standardized Error Response
```java
// Error response structure
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private String error;
    private String message;
    private String details;
    private String timestamp;
    private String path;
    private Integer status;
    private List<ValidationError> validationErrors;
    
    // Constructors and getters
    
    public static class ValidationError {
        private String field;
        private Object rejectedValue;
        private String message;
        
        // Constructors and getters
    }
}

// Global exception handler
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        
        ApiErrorResponse error = new ApiErrorResponse();
        error.setError("RESOURCE_NOT_FOUND");
        error.setMessage(ex.getMessage());
        error.setStatus(404);
        error.setTimestamp(Instant.now().toString());
        error.setPath(request.getRequestURI());
        
        return ResponseEntity.status(404).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        List<ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fieldError -> new ValidationError(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage()
            ))
            .collect(Collectors.toList());
        
        ApiErrorResponse error = new ApiErrorResponse();
        error.setError("VALIDATION_FAILED");
        error.setMessage("Request validation failed");
        error.setStatus(400);
        error.setTimestamp(Instant.now().toString());
        error.setPath(request.getRequestURI());
        error.setValidationErrors(validationErrors);
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(
            RateLimitExceededException ex, HttpServletRequest request) {
        
        ApiErrorResponse error = new ApiErrorResponse();
        error.setError("RATE_LIMIT_EXCEEDED");
        error.setMessage("API rate limit exceeded");
        error.setDetails("Retry after: " + ex.getRetryAfter() + " seconds");
        error.setStatus(429);
        error.setTimestamp(Instant.now().toString());
        error.setPath(request.getRequestURI());
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.getRetryAfter()));
        
        return ResponseEntity.status(429).headers(headers).body(error);
    }
}
```

## 4. Pagination and Filtering

### Cursor-Based Pagination (WhatsApp Message History)
```java
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {
    
    @GetMapping
    public ResponseEntity<CursorPagedResponse<Message>> getMessages(
            @RequestParam String chatId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") @Max(100) int limit) {
        
        CursorPagedResponse<Message> response = messageService.getMessages(
            chatId, cursor, limit
        );
        
        return ResponseEntity.ok(response);
    }
}

// Cursor-based response
public class CursorPagedResponse<T> {
    private List<T> data;
    private String nextCursor;
    private String previousCursor;
    private boolean hasNext;
    private boolean hasPrevious;
    private int limit;
    
    // Constructors and getters
}

// Service implementation
@Service
public class MessageService {
    
    public CursorPagedResponse<Message> getMessages(String chatId, String cursor, int limit) {
        // Build query with cursor
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM messages WHERE chat_id = ? "
        );
        
        List<Object> params = new ArrayList<>();
        params.add(chatId);
        
        if (cursor != null) {
            sql.append("AND created_at < ? ");
            params.add(decodeCursor(cursor));
        }
        
        sql.append("ORDER BY created_at DESC LIMIT ?");
        params.add(limit + 1); // Fetch one extra to check hasNext
        
        List<Message> messages = jdbcTemplate.query(sql.toString(), 
            params.toArray(), new MessageRowMapper());
        
        boolean hasNext = messages.size() > limit;
        if (hasNext) {
            messages.remove(messages.size() - 1); // Remove extra item
        }
        
        String nextCursor = hasNext && !messages.isEmpty() ? 
            encodeCursor(messages.get(messages.size() - 1).getCreatedAt()) : null;
        
        return new CursorPagedResponse<>(messages, nextCursor, null, hasNext, false, limit);
    }
}
```

### Advanced Filtering and Sorting
```java
// Netflix content filtering
@GetMapping("/api/v1/content")
public ResponseEntity<PagedResponse<Content>> getContent(
        @RequestParam(required = false) List<String> genres,
        @RequestParam(required = false) @Range(min = 1900, max = 2030) Integer releaseYear,
        @RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("10.0") Double minRating,
        @RequestParam(required = false) List<String> languages,
        @RequestParam(required = false) ContentType type,
        @RequestParam(defaultValue = "popularity") String sortBy,
        @RequestParam(defaultValue = "desc") String sortOrder,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") @Max(100) int size) {
    
    ContentFilter filter = ContentFilter.builder()
        .genres(genres)
        .releaseYear(releaseYear)
        .minRating(minRating)
        .languages(languages)
        .type(type)
        .build();
    
    Sort sort = Sort.by(
        "desc".equalsIgnoreCase(sortOrder) ? 
            Sort.Direction.DESC : Sort.Direction.ASC,
        sortBy
    );
    
    Pageable pageable = PageRequest.of(page, size, sort);
    
    Page<Content> contentPage = contentService.findWithFilter(filter, pageable);
    
    PagedResponse<Content> response = PagedResponse.<Content>builder()
        .data(contentPage.getContent())
        .page(page)
        .size(size)
        .totalElements(contentPage.getTotalElements())
        .totalPages(contentPage.getTotalPages())
        .hasNext(contentPage.hasNext())
        .hasPrevious(contentPage.hasPrevious())
        .build();
    
    return ResponseEntity.ok(response);
}
```

## 5. Rate Limiting and Throttling

### Token Bucket Rate Limiting
```java
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        
        String clientId = extractClientId(request);
        String endpoint = request.getRequestURI();
        
        RateLimitConfig config = getRateLimitConfig(endpoint);
        
        if (!isRequestAllowed(clientId, endpoint, config)) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(config.getWindowSeconds()));
            response.setHeader("X-RateLimit-Limit", String.valueOf(config.getMaxRequests()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", 
                String.valueOf(System.currentTimeMillis() + config.getWindowSeconds() * 1000));
            
            response.getWriter().write("{\"error\":\"RATE_LIMIT_EXCEEDED\"}");
            return false;
        }
        
        return true;
    }
    
    private boolean isRequestAllowed(String clientId, String endpoint, RateLimitConfig config) {
        String key = "rate_limit:" + clientId + ":" + endpoint;
        String currentCount = redisTemplate.opsForValue().get(key);
        
        if (currentCount == null) {
            redisTemplate.opsForValue().set(key, "1", 
                Duration.ofSeconds(config.getWindowSeconds()));
            return true;
        }
        
        int count = Integer.parseInt(currentCount);
        if (count >= config.getMaxRequests()) {
            return false;
        }
        
        redisTemplate.opsForValue().increment(key);
        return true;
    }
}

// Rate limit configuration
public class RateLimitConfig {
    private int maxRequests;
    private int windowSeconds;
    private String endpoint;
    
    // Getters and constructors
}
```

## 6. Request/Response Validation

### Input Validation
```java
// Request DTOs with validation
public class CreateUserRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]", 
             message = "Password must contain at least one uppercase, lowercase, digit and special character")
    private String password;
    
    @NotNull(message = "User preferences are required")
    @Valid
    private UserPreferences preferences;
    
    @JsonProperty("profile_picture_url")
    @URL(message = "Invalid profile picture URL")
    private String profilePictureUrl;
    
    // Getters and setters
}

public class UserPreferences {
    
    @NotEmpty(message = "At least one preferred genre is required")
    @Size(max = 10, message = "Maximum 10 preferred genres allowed")
    private List<@NotBlank String> preferredGenres;
    
    @NotNull(message = "Language preference is required")
    @Pattern(regexp = "^[a-z]{2}$", message = "Language must be a valid 2-character code")
    private String language;
    
    @DecimalMin(value = "0.0", message = "Minimum rating must be positive")
    @DecimalMax(value = "10.0", message = "Maximum rating cannot exceed 10")
    private Double minimumRating;
    
    // Getters and setters
}

// Custom validation annotations
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContentRatingValidator.class)
public @interface ValidContentRating {
    String message() default "Invalid content rating";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class ContentRatingValidator implements ConstraintValidator<ValidContentRating, String> {
    
    private static final Set<String> VALID_RATINGS = Set.of(
        "G", "PG", "PG-13", "R", "NC-17", "TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14", "TV-MA"
    );
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || VALID_RATINGS.contains(value);
    }
}
```

## 7. Content Negotiation and HATEOAS

### Content Negotiation
```java
@RestController
@RequestMapping("/api/v1/content")
public class ContentController {
    
    @GetMapping(value = "/{id}", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        "application/vnd.api+json"
    })
    public ResponseEntity<?> getContent(@PathVariable String id, 
                                       HttpServletRequest request) {
        
        Content content = contentService.findById(id);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        
        String acceptHeader = request.getHeader("Accept");
        
        if (acceptHeader.contains("application/xml")) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(content);
        } else if (acceptHeader.contains("application/vnd.api+json")) {
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.api+json"))
                .body(toJsonApiFormat(content));
        } else {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(content);
        }
    }
}
```

### HATEOAS Implementation
```java
// HATEOAS-enabled response
@RestController
public class ContentHateoasController {
    
    @GetMapping("/api/v1/content/{id}")
    public ResponseEntity<EntityModel<Content>> getContent(@PathVariable String id) {
        Content content = contentService.findById(id);
        
        EntityModel<Content> contentModel = EntityModel.of(content);
        
        // Add self link
        contentModel.add(linkTo(methodOn(ContentController.class)
            .getContent(id)).withSelfRel());
        
        // Add related links
        contentModel.add(linkTo(methodOn(ContentController.class)
            .getRecommendations(id)).withRel("recommendations"));
        
        contentModel.add(linkTo(methodOn(ContentController.class)
            .getReviews(id)).withRel("reviews"));
        
        // Conditional links based on user permissions
        if (hasEditPermission()) {
            contentModel.add(linkTo(methodOn(ContentController.class)
                .updateContent(id, null)).withRel("edit"));
        }
        
        if (hasDeletePermission()) {
            contentModel.add(linkTo(methodOn(ContentController.class)
                .deleteContent(id)).withRel("delete"));
        }
        
        return ResponseEntity.ok(contentModel);
    }
    
    @GetMapping("/api/v1/content")
    public ResponseEntity<CollectionModel<EntityModel<Content>>> getAllContent(
            Pageable pageable) {
        
        Page<Content> contentPage = contentService.findAll(pageable);
        
        List<EntityModel<Content>> contentModels = contentPage.getContent()
            .stream()
            .map(content -> EntityModel.of(content)
                .add(linkTo(methodOn(ContentController.class)
                    .getContent(content.getId())).withSelfRel()))
            .collect(Collectors.toList());
        
        CollectionModel<EntityModel<Content>> collectionModel = 
            CollectionModel.of(contentModels);
        
        // Add pagination links
        if (contentPage.hasNext()) {
            collectionModel.add(linkTo(methodOn(ContentController.class)
                .getAllContent(pageable.next())).withRel("next"));
        }
        
        if (contentPage.hasPrevious()) {
            collectionModel.add(linkTo(methodOn(ContentController.class)
                .getAllContent(pageable.previousOrFirst())).withRel("prev"));
        }
        
        collectionModel.add(linkTo(methodOn(ContentController.class)
            .getAllContent(pageable)).withSelfRel());
        
        return ResponseEntity.ok(collectionModel);
    }
}
```

## 8. API Documentation and Testing

### OpenAPI 3.0 Documentation
```java
@OpenAPIDefinition(
    info = @Info(
        title = "Content Management API",
        version = "1.0",
        description = "API for managing content in streaming platform",
        contact = @Contact(name = "API Support", email = "api-support@company.com"),
        license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
    ),
    servers = {
        @Server(url = "https://api.company.com", description = "Production server"),
        @Server(url = "https://staging-api.company.com", description = "Staging server")
    }
)
@RestController
public class ContentController {
    
    @Operation(
        summary = "Get content by ID",
        description = "Retrieves detailed information about a specific content item",
        tags = {"Content"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Content found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Content.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Content not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded",
            headers = @Header(name = "Retry-After", description = "Seconds to wait before retry")
        )
    })
    @GetMapping("/api/v1/content/{id}")
    public ResponseEntity<Content> getContent(
            @Parameter(description = "Content ID", example = "content123")
            @PathVariable String id) {
        // Implementation
    }
}
```

## 9. API Security Best Practices

### Security Headers and CORS
```java
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .contentTypeOptions(ContentTypeOptionsHeaderWriter.Directive.NOSNIFF)
                .frameOptions(FrameOptionsHeaderWriter.Mode.DENY)
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true))
                .and()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder())));
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("https://*.company.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

## Best Practices Summary

1. **Consistent Naming**: Use kebab-case for URLs, camelCase for JSON
2. **Proper HTTP Methods**: GET for retrieval, POST for creation, PUT for updates, DELETE for removal
3. **Status Codes**: Use appropriate HTTP status codes for different scenarios
4. **Versioning**: Plan for API evolution from the beginning
5. **Error Handling**: Provide consistent, detailed error responses
6. **Pagination**: Use cursor-based pagination for large datasets
7. **Rate Limiting**: Protect your API from abuse
8. **Validation**: Validate all input thoroughly
9. **Documentation**: Maintain comprehensive API documentation
10. **Security**: Implement proper authentication, authorization, and security headers
11. **Monitoring**: Add logging, metrics, and health checks
12. **Testing**: Include unit, integration, and contract tests
