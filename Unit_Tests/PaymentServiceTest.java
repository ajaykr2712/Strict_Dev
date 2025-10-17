import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * Unit tests for Payment Service
 * Tests payment processing, validation, and transaction handling
 */
public class PaymentServiceTest {

    private PaymentProcessor paymentProcessor;

    @Before
    public void setUp() {
        paymentProcessor = new PaymentProcessor();
    }

    @Test
    public void testProcessPayment_ValidAmount_Success() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        String cardNumber = "1234-5678-9012-3456";

        // Act
        PaymentResult result = paymentProcessor.processPayment(amount, cardNumber);

        // Assert
        assertTrue("Payment should be successful", result.isSuccessful());
        assertNotNull("Transaction ID should not be null", result.getTransactionId());
    }

    @Test
    public void testProcessPayment_NegativeAmount_Fails() {
        // Arrange
        BigDecimal amount = new BigDecimal("-50.00");
        String cardNumber = "1234-5678-9012-3456";

        // Act
        PaymentResult result = paymentProcessor.processPayment(amount, cardNumber);

        // Assert
        assertFalse("Payment should fail for negative amount", result.isSuccessful());
        assertEquals("Should have error message", "Invalid amount", result.getErrorMessage());
    }

    @Test
    public void testProcessPayment_ZeroAmount_Fails() {
        // Arrange
        BigDecimal amount = BigDecimal.ZERO;
        String cardNumber = "1234-5678-9012-3456";

        // Act
        PaymentResult result = paymentProcessor.processPayment(amount, cardNumber);

        // Assert
        assertFalse("Payment should fail for zero amount", result.isSuccessful());
    }

    @Test
    public void testProcessPayment_InvalidCardNumber_Fails() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        String cardNumber = "invalid";

        // Act
        PaymentResult result = paymentProcessor.processPayment(amount, cardNumber);

        // Assert
        assertFalse("Payment should fail for invalid card", result.isSuccessful());
        assertEquals("Should have error message", "Invalid card number", result.getErrorMessage());
    }

    @Test
    public void testProcessPayment_NullCardNumber_Fails() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        PaymentResult result = paymentProcessor.processPayment(amount, null);

        // Assert
        assertFalse("Payment should fail for null card", result.isSuccessful());
    }

    @Test
    public void testProcessPayment_LargeAmount_Success() {
        // Arrange
        BigDecimal amount = new BigDecimal("999999.99");
        String cardNumber = "1234-5678-9012-3456";

        // Act
        PaymentResult result = paymentProcessor.processPayment(amount, cardNumber);

        // Assert
        assertTrue("Payment should succeed for large amounts", result.isSuccessful());
    }

    @Test
    public void testRefundPayment_ValidTransaction_Success() {
        // Arrange
        BigDecimal amount = new BigDecimal("50.00");
        String cardNumber = "1234-5678-9012-3456";
        PaymentResult payment = paymentProcessor.processPayment(amount, cardNumber);

        // Act
        RefundResult refund = paymentProcessor.refundPayment(payment.getTransactionId(), amount);

        // Assert
        assertTrue("Refund should be successful", refund.isSuccessful());
    }

    @Test
    public void testRefundPayment_NonExistentTransaction_Fails() {
        // Arrange
        BigDecimal amount = new BigDecimal("50.00");

        // Act
        RefundResult refund = paymentProcessor.refundPayment("invalid-tx-id", amount);

        // Assert
        assertFalse("Refund should fail for non-existent transaction", refund.isSuccessful());
    }

    // Test classes
    static class PaymentResult {
        private boolean successful;
        private String transactionId;
        private String errorMessage;

        public PaymentResult(boolean successful, String transactionId, String errorMessage) {
            this.successful = successful;
            this.transactionId = transactionId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccessful() { return successful; }
        public String getTransactionId() { return transactionId; }
        public String getErrorMessage() { return errorMessage; }
    }

    static class RefundResult {
        private boolean successful;
        
        public RefundResult(boolean successful) {
            this.successful = successful;
        }
        
        public boolean isSuccessful() { return successful; }
    }

    static class PaymentProcessor {
        public PaymentResult processPayment(BigDecimal amount, String cardNumber) {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return new PaymentResult(false, null, "Invalid amount");
            }
            if (cardNumber == null || !isValidCardNumber(cardNumber)) {
                return new PaymentResult(false, null, "Invalid card number");
            }
            String txId = "TX-" + System.currentTimeMillis();
            return new PaymentResult(true, txId, null);
        }

        public RefundResult refundPayment(String transactionId, BigDecimal amount) {
            if (transactionId == null || !transactionId.startsWith("TX-")) {
                return new RefundResult(false);
            }
            return new RefundResult(true);
        }

        private boolean isValidCardNumber(String cardNumber) {
            return cardNumber.matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}");
        }
    }
}
