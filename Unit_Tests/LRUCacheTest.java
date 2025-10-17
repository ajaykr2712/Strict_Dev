import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for LRUCache
 * Tests cache operations including get, put, eviction, and capacity management
 */
public class LRUCacheTest {

    private LRUCache cache;

    @Before
    public void setUp() {
        cache = new LRUCache(3); // Capacity of 3
    }

    @Test
    public void testPut_AndGet_SingleItem() {
        // Arrange & Act
        cache.put(1, 100);
        int result = cache.get(1);

        // Assert
        assertEquals("Should retrieve the value that was put", 100, result);
    }

    @Test
    public void testGet_NonExistentKey_ReturnsMinusOne() {
        // Act
        int result = cache.get(999);

        // Assert
        assertEquals("Should return -1 for non-existent key", -1, result);
    }

    @Test
    public void testPut_UpdateExistingKey() {
        // Arrange
        cache.put(1, 100);
        
        // Act
        cache.put(1, 200);
        int result = cache.get(1);

        // Assert
        assertEquals("Should return updated value", 200, result);
    }

    @Test
    public void testEviction_WhenCapacityExceeded() {
        // Arrange
        cache.put(1, 100);
        cache.put(2, 200);
        cache.put(3, 300);
        
        // Act - This should evict key 1 (least recently used)
        cache.put(4, 400);

        // Assert
        assertEquals("Key 1 should be evicted", -1, cache.get(1));
        assertEquals("Key 2 should still exist", 200, cache.get(2));
        assertEquals("Key 3 should still exist", 300, cache.get(3));
        assertEquals("Key 4 should be added", 400, cache.get(4));
    }

    @Test
    public void testGet_UpdatesRecentlyUsed() {
        // Arrange
        cache.put(1, 100);
        cache.put(2, 200);
        cache.put(3, 300);
        
        // Act - Access key 1, making it recently used
        cache.get(1);
        
        // Now add key 4, which should evict key 2 (now LRU)
        cache.put(4, 400);

        // Assert
        assertEquals("Key 1 should still exist (was accessed)", 100, cache.get(1));
        assertEquals("Key 2 should be evicted", -1, cache.get(2));
        assertEquals("Key 3 should still exist", 300, cache.get(3));
        assertEquals("Key 4 should be added", 400, cache.get(4));
    }

    @Test
    public void testPut_ExistingKey_MovesToFront() {
        // Arrange
        cache.put(1, 100);
        cache.put(2, 200);
        cache.put(3, 300);
        
        // Act - Update key 1, making it most recently used
        cache.put(1, 150);
        
        // Add key 4, should evict key 2
        cache.put(4, 400);

        // Assert
        assertEquals("Key 1 should still exist with updated value", 150, cache.get(1));
        assertEquals("Key 2 should be evicted", -1, cache.get(2));
    }

    @Test
    public void testCapacity_One() {
        // Arrange
        LRUCache smallCache = new LRUCache(1);
        
        // Act
        smallCache.put(1, 100);
        smallCache.put(2, 200);

        // Assert
        assertEquals("Only key 2 should exist", 200, smallCache.get(2));
        assertEquals("Key 1 should be evicted", -1, smallCache.get(1));
    }

    @Test
    public void testSequentialOperations() {
        // Arrange
        cache.put(1, 10);
        cache.put(2, 20);
        
        // Act
        assertEquals(10, cache.get(1));
        cache.put(3, 30);
        assertEquals(-1, cache.get(2)); // Should not be evicted yet
        cache.put(4, 40);

        // Assert
        assertEquals("Key 2 should be evicted", -1, cache.get(2));
        assertEquals("Key 1 should still exist", 10, cache.get(1));
        assertEquals("Key 3 should still exist", 30, cache.get(3));
        assertEquals("Key 4 should exist", 40, cache.get(4));
    }

    @Test
    public void testMultipleGets_DoNotCauseEviction() {
        // Arrange
        cache.put(1, 100);
        cache.put(2, 200);
        cache.put(3, 300);

        // Act - Multiple gets shouldn't change eviction order
        cache.get(1);
        cache.get(1);
        cache.get(1);

        // Assert - All keys should still exist
        assertEquals(100, cache.get(1));
        assertEquals(200, cache.get(2));
        assertEquals(300, cache.get(3));
    }

    @Test
    public void testZeroValues() {
        // Arrange & Act
        cache.put(1, 0);

        // Assert
        assertEquals("Should correctly store and retrieve zero", 0, cache.get(1));
    }

    @Test
    public void testNegativeValues() {
        // Arrange & Act
        cache.put(1, -100);

        // Assert
        assertEquals("Should correctly store and retrieve negative values", -100, cache.get(1));
    }
}
