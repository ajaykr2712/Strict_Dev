import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for Validation Utilities
 * Tests email, phone, URL, and general input validation
 */
public class ValidationUtilTest {

    @Test
    public void testIsValidEmail_ValidEmail_ReturnsTrue() {
        assertTrue(ValidationUtil.isValidEmail("test@example.com"));
        assertTrue(ValidationUtil.isValidEmail("user.name@domain.co.uk"));
        assertTrue(ValidationUtil.isValidEmail("first.last@sub.domain.com"));
    }

    @Test
    public void testIsValidEmail_InvalidEmail_ReturnsFalse() {
        assertFalse(ValidationUtil.isValidEmail("invalid"));
        assertFalse(ValidationUtil.isValidEmail("@example.com"));
        assertFalse(ValidationUtil.isValidEmail("user@"));
        assertFalse(ValidationUtil.isValidEmail("user @example.com"));
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    @Test
    public void testIsValidPhoneNumber_ValidPhone_ReturnsTrue() {
        assertTrue(ValidationUtil.isValidPhoneNumber("123-456-7890"));
        assertTrue(ValidationUtil.isValidPhoneNumber("(123) 456-7890"));
        assertTrue(ValidationUtil.isValidPhoneNumber("+1-123-456-7890"));
    }

    @Test
    public void testIsValidPhoneNumber_InvalidPhone_ReturnsFalse() {
        assertFalse(ValidationUtil.isValidPhoneNumber("123"));
        assertFalse(ValidationUtil.isValidPhoneNumber("abcd"));
        assertFalse(ValidationUtil.isValidPhoneNumber(null));
    }

    @Test
    public void testIsValidURL_ValidURL_ReturnsTrue() {
        assertTrue(ValidationUtil.isValidURL("http://www.example.com"));
        assertTrue(ValidationUtil.isValidURL("https://example.com/path"));
        assertTrue(ValidationUtil.isValidURL("https://sub.example.com:8080/path?query=1"));
    }

    @Test
    public void testIsValidURL_InvalidURL_ReturnsFalse() {
        assertFalse(ValidationUtil.isValidURL("not a url"));
        assertFalse(ValidationUtil.isValidURL("htp://invalid"));
        assertFalse(ValidationUtil.isValidURL(null));
    }

    @Test
    public void testIsNotEmpty_NonEmptyString_ReturnsTrue() {
        assertTrue(ValidationUtil.isNotEmpty("test"));
        assertTrue(ValidationUtil.isNotEmpty("a"));
        assertTrue(ValidationUtil.isNotEmpty("   text   "));
    }

    @Test
    public void testIsNotEmpty_EmptyOrNull_ReturnsFalse() {
        assertFalse(ValidationUtil.isNotEmpty(""));
        assertFalse(ValidationUtil.isNotEmpty("   "));
        assertFalse(ValidationUtil.isNotEmpty(null));
    }

    @Test
    public void testIsNumeric_NumericString_ReturnsTrue() {
        assertTrue(ValidationUtil.isNumeric("123"));
        assertTrue(ValidationUtil.isNumeric("0"));
        assertTrue(ValidationUtil.isNumeric("999999"));
    }

    @Test
    public void testIsNumeric_NonNumeric_ReturnsFalse() {
        assertFalse(ValidationUtil.isNumeric("abc"));
        assertFalse(ValidationUtil.isNumeric("12.34"));
        assertFalse(ValidationUtil.isNumeric("12a"));
        assertFalse(ValidationUtil.isNumeric(null));
    }

    @Test
    public void testIsValidPassword_StrongPassword_ReturnsTrue() {
        assertTrue(ValidationUtil.isValidPassword("Passw0rd!"));
        assertTrue(ValidationUtil.isValidPassword("SecureP@ss123"));
    }

    @Test
    public void testIsValidPassword_WeakPassword_ReturnsFalse() {
        assertFalse(ValidationUtil.isValidPassword("short"));
        assertFalse(ValidationUtil.isValidPassword("nouppercase1!"));
        assertFalse(ValidationUtil.isValidPassword("NOLOWERCASE1!"));
        assertFalse(ValidationUtil.isValidPassword("NoNumber!"));
        assertFalse(ValidationUtil.isValidPassword("NoSpecial1"));
    }

    @Test
    public void testIsCreditCardValid_ValidCard_ReturnsTrue() {
        assertTrue(ValidationUtil.isCreditCardValid("4532-1488-0343-6467")); // Visa format
    }

    @Test
    public void testIsCreditCardValid_InvalidCard_ReturnsFalse() {
        assertFalse(ValidationUtil.isCreditCardValid("1234-5678-9012-3456"));
        assertFalse(ValidationUtil.isCreditCardValid("invalid"));
        assertFalse(ValidationUtil.isCreditCardValid(null));
    }

    @Test
    public void testIsAlphabetic_AlphabeticOnly_ReturnsTrue() {
        assertTrue(ValidationUtil.isAlphabetic("abcd"));
        assertTrue(ValidationUtil.isAlphabetic("ABCD"));
        assertTrue(ValidationUtil.isAlphabetic("aBcD"));
    }

    @Test
    public void testIsAlphabetic_NonAlphabetic_ReturnsFalse() {
        assertFalse(ValidationUtil.isAlphabetic("abc123"));
        assertFalse(ValidationUtil.isAlphabetic("abc@"));
        assertFalse(ValidationUtil.isAlphabetic("123"));
    }

    // Validation utility class
    static class ValidationUtil {
        
        public static boolean isValidEmail(String email) {
            if (email == null) return false;
            return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        }

        public static boolean isValidPhoneNumber(String phone) {
            if (phone == null) return false;
            return phone.matches("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$");
        }

        public static boolean isValidURL(String url) {
            if (url == null) return false;
            return url.matches("^(http|https)://[A-Za-z0-9.-]+(:[0-9]{1,5})?(/.*)?$");
        }

        public static boolean isNotEmpty(String str) {
            return str != null && !str.trim().isEmpty();
        }

        public static boolean isNumeric(String str) {
            if (str == null) return false;
            return str.matches("^[0-9]+$");
        }

        public static boolean isValidPassword(String password) {
            if (password == null || password.length() < 8) return false;
            return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
        }

        public static boolean isCreditCardValid(String cardNumber) {
            if (cardNumber == null) return false;
            String cleaned = cardNumber.replaceAll("[^0-9]", "");
            return cleaned.matches("^[0-9]{16}$") && luhnCheck(cleaned);
        }

        private static boolean luhnCheck(String cardNumber) {
            int sum = 0;
            boolean alternate = false;
            for (int i = cardNumber.length() - 1; i >= 0; i--) {
                int n = Integer.parseInt(cardNumber.substring(i, i + 1));
                if (alternate) {
                    n *= 2;
                    if (n > 9) n -= 9;
                }
                sum += n;
                alternate = !alternate;
            }
            return (sum % 10 == 0);
        }

        public static boolean isAlphabetic(String str) {
            if (str == null) return false;
            return str.matches("^[A-Za-z]+$");
        }
    }
}
