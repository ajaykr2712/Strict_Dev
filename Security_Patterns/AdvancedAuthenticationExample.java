package com.systemdesign.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Advanced Authentication and Authorization System
 * 
 * Implements OAuth 2.0 + JWT with role-based access control (RBAC)
 * and attribute-based access control (ABAC) patterns used by
 * companies like Netflix, Uber, and WhatsApp.
 * 
 * Features:
 * - JWT token generation and validation
 * - Role-based permissions
 * - Session management
 * - Rate limiting per user
 * - Audit logging
 * - Multi-factor authentication support
 */

// User entity with roles and attributes
class User {
    private final String userId;
    private final String email;
    private final String hashedPassword;
    private final Set<Role> roles;
    private final Map<String, Object> attributes;
    private final boolean mfaEnabled;
    private final Instant createdAt;
    private volatile Instant lastLoginAt;
    private final AtomicInteger failedLoginAttempts;
    
    public User(String userId, String email, String hashedPassword, 
               Set<Role> roles, Map<String, Object> attributes) {
        this.userId = userId;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.roles = new HashSet<>(roles);
        this.attributes = new HashMap<>(attributes);
        this.mfaEnabled = (Boolean) attributes.getOrDefault("mfaEnabled", false);
        this.createdAt = Instant.now();
        this.lastLoginAt = null;
        this.failedLoginAttempts = new AtomicInteger(0);
    }
    
    public void recordSuccessfulLogin() {
        this.lastLoginAt = Instant.now();
        this.failedLoginAttempts.set(0);
    }
    
    public int recordFailedLogin() {
        return failedLoginAttempts.incrementAndGet();
    }
    
    public boolean isLocked() {
        return failedLoginAttempts.get() >= 5;
    }
    
    // Getters
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getHashedPassword() { return hashedPassword; }
    public Set<Role> getRoles() { return new HashSet<>(roles); }
    public Map<String, Object> getAttributes() { return new HashMap<>(attributes); }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public int getFailedLoginAttempts() { return failedLoginAttempts.get(); }
}

// Role with permissions
class Role {
    private final String name;
    private final String description;
    private final Set<Permission> permissions;
    private final int priority; // Higher number = higher priority
    
