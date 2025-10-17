import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Adapter Pattern Implementation
 * Tests legacy system adaptation and interface conversion
 */
public class AdapterPatternTest {

    @Test
    public void testAdapter_ConvertsLegacyToNewInterface() {
        // Arrange
        LegacyPrinter legacyPrinter = new LegacyPrinter();
        ModernPrinter adapter = new PrinterAdapter(legacyPrinter);

        // Act
        String result = adapter.printDocument("Test Document");

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Should contain legacy format", result.contains("LEGACY"));
    }

    @Test
    public void testLegacyPrinter_PrintsInOldFormat() {
        // Arrange
        LegacyPrinter printer = new LegacyPrinter();

        // Act
        String result = printer.printInOldFormat("Document");

        // Assert
        assertEquals("LEGACY: Document", result);
    }

    @Test
    public void testAdapter_EmptyDocument() {
        // Arrange
        LegacyPrinter legacyPrinter = new LegacyPrinter();
        ModernPrinter adapter = new PrinterAdapter(legacyPrinter);

        // Act
        String result = adapter.printDocument("");

        // Assert
        assertEquals("LEGACY: ", result);
    }

    @Test
    public void testAdapter_NullDocument_HandlesGracefully() {
        // Arrange
        LegacyPrinter legacyPrinter = new LegacyPrinter();
        ModernPrinter adapter = new PrinterAdapter(legacyPrinter);

        // Act
        String result = adapter.printDocument(null);

        // Assert
        assertNotNull("Should handle null gracefully", result);
    }

    @Test
    public void testAdapter_MultipleCalls() {
        // Arrange
        LegacyPrinter legacyPrinter = new LegacyPrinter();
        ModernPrinter adapter = new PrinterAdapter(legacyPrinter);

        // Act
        String result1 = adapter.printDocument("Doc1");
        String result2 = adapter.printDocument("Doc2");

        // Assert
        assertEquals("LEGACY: Doc1", result1);
        assertEquals("LEGACY: Doc2", result2);
    }

    // Test interfaces and implementations
    interface ModernPrinter {
        String printDocument(String document);
    }

    static class LegacyPrinter {
        public String printInOldFormat(String text) {
            return "LEGACY: " + text;
        }
    }

    static class PrinterAdapter implements ModernPrinter {
        private LegacyPrinter legacyPrinter;

        public PrinterAdapter(LegacyPrinter legacyPrinter) {
            this.legacyPrinter = legacyPrinter;
        }

        @Override
        public String printDocument(String document) {
            if (document == null) {
                return legacyPrinter.printInOldFormat("");
            }
            return legacyPrinter.printInOldFormat(document);
        }
    }
}
