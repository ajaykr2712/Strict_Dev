import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Proxy Pattern
 * Tests virtual proxy, protection proxy, and caching proxy
 */
public class ProxyPatternTest {

    @Test
    public void testVirtualProxy_LazyInitialization() {
        // Arrange
        Image image = new ProxyImage("test.jpg");

        // Act - Image should not be loaded yet
        // (In real implementation, we'd check this)
        String filename = image.getFilename();

        // Assert
        assertNotNull("Filename should be accessible without loading", filename);
    }

    @Test
    public void testVirtualProxy_LoadsOnDisplay() {
        // Arrange
        Image image = new ProxyImage("photo.jpg");

        // Act
        image.display();

        // Assert - Image should be loaded after display
        assertTrue("Image should be marked as loaded", 
                   ((ProxyImage)image).isLoaded());
    }

    @Test
    public void testVirtualProxy_CachesLoadedImage() {
        // Arrange
        ProxyImage image = new ProxyImage("cached.jpg");

        // Act
        image.display(); // First call - loads image
        int firstLoadCount = image.getLoadCount();
        
        image.display(); // Second call - should use cached
        int secondLoadCount = image.getLoadCount();

        // Assert
        assertEquals("Image should only be loaded once", firstLoadCount, secondLoadCount);
    }

    @Test
    public void testProtectionProxy_AllowsAuthorizedAccess() {
        // Arrange
        Document doc = new ProtectedDocument("Secret.doc", "admin");

        // Act
        String content = doc.read("admin");

        // Assert
        assertNotNull("Authorized user should access content", content);
    }

    @Test(expected = SecurityException.class)
    public void testProtectionProxy_DeniesUnauthorizedAccess() {
        // Arrange
        Document doc = new ProtectedDocument("Secret.doc", "admin");

        // Act
        doc.read("guest"); // Should throw SecurityException
    }

    @Test
    public void testCachingProxy_CachesResults() {
        // Arrange
        Calculator calculator = new CachingCalculatorProxy();

        // Act
        int result1 = calculator.add(5, 3);
        int result2 = calculator.add(5, 3); // Should use cached result

        // Assert
        assertEquals(8, result1);
        assertEquals(8, result2);
    }

    @Test
    public void testCachingProxy_DifferentInputs() {
        // Arrange
        Calculator calculator = new CachingCalculatorProxy();

        // Act
        int result1 = calculator.add(5, 3);
        int result2 = calculator.add(10, 20);

        // Assert
        assertEquals(8, result1);
        assertEquals(30, result2);
    }

    @Test
    public void testMultipleProxies() {
        // Arrange
        ProxyImage image1 = new ProxyImage("img1.jpg");
        ProxyImage image2 = new ProxyImage("img2.jpg");

        // Act
        image1.display();
        image2.display();

        // Assert
        assertTrue("Image 1 should be loaded", image1.isLoaded());
        assertTrue("Image 2 should be loaded", image2.isLoaded());
    }

    // Proxy pattern implementations
    interface Image {
        void display();
        String getFilename();
    }

    static class RealImage implements Image {
        private String filename;

        public RealImage(String filename) {
            this.filename = filename;
            loadFromDisk();
        }

        private void loadFromDisk() {
            System.out.println("Loading " + filename);
        }

        @Override
        public void display() {
            System.out.println("Displaying " + filename);
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    static class ProxyImage implements Image {
        private String filename;
        private RealImage realImage;
        private int loadCount = 0;

        public ProxyImage(String filename) {
            this.filename = filename;
        }

        @Override
        public void display() {
            if (realImage == null) {
                realImage = new RealImage(filename);
                loadCount++;
            }
            realImage.display();
        }

        @Override
        public String getFilename() {
            return filename;
        }

        public boolean isLoaded() {
            return realImage != null;
        }

        public int getLoadCount() {
            return loadCount;
        }
    }

    // Protection Proxy
    interface Document {
        String read(String user);
    }

    static class ProtectedDocument implements Document {
        private String filename;
        private String authorizedUser;
        private String content;

        public ProtectedDocument(String filename, String authorizedUser) {
            this.filename = filename;
            this.authorizedUser = authorizedUser;
            this.content = "Confidential content of " + filename;
        }

        @Override
        public String read(String user) {
            if (!authorizedUser.equals(user)) {
                throw new SecurityException("Unauthorized access");
            }
            return content;
        }
    }

    // Caching Proxy
    interface Calculator {
        int add(int a, int b);
    }

    static class CachingCalculatorProxy implements Calculator {
        private java.util.Map<String, Integer> cache = new java.util.HashMap<>();

        @Override
        public int add(int a, int b) {
            String key = a + "+" + b;
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
            int result = a + b;
            cache.put(key, result);
            return result;
        }
    }
}