    public Role(String name, String description, Set<Permission> permissions, int priority) {
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>(permissions);
        this.priority = priority;
    }
    
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
    
    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Set<Permission> getPermissions() { return new HashSet<>(permissions); }
    public int getPriority() { return priority; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Role role = (Role) obj;
        return Objects.equals(name, role.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

// Permissions enum
enum Permission {
    // Netflix-style permissions
    CONTENT_READ, CONTENT_WRITE, CONTENT_DELETE,
    USER_READ, USER_WRITE, USER_DELETE,
    ANALYTICS_READ, ANALYTICS_WRITE,
    ADMIN_PANEL_ACCESS,
    
    // Uber-style permissions
    DRIVER_MANAGEMENT, RIDER_MANAGEMENT,
    TRIP_MANAGEMENT, PAYMENT_PROCESSING,
    SURGE_PRICING_CONTROL,
    
    // WhatsApp-style permissions
    MESSAGE_READ, MESSAGE_SEND, MESSAGE_DELETE,
    GROUP_CREATE, GROUP_MANAGE,
    BROADCAST_SEND,
    
    // System permissions
    SYSTEM_CONFIG, AUDIT_LOG_READ, API_ACCESS
}

// JWT token structure
class JwtToken {
    private final String tokenId;
    private final String userId;
    private final Set<String> roles;
    private final Map<String, Object> claims;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final String signature;
    
    public JwtToken(String tokenId, String userId, Set<String> roles, 
                   Map<String, Object> claims, Duration validity, String signature) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.roles = new HashSet<>(roles);
        this.claims = new HashMap<>(claims);
        this.issuedAt = Instant.now();
        this.expiresAt = issuedAt.plus(validity);
        this.signature = signature;
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !isExpired() && signature != null;
    }
    
    // Getters
    public String getTokenId() { return tokenId; }
    public String getUserId() { return userId; }
    public Set<String> getRoles() { return new HashSet<>(roles); }
    public Map<String, Object> getClaims() { return new HashMap<>(claims); }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getSignature() { return signature; }
}

// Session management
class UserSession {
    private final String sessionId;
    private final String userId;
    private final String deviceId;
    private final String ipAddress;
    private final String userAgent;
    private final Instant createdAt;
    private volatile Instant lastAccessedAt;
    private final AtomicLong requestCount;
    private volatile boolean active;
    
    public UserSession(String sessionId, String userId, String deviceId, 
                      String ipAddress, String userAgent) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = Instant.now();
        this.lastAccessedAt = createdAt;
        this.requestCount = new AtomicLong(0);
        this.active = true;
    }
    
    public void updateAccess() {
        this.lastAccessedAt = Instant.now();
        this.requestCount.incrementAndGet();
    }
    
    public void invalidate() {
        this.active = false;
    }
    
    public boolean isExpired(Duration sessionTimeout) {
        return Instant.now().isAfter(lastAccessedAt.plus(sessionTimeout));
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAccessedAt() { return lastAccessedAt; }
    public long getRequestCount() { return requestCount.get(); }
    public boolean isActive() { return active; }
}

// Audit log entry
class AuditLogEntry {
    private final String entryId;
    private final String userId;
    private final String action;
    private final String resource;
    private final String ipAddress;
    private final String userAgent;
    private final Instant timestamp;
    private final boolean success;
    private final String details;
    
    public AuditLogEntry(String userId, String action, String resource, 
                        String ipAddress, String userAgent, boolean success, String details) {
        this.entryId = UUID.randomUUID().toString();
        this.userId = userId;
        this.action = action;
        this.resource = resource;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = Instant.now();
        this.success = success;
        this.details = details;
    }
    
    // Getters
    public String getEntryId() { return entryId; }
    public String getUserId() { return userId; }
    public String getAction() { return action; }
    public String getResource() { return resource; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getTimestamp() { return timestamp; }
    public boolean isSuccess() { return success; }
    public String getDetails() { return details; }
    
    @Override
    public String toString() {
        return String.format("AuditLog{user=%s, action=%s, resource=%s, success=%s, time=%s}",
                           userId, action, resource, success, timestamp);
    }
}

// Main authentication service
class AdvancedAuthenticationService {
    private final Map<String, User> users;
    private final Map<String, UserSession> activeSessions;
    private final Map<String, JwtToken> activeTokens;
    private final List<AuditLogEntry> auditLog;
    private final SecureRandom secureRandom;
    private final ScheduledExecutorService cleanupExecutor;
    
    // Configuration
    private final Duration sessionTimeout = Duration.ofHours(24);
    private final Duration tokenValidity = Duration.ofHours(1);
    private final String jwtSecret = "super-secret-key-should-be-from-config";
    private final int maxSessionsPerUser = 5;
    
    // Rate limiting
    private final Map<String, AtomicInteger> loginAttempts;
    private final Map<String, Instant> lastAttemptTime;
    
    public AdvancedAuthenticationService() {
        this.users = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.activeTokens = new ConcurrentHashMap<>();
        this.auditLog = new CopyOnWriteArrayList<>();
        this.secureRandom = new SecureRandom();
        this.cleanupExecutor = Executors.newScheduledThreadPool(2);
        this.loginAttempts = new ConcurrentHashMap<>();
        this.lastAttemptTime = new ConcurrentHashMap<>();
        
        initializeDefaultUsers();
        startCleanupTasks();
    }
    
    private void initializeDefaultUsers() {
        // Netflix-style roles
        Set<Permission> adminPerms = EnumSet.of(
            Permission.CONTENT_READ, Permission.CONTENT_WRITE, Permission.CONTENT_DELETE,
            Permission.USER_READ, Permission.USER_WRITE, Permission.ADMIN_PANEL_ACCESS,
            Permission.ANALYTICS_READ, Permission.SYSTEM_CONFIG
        );
        
        Set<Permission> contentManagerPerms = EnumSet.of(
            Permission.CONTENT_READ, Permission.CONTENT_WRITE,
            Permission.ANALYTICS_READ
        );
        
        Set<Permission> userPerms = EnumSet.of(
            Permission.CONTENT_READ, Permission.MESSAGE_READ, Permission.MESSAGE_SEND
        );
        
        Role adminRole = new Role("ADMIN", "System Administrator", adminPerms, 100);
        Role contentManagerRole = new Role("CONTENT_MANAGER", "Content Manager", contentManagerPerms, 50);
        Role userRole = new Role("USER", "Regular User", userPerms, 10);
        
        // Create users
        Map<String, Object> adminAttrs = Map.of(
            "department", "IT",
            "mfaEnabled", true,
            "region", "US"
        );
        
        Map<String, Object> userAttrs = Map.of(
            "subscription", "premium",
            "mfaEnabled", false,
            "region", "US"
        );
        
        users.put("admin", new User("admin", "admin@company.com", 
                                  hashPassword("admin123"), 
                                  Set.of(adminRole), adminAttrs));
        
        users.put("content_manager", new User("content_manager", "cm@company.com",
                                            hashPassword("cm123"),
                                            Set.of(contentManagerRole), userAttrs));
        
        users.put("user1", new User("user1", "user1@company.com",
                                   hashPassword("user123"),
                                   Set.of(userRole), userAttrs));
    }
    
    // Authentication methods
    public AuthenticationResult authenticate(String email, String password, 
                                           String ipAddress, String userAgent, String deviceId) {
        logAuditEvent(null, "LOGIN_ATTEMPT", "AUTH_SERVICE", ipAddress, userAgent, false, 
                     "Attempt for email: " + email);
        
        // Rate limiting check
        if (isRateLimited(ipAddress)) {
            return new AuthenticationResult(false, "Too many login attempts. Please try again later.", 
                                          null, null);
        }
        
        // Find user by email
        User user = users.values().stream()
            .filter(u -> u.getEmail().equals(email))
            .findFirst()
            .orElse(null);
        
        if (user == null || !verifyPassword(password, user.getHashedPassword())) {
            recordFailedAttempt(ipAddress);
            if (user != null) {
                user.recordFailedLogin();
                logAuditEvent(user.getUserId(), "LOGIN_FAILED", "AUTH_SERVICE", 
                           ipAddress, userAgent, false, "Invalid credentials");
            }
            return new AuthenticationResult(false, "Invalid credentials", null, null);
        }
        
        // Check if account is locked
        if (user.isLocked()) {
            logAuditEvent(user.getUserId(), "LOGIN_BLOCKED", "AUTH_SERVICE", 
                       ipAddress, userAgent, false, "Account locked");
            return new AuthenticationResult(false, "Account locked due to too many failed attempts", 
                                          null, null);
        }
        
        // Successful authentication
        user.recordSuccessfulLogin();
        clearFailedAttempts(ipAddress);
        
        // Create session
        UserSession session = createSession(user.getUserId(), deviceId, ipAddress, userAgent);
        
        // Generate JWT token
        JwtToken token = generateJwtToken(user);
        
        logAuditEvent(user.getUserId(), "LOGIN_SUCCESS", "AUTH_SERVICE", 
                   ipAddress, userAgent, true, "Successful login");
        
        return new AuthenticationResult(true, "Authentication successful", session, token);
    }
    
    // Authorization methods
    public boolean hasPermission(String tokenId, Permission permission) {
        JwtToken token = activeTokens.get(tokenId);
        if (token == null || !token.isValid()) {
            return false;
        }
        
        User user = users.get(token.getUserId());
        if (user == null) {
            return false;
        }
        
        return user.getRoles().stream()
            .anyMatch(role -> role.hasPermission(permission));
    }
    
    public boolean hasRole(String tokenId, String roleName) {
        JwtToken token = activeTokens.get(tokenId);
        if (token == null || !token.isValid()) {
            return false;
        }
        
        return token.getRoles().contains(roleName);
    }
    
    // ABAC (Attribute-Based Access Control)
    public boolean checkAttributeAccess(String tokenId, String resource, String action) {
        JwtToken token = activeTokens.get(tokenId);
        if (token == null || !token.isValid()) {
            return false;
        }
        
        User user = users.get(token.getUserId());
        if (user == null) {
            return false;
        }
        
        // Example ABAC rules
        String userRegion = (String) user.getAttributes().get("region");
        String userDepartment = (String) user.getAttributes().get("department");
        
        // Regional content access
        if (resource.startsWith("content/") && action.equals("read")) {
            String contentRegion = extractRegionFromResource(resource);
            return userRegion.equals(contentRegion) || userDepartment.equals("IT");
        }
        
        // Department-specific access
        if (resource.startsWith("analytics/") && userDepartment != null) {
            return userDepartment.equals("IT") || userDepartment.equals("Marketing");
        }
        
        return false;
    }
    
    // Session management
    private UserSession createSession(String userId, String deviceId, 
                                    String ipAddress, String userAgent) {
        // Limit sessions per user
        long userSessions = activeSessions.values().stream()
            .filter(s -> s.getUserId().equals(userId) && s.isActive())
            .count();
        
        if (userSessions >= maxSessionsPerUser) {
            // Remove oldest session
            activeSessions.values().stream()
                .filter(s -> s.getUserId().equals(userId) && s.isActive())
                .min(Comparator.comparing(UserSession::getCreatedAt))
                .ifPresent(UserSession::invalidate);
        }
        
        String sessionId = generateSecureId();
        UserSession session = new UserSession(sessionId, userId, deviceId, ipAddress, userAgent);
        activeSessions.put(sessionId, session);
        
        return session;
    }
    
    public boolean validateSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session == null || !session.isActive() || session.isExpired(sessionTimeout)) {
            if (session != null) {
                session.invalidate();
            }
            return false;
        }
        
        session.updateAccess();
        return true;
    }
    
    // JWT token methods
    private JwtToken generateJwtToken(User user) {
        String tokenId = generateSecureId();
        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(java.util.stream.Collectors.toSet());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("department", user.getAttributes().get("department"));
        claims.put("region", user.getAttributes().get("region"));
        
        String signature = generateSignature(tokenId, user.getUserId(), claims);
        
        JwtToken token = new JwtToken(tokenId, user.getUserId(), roleNames, 
                                     claims, tokenValidity, signature);
        activeTokens.put(tokenId, token);
        
        return token;
    }
    
    // Utility methods
    private boolean isRateLimited(String ipAddress) {
        AtomicInteger attempts = loginAttempts.get(ipAddress);
        Instant lastAttempt = lastAttemptTime.get(ipAddress);
        
        if (attempts == null || lastAttempt == null) {
            return false;
        }
        
        // Reset if more than 15 minutes passed
        if (Duration.between(lastAttempt, Instant.now()).toMinutes() > 15) {
            loginAttempts.remove(ipAddress);
            lastAttemptTime.remove(ipAddress);
            return false;
        }
        
        return attempts.get() >= 10; // Max 10 attempts per 15 minutes
    }
    
    private void recordFailedAttempt(String ipAddress) {
        loginAttempts.computeIfAbsent(ipAddress, k -> new AtomicInteger(0)).incrementAndGet();
        lastAttemptTime.put(ipAddress, Instant.now());
    }
    
    private void clearFailedAttempts(String ipAddress) {
        loginAttempts.remove(ipAddress);
        lastAttemptTime.remove(ipAddress);
    }
    
    private String hashPassword(String password) {
        // In real implementation, use BCrypt or Argon2
        return "hashed_" + password;
    }
    
    private boolean verifyPassword(String password, String hashedPassword) {
        return hashedPassword.equals("hashed_" + password);
    }
    
    private String generateSecureId() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    private String generateSignature(String tokenId, String userId, Map<String, Object> claims) {
        // Simplified HMAC signature
        try {
            String payload = tokenId + userId + claims.toString();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
        }
    }
    
    private String extractRegionFromResource(String resource) {
        // Extract region from resource path like "content/us/movie123"
        String[] parts = resource.split("/");
        return parts.length > 1 ? parts[1] : "unknown";
    }
    
    private void logAuditEvent(String userId, String action, String resource, 
                              String ipAddress, String userAgent, boolean success, String details) {
        AuditLogEntry entry = new AuditLogEntry(userId, action, resource, 
                                              ipAddress, userAgent, success, details);
        auditLog.add(entry);
        System.out.println("AUDIT: " + entry);
    }
    
    private void startCleanupTasks() {
        // Clean expired sessions every 30 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredSessions, 
                                          30, 30, TimeUnit.MINUTES);
        
        // Clean expired tokens every 15 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredTokens, 
                                          15, 15, TimeUnit.MINUTES);
    }
    
    private void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> {
            UserSession session = entry.getValue();
            if (!session.isActive() || session.isExpired(sessionTimeout)) {
                System.out.println("Cleaning up expired session: " + session.getSessionId());
                return true;
            }
            return false;
        });
    }
    
    private void cleanupExpiredTokens() {
        activeTokens.entrySet().removeIf(entry -> {
            JwtToken token = entry.getValue();
            if (token.isExpired()) {
                System.out.println("Cleaning up expired token: " + token.getTokenId());
                return true;
            }
            return false;
        });
    }
    
    // Logout
    public void logout(String sessionId, String tokenId) {
        UserSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.invalidate();
            logAuditEvent(session.getUserId(), "LOGOUT", "AUTH_SERVICE", 
                       session.getIpAddress(), session.getUserAgent(), true, "User logged out");
        }
        
        activeTokens.remove(tokenId);
    }
    
    // Statistics
    public void printStatistics() {
        System.out.println("\n=== Authentication Service Statistics ===");
        System.out.println("Total users: " + users.size());
        System.out.println("Active sessions: " + activeSessions.values().stream()
                          .mapToInt(s -> s.isActive() ? 1 : 0).sum());
        System.out.println("Active tokens: " + activeTokens.size());
        System.out.println("Audit log entries: " + auditLog.size());
        
        // Failed login statistics
        long failedLogins = auditLog.stream()
            .filter(entry -> "LOGIN_FAILED".equals(entry.getAction()))
            .count();
        System.out.println("Failed login attempts: " + failedLogins);
    }
}

