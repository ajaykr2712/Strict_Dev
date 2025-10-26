package unittests;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for String Utilities
 * Tests string manipulation, conversion, and formatting operations
 */
public class StringUtilTest {

    @Test
    public void testCapitalize_LowercaseString() {
        assertEquals("Hello", StringUtil.capitalize("hello"));
    }

    @Test
    public void testCapitalize_UppercaseString() {
        assertEquals("HELLO", StringUtil.capitalize("HELLO"));
    }

    @Test
    public void testCapitalize_EmptyString() {
        assertEquals("", StringUtil.capitalize(""));
    }

    @Test
    public void testReverse_NormalString() {
        assertEquals("olleh", StringUtil.reverse("hello"));
    }

    @Test
    public void testReverse_Palindrome() {
        assertEquals("racecar", StringUtil.reverse("racecar"));
    }

    @Test
    public void testIsPalindrome_ValidPalindrome() {
        assertTrue(StringUtil.isPalindrome("racecar"));
        assertTrue(StringUtil.isPalindrome("A man a plan a canal Panama"));
    }

    @Test
    public void testIsPalindrome_NotPalindrome() {
        assertFalse(StringUtil.isPalindrome("hello"));
        assertFalse(StringUtil.isPalindrome("world"));
    }

    @Test
    public void testTruncate_LongString() {
        assertEquals("Hello...", StringUtil.truncate("Hello World", 8));
    }

    @Test
    public void testTruncate_ShortString() {
        assertEquals("Hi", StringUtil.truncate("Hi", 10));
    }

    @Test
    public void testCountOccurrences() {
        assertEquals(3, StringUtil.countOccurrences("hello world hello", "hello"));
    }

    @Test
    public void testCountOccurrences_NotFound() {
        assertEquals(0, StringUtil.countOccurrences("hello world", "xyz"));
    }

    @Test
    public void testRemoveWhitespace() {
        assertEquals("helloworld", StringUtil.removeWhitespace("hello world"));
        assertEquals("test", StringUtil.removeWhitespace("  test  "));
    }

    @Test
    public void testCamelToSnakeCase() {
        assertEquals("hello_world", StringUtil.camelToSnakeCase("helloWorld"));
        assertEquals("my_variable_name", StringUtil.camelToSnakeCase("myVariableName"));
    }

    @Test
    public void testSnakeToCamelCase() {
        assertEquals("helloWorld", StringUtil.snakeToCamelCase("hello_world"));
        assertEquals("myVariableName", StringUtil.snakeToCamelCase("my_variable_name"));
    }

    @Test
    public void testIsBlank_BlankStrings() {
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank("   "));
        assertTrue(StringUtil.isBlank(null));
    }

    @Test
    public void testIsBlank_NonBlankStrings() {
        assertFalse(StringUtil.isBlank("hello"));
        assertFalse(StringUtil.isBlank(" a "));
    }

    @Test
    public void testRepeat() {
        assertEquals("aaaa", StringUtil.repeat("a", 4));
        assertEquals("hohoho", StringUtil.repeat("ho", 3));
    }

    @Test
    public void testContainsIgnoreCase() {
        assertTrue(StringUtil.containsIgnoreCase("Hello World", "WORLD"));
        assertTrue(StringUtil.containsIgnoreCase("Test String", "test"));
    }

    @Test
    public void testContainsIgnoreCase_NotFound() {
        assertFalse(StringUtil.containsIgnoreCase("Hello World", "xyz"));
    }

    // String utility implementation
    static class StringUtil {
        
        public static String capitalize(String str) {
            if (str == null || str.isEmpty()) return str;
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }

        public static String reverse(String str) {
            if (str == null) return null;
            return new StringBuilder(str).reverse().toString();
        }

        public static boolean isPalindrome(String str) {
            if (str == null) return false;
            String cleaned = str.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            return cleaned.equals(reverse(cleaned));
        }

        public static String truncate(String str, int maxLength) {
            if (str == null || str.length() <= maxLength) return str;
            return str.substring(0, maxLength - 3) + "...";
        }

        public static int countOccurrences(String str, String substring) {
            if (str == null || substring == null || substring.isEmpty()) return 0;
            int count = 0;
            int index = 0;
            while ((index = str.indexOf(substring, index)) != -1) {
                count++;
                index += substring.length();
            }
            return count;
        }

        public static String removeWhitespace(String str) {
            if (str == null) return null;
            return str.replaceAll("\\s+", "");
        }

        public static String camelToSnakeCase(String str) {
            if (str == null) return null;
            return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        }

        public static String snakeToCamelCase(String str) {
            if (str == null) return null;
            StringBuilder result = new StringBuilder();
            boolean nextUpper = false;
            for (char c : str.toCharArray()) {
                if (c == '_') {
                    nextUpper = true;
                } else {
                    result.append(nextUpper ? Character.toUpperCase(c) : c);
                    nextUpper = false;
                }
            }
            return result.toString();
        }

        public static boolean isBlank(String str) {
            return str == null || str.trim().isEmpty();
        }

        public static String repeat(String str, int times) {
            if (str == null || times <= 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < times; i++) {
                sb.append(str);
            }
            return sb.toString();
        }

        public static boolean containsIgnoreCase(String str, String substring) {
            if (str == null || substring == null) return false;
            return str.toLowerCase().contains(substring.toLowerCase());
        }
    }
}
