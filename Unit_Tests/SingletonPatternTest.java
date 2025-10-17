import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Singleton Pattern Implementation
 * Tests singleton instance creation, thread safety, and uniqueness
 */
public class SingletonPatternTest {

    @Test
    public void testGetInstance_ReturnsSameInstance() {
        // Act
        Object instance1 = getSingletonInstance();
        Object instance2 = getSingletonInstance();

        // Assert
        assertSame("getInstance should return the same instance", instance1, instance2);
    }

    @Test
    public void testSingleton_IsNotNull() {
        // Act
        Object instance = getSingletonInstance();

        // Assert
        assertNotNull("Singleton instance should not be null", instance);
    }

    @Test
    public void testMultipleCalls_ReturnSameHashCode() {
        // Act
        Object instance1 = getSingletonInstance();
        Object instance2 = getSingletonInstance();

        // Assert
        assertEquals("Both instances should have same hash code",
                     instance1.hashCode(), instance2.hashCode());
    }

    @Test
    public void testThreadSafety_ConcurrentAccess() throws InterruptedException {
        // Arrange
        final Object[] instances = new Object[10];
        Thread[] threads = new Thread[10];

        // Act - Create multiple threads that access singleton
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> instances[index] = getSingletonInstance());
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert - All instances should be the same
        Object firstInstance = instances[0];
        for (int i = 1; i < instances.length; i++) {
            assertSame("All thread instances should be the same", 
                       firstInstance, instances[i]);
        }
    }

    // Helper method to simulate getting singleton instance
    private Object getSingletonInstance() {
        // This would typically call your Singleton.getInstance()
        // For testing purposes, we'll use a simple implementation
        return SimpleSingleton.getInstance();
    }

    // Simple Singleton implementation for testing
    static class SimpleSingleton {
        private static volatile SimpleSingleton instance;

        private SimpleSingleton() {}

        public static SimpleSingleton getInstance() {
            if (instance == null) {
                synchronized (SimpleSingleton.class) {
                    if (instance == null) {
                        instance = new SimpleSingleton();
                    }
                }
            }
            return instance;
        }
    }
}
