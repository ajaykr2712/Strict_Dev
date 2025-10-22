package unittests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * Unit tests for Currency Converter
 * Tests currency conversion, exchange rates, and rounding
 */
public class CurrencyConverterTest {

    private CurrencyConverter converter;

    @Before
    public void setUp() {
        converter = new CurrencyConverter();
        // Setup some exchange rates
        converter.setExchangeRate("USD", "EUR", new BigDecimal("0.85"));
        converter.setExchangeRate("USD", "GBP", new BigDecimal("0.73"));
        converter.setExchangeRate("EUR", "GBP", new BigDecimal("0.86"));
    }

    @Test
    public void testConvert_USDToEUR() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        BigDecimal result = converter.convert(amount, "USD", "EUR");

        // Assert
        assertEquals(new BigDecimal("85.00"), result);
    }

    @Test
    public void testConvert_USDToGBP() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        BigDecimal result = converter.convert(amount, "USD", "GBP");

        // Assert
        assertEquals(new BigDecimal("73.00"), result);
    }

    @Test
    public void testConvert_SameCurrency_ReturnsOriginal() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        BigDecimal result = converter.convert(amount, "USD", "USD");

        // Assert
        assertEquals(amount, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvert_UnknownCurrency_ThrowsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        converter.convert(amount, "USD", "XYZ");
    }

    @Test
    public void testConvert_ZeroAmount() {
        // Arrange
        BigDecimal amount = BigDecimal.ZERO;

        // Act
        BigDecimal result = converter.convert(amount, "USD", "EUR");

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testRoundToTwoDecimals() {
        // Arrange
        BigDecimal value = new BigDecimal("123.456");

        // Act
        BigDecimal result = CurrencyConverter.round(value, 2);

        // Assert
        assertEquals(new BigDecimal("123.46"), result);
    }

    @Test
    public void testFormatCurrency() {
        // Arrange
        BigDecimal amount = new BigDecimal("1234.56");

        // Act
        String result = converter.formatCurrency(amount, "USD");

        // Assert
        assertEquals("$1,234.56", result);
    }

    @Test
    public void testGetExchangeRate() {
        // Act
        BigDecimal rate = converter.getExchangeRate("USD", "EUR");

        // Assert
        assertEquals(new BigDecimal("0.85"), rate);
    }

    @Test
    public void testSetExchangeRate() {
        // Act
        converter.setExchangeRate("USD", "JPY", new BigDecimal("110.50"));
        BigDecimal rate = converter.getExchangeRate("USD", "JPY");

        // Assert
        assertEquals(new BigDecimal("110.50"), rate);
    }

    // Currency converter implementation
    static class CurrencyConverter {
        private java.util.Map<String, BigDecimal> exchangeRates;

        public CurrencyConverter() {
            this.exchangeRates = new java.util.HashMap<>();
        }

        public void setExchangeRate(String from, String to, BigDecimal rate) {
            exchangeRates.put(from + "_" + to, rate);
        }

        public BigDecimal getExchangeRate(String from, String to) {
            return exchangeRates.get(from + "_" + to);
        }

        public BigDecimal convert(BigDecimal amount, String from, String to) {
            if (from.equals(to)) {
                return amount;
            }

            BigDecimal rate = exchangeRates.get(from + "_" + to);
            if (rate == null) {
                throw new IllegalArgumentException("Exchange rate not found");
            }

            return amount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        public String formatCurrency(BigDecimal amount, String currency) {
            if ("USD".equals(currency)) {
                return "$" + String.format("%,.2f", amount);
            }
            return amount.toString();
        }

        public static BigDecimal round(BigDecimal value, int places) {
            return value.setScale(places, BigDecimal.ROUND_HALF_UP);
        }
    }
}
