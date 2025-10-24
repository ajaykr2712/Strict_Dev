package unittests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for IdempotencyKeyMiddleware
 * Tests idempotent request handling and concurrent request deduplication
 */
public class IdempotencyKeyMiddlewareTest {

    private IdempotencyKeyMiddlewareExample.OrderService orderService;

    @Before
    public void setUp() {
        orderService = new IdempotencyKeyMiddlewareExample.OrderService();
    }

    @Test
    public void testHandleCreate_WithValidKey_Success() throws Exception {
        // Arrange
        String idempotencyKey = "test-key-123";
        String payload = "{}";

        // Act
        IdempotencyKeyMiddlewareExample.CachedResponse result = 
                orderService.handleCreate(idempotencyKey, payload);

        // Assert
        assertNotNull("Response should not be null", result);
        assertEquals("Should return 201 status", 201, result.status);
        assertNotNull("Response body should not be null", result.body);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandleCreate_WithNullKey_ThrowsException() throws Exception {
        // Arrange
        String payload = "{}";

        // Act
        orderService.handleCreate(null, payload);

        // Assert - Exception expected
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandleCreate_WithBlankKey_ThrowsException() throws Exception {
        // Arrange
        String payload = "{}";

        // Act
        orderService.handleCreate("", payload);

        // Assert - Exception expected
    }

    @Test
    public void testIdempotency_SameKeyReturnsSameResponse() throws Exception {
        // Arrange
        String idempotencyKey = "same-key-456";
        String payload = "{}";

        // Act
        IdempotencyKeyMiddlewareExample.CachedResponse response1 = 
                orderService.handleCreate(idempotencyKey, payload);
        IdempotencyKeyMiddlewareExample.CachedResponse response2 = 
                orderService.handleCreate(idempotencyKey, payload);

        // Assert
        assertNotNull("First response should not be null", response1);
        assertNotNull("Second response should not be null", response2);
        assertEquals("Both responses should have same status", 
                     response1.status, response2.status);
        assertEquals("Both responses should have same body", 
                     response1.body, response2.body);
    }

    @Test
    public void testConcurrentRequests_SameKey_ReturnSameResponse() throws Exception {
        // Arrange
        String idempotencyKey = "concurrent-key-789";
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<IdempotencyKeyMiddlewareExample.CachedResponse>> futures = new ArrayList<>();

        // Act - Submit 5 concurrent requests with same idempotency key
        for (int i = 0; i < 5; i++) {
            futures.add(executor.submit(() -> orderService.handleCreate(idempotencyKey, "{}")));
        }

        // Wait for all to complete
        List<IdempotencyKeyMiddlewareExample.CachedResponse> responses = new ArrayList<>();
        for (Future<IdempotencyKeyMiddlewareExample.CachedResponse> future : futures) {
            responses.add(future.get());
        }
        executor.shutdown();

        // Assert
        assertEquals("Should have 5 responses", 5, responses.size());
        
        // All responses should have the same body (same order ID)
        String firstBody = responses.get(0).body;
        for (IdempotencyKeyMiddlewareExample.CachedResponse response : responses) {
            assertEquals("All responses should have same body", firstBody, response.body);
            assertEquals("All responses should have 201 status", 201, response.status);
        }
    }

    @Test
    public void testDifferentKeys_GenerateDifferentResponses() throws Exception {
        // Arrange
        String key1 = "key-one";
        String key2 = "key-two";

        // Act
        IdempotencyKeyMiddlewareExample.CachedResponse response1 = 
                orderService.handleCreate(key1, "{}");
        IdempotencyKeyMiddlewareExample.CachedResponse response2 = 
                orderService.handleCreate(key2, "{}");

        // Assert
        assertNotNull("First response should not be null", response1);
        assertNotNull("Second response should not be null", response2);
        assertNotEquals("Different keys should produce different order IDs",
                        response1.body, response2.body);
    }

    @Test
    public void testCachedResponse_HasTimestamp() throws Exception {
        // Arrange
        String idempotencyKey = "timestamp-test";

        // Act
        IdempotencyKeyMiddlewareExample.CachedResponse response = 
                orderService.handleCreate(idempotencyKey, "{}");

        // Assert
        assertNotNull("Timestamp should not be null", response.ts);
    }

    @Test
    public void testResponseStatus_Is201Created() throws Exception {
        // Arrange
        String idempotencyKey = "status-test";

        // Act
        IdempotencyKeyMiddlewareExample.CachedResponse response = 
                orderService.handleCreate(idempotencyKey, "{}");

        // Assert
        assertEquals("Status should be 201 Created", 201, response.status);
    }

    @Test
    public void testResponseBody_ContainsOrderId() throws Exception {
        // Arrange
        String idempotencyKey = "order-id-test";

        // Act
        IdempotencyKeyMiddlewareExample.CachedResponse response = 
                orderService.handleCreate(idempotencyKey, "{}");

        // Assert
        assertTrue("Response body should contain orderId", 
                   response.body.contains("orderId"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandleCreate_WithWhitespaceKey_ThrowsException() throws Exception {
        // Act
        orderService.handleCreate("   ", "{}");

        // Assert - Exception expected
    }
}
