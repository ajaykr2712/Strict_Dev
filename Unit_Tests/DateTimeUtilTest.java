package unittests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Unit tests for DateTime Utilities
 * Tests date/time parsing, formatting, and manipulation
 */
public class DateTimeUtilTest {

    @Test
    public void testFormatDateTime_StandardFormat() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        // Act
        String result = DateTimeUtil.formatDateTime(dateTime, "yyyy-MM-dd HH:mm:ss");

        // Assert
        assertEquals("2024-01-15 10:30:00", result);
    }

    @Test
    public void testFormatDateTime_CustomFormat() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 25, 15, 45, 30);

        // Act
        String result = DateTimeUtil.formatDateTime(dateTime, "dd/MM/yyyy HH:mm");

        // Assert
        assertEquals("25/12/2024 15:45", result);
    }

    @Test
    public void testParseDateTime_ValidString() {
        // Arrange
        String dateTimeStr = "2024-01-15 10:30:00";

        // Act
        LocalDateTime result = DateTimeUtil.parseDateTime(dateTimeStr, "yyyy-MM-dd HH:mm:ss");

        // Assert
        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(10, result.getHour());
        assertEquals(30, result.getMinute());
    }

    @Test(expected = Exception.class)
    public void testParseDateTime_InvalidString_ThrowsException() {
        // Act
        DateTimeUtil.parseDateTime("invalid", "yyyy-MM-dd");
    }

    @Test
    public void testAddDays_PositiveDays() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 0, 0);

        // Act
        LocalDateTime result = DateTimeUtil.addDays(dateTime, 10);

        // Assert
        assertEquals(11, result.getDayOfMonth());
    }

    @Test
    public void testAddDays_NegativeDays() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 0, 0);

        // Act
        LocalDateTime result = DateTimeUtil.addDays(dateTime, -5);

        // Assert
        assertEquals(10, result.getDayOfMonth());
    }

    @Test
    public void testIsBefore_EarlierDate_ReturnsTrue() {
        // Arrange
        LocalDateTime earlier = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime later = LocalDateTime.of(2024, 12, 31, 0, 0);

        // Act & Assert
        assertTrue(DateTimeUtil.isBefore(earlier, later));
    }

    @Test
    public void testIsBefore_LaterDate_ReturnsFalse() {
        // Arrange
        LocalDateTime earlier = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime later = LocalDateTime.of(2024, 12, 31, 0, 0);

        // Act & Assert
        assertFalse(DateTimeUtil.isBefore(later, earlier));
    }

    @Test
    public void testIsAfter_LaterDate_ReturnsTrue() {
        // Arrange
        LocalDateTime earlier = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime later = LocalDateTime.of(2024, 12, 31, 0, 0);

        // Act & Assert
        assertTrue(DateTimeUtil.isAfter(later, earlier));
    }

    @Test
    public void testGetDaysBetween() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 11, 0, 0);

        // Act
        long days = DateTimeUtil.getDaysBetween(start, end);

        // Assert
        assertEquals(10, days);
    }

    @Test
    public void testIsWeekend_Saturday_ReturnsTrue() {
        // Arrange - 2024-01-06 is a Saturday
        LocalDateTime saturday = LocalDateTime.of(2024, 1, 6, 10, 0);

        // Act & Assert
        assertTrue(DateTimeUtil.isWeekend(saturday));
    }

    @Test
    public void testIsWeekend_Monday_ReturnsFalse() {
        // Arrange - 2024-01-01 is a Monday
        LocalDateTime monday = LocalDateTime.of(2024, 1, 1, 10, 0);

        // Act & Assert
        assertFalse(DateTimeUtil.isWeekend(monday));
    }

    // DateTime utility implementation
    static class DateTimeUtil {
        
        public static String formatDateTime(LocalDateTime dateTime, String pattern) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return dateTime.format(formatter);
        }

        public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDateTime.parse(dateTimeStr, formatter);
        }

        public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
            return dateTime.plusDays(days);
        }

        public static boolean isBefore(LocalDateTime dateTime1, LocalDateTime dateTime2) {
            return dateTime1.isBefore(dateTime2);
        }

        public static boolean isAfter(LocalDateTime dateTime1, LocalDateTime dateTime2) {
            return dateTime1.isAfter(dateTime2);
        }

        public static long getDaysBetween(LocalDateTime start, LocalDateTime end) {
            return java.time.temporal.ChronoUnit.DAYS.between(start, end);
        }

        public static boolean isWeekend(LocalDateTime dateTime) {
            java.time.DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
            return dayOfWeek == java.time.DayOfWeek.SATURDAY || 
                   dayOfWeek == java.time.DayOfWeek.SUNDAY;
        }
    }
}