// Authentication result
class AuthenticationResult {
    private final boolean success;
    private final String message;
    private final UserSession session;
    private final JwtToken token;
    
    public AuthenticationResult(boolean success, String message, 
                              UserSession session, JwtToken token) {
        this.success = success;
        this.message = message;
        this.session = session;
        this.token = token;
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public UserSession getSession() { return session; }
    public JwtToken getToken() { return token; }
}

public class AdvancedAuthenticationExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Advanced Authentication & Authorization Demo ===\n");
        
        AdvancedAuthenticationService authService = new AdvancedAuthenticationService();
        
        // Simulate authentication scenarios
        demonstrateAuthentication(authService);
        demonstrateAuthorization(authService);
        demonstrateRateLimiting(authService);
        
        Thread.sleep(2000);
        authService.printStatistics();
    }
    
    private static void demonstrateAuthentication(AdvancedAuthenticationService authService) {
        System.out.println("=== Authentication Demo ===");
        
        // Successful login
        AuthenticationResult result = authService.authenticate(
            "admin@company.com", "admin123", "192.168.1.100", 
            "Mozilla/5.0", "device-123"
        );
        System.out.println("Admin login: " + result.getMessage());
        
        // Failed login
        result = authService.authenticate(
            "admin@company.com", "wrongpassword", "192.168.1.100", 
            "Mozilla/5.0", "device-123"
        );
        System.out.println("Wrong password: " + result.getMessage());
        
        // User login
        result = authService.authenticate(
            "user1@company.com", "user123", "192.168.1.101", 
            "Chrome/95.0", "device-456"
        );
        System.out.println("User login: " + result.getMessage());
        
        System.out.println();
    }
    
    private static void demonstrateAuthorization(AdvancedAuthenticationService authService) {
        System.out.println("=== Authorization Demo ===");
        
        // Get admin token
        AuthenticationResult adminResult = authService.authenticate(
            "admin@company.com", "admin123", "192.168.1.100", 
            "Mozilla/5.0", "device-123"
        );
        
        if (adminResult.isSuccess()) {
            String tokenId = adminResult.getToken().getTokenId();
            
            // Test permissions
            System.out.println("Admin can read content: " + 
                authService.hasPermission(tokenId, Permission.CONTENT_READ));
            System.out.println("Admin can write content: " + 
                authService.hasPermission(tokenId, Permission.CONTENT_WRITE));
            System.out.println("Admin has ADMIN role: " + 
                authService.hasRole(tokenId, "ADMIN"));
            
            // Test ABAC
            System.out.println("Admin can access US content: " + 
                authService.checkAttributeAccess(tokenId, "content/us/movie123", "read"));
            System.out.println("Admin can access analytics: " + 
                authService.checkAttributeAccess(tokenId, "analytics/dashboard", "read"));
        }
        
        System.out.println();
    }
    
    private static void demonstrateRateLimiting(AdvancedAuthenticationService authService) {
        System.out.println("=== Rate Limiting Demo ===");
        
        String suspiciousIp = "192.168.1.999";
        
        // Simulate multiple failed login attempts
        for (int i = 0; i < 12; i++) {
            AuthenticationResult result = authService.authenticate(
                "fake@email.com", "fakepassword", suspiciousIp, 
                "Suspicious-Agent", "fake-device"
            );
            
            if (i == 5 || i == 10) {
                System.out.println("Attempt " + (i + 1) + ": " + result.getMessage());
            }
        }
        
        System.out.println();
    }
}
