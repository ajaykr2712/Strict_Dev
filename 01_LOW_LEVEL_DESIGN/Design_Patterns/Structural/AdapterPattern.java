/**
 * Adapter Pattern Implementation
 * 
 * The Adapter pattern allows incompatible interfaces to work together.
 * It acts as a bridge between two incompatible interfaces by wrapping
 * an existing class with a new interface.
 * 
 * Real-world examples:
 * - Legacy system integration
 * - Third-party library integration
 * - Database adapters
 * - Media format converters
 */

import java.util.*;

// Target interface - what the client expects
interface MediaPlayer {
    void play(String audioType, String fileName);
}

// Adaptee classes - existing incompatible interfaces
class Mp3Player {
    public void playMp3(String fileName) {
        System.out.println("Playing MP3 file: " + fileName);
    }
}

class Mp4Player {
    public void playMp4(String fileName) {
        System.out.println("Playing MP4 file: " + fileName);
    }
}

class WavPlayer {
    public void playWav(String fileName) {
        System.out.println("Playing WAV file: " + fileName);
    }
}

// Advanced media player interface
interface AdvancedMediaPlayer {
    void playMp4(String fileName);
    void playWav(String fileName);
}

// Adapter class implementing the advanced media player
class MediaAdapter implements AdvancedMediaPlayer {
    Mp4Player mp4Player;
    WavPlayer wavPlayer;
    
    public MediaAdapter(String audioType) {
        if (audioType.equalsIgnoreCase("mp4")) {
            mp4Player = new Mp4Player();
        } else if (audioType.equalsIgnoreCase("wav")) {
            wavPlayer = new WavPlayer();
        }
    }
    
    @Override
    public void playMp4(String fileName) {
        if (mp4Player != null) {
            mp4Player.playMp4(fileName);
        }
    }
    
    @Override
    public void playWav(String fileName) {
        if (wavPlayer != null) {
            wavPlayer.playWav(fileName);
        }
    }
}

// Context class that uses the adapter
class AudioPlayer implements MediaPlayer {
    MediaAdapter mediaAdapter;
    
    @Override
    public void play(String audioType, String fileName) {
        // Built-in support for MP3
        if (audioType.equalsIgnoreCase("mp3")) {
            Mp3Player mp3Player = new Mp3Player();
            mp3Player.playMp3(fileName);
        }
        // MediaAdapter is providing support to play other file formats
        else if (audioType.equalsIgnoreCase("mp4") || audioType.equalsIgnoreCase("wav")) {
            mediaAdapter = new MediaAdapter(audioType);
            if (audioType.equalsIgnoreCase("mp4")) {
                mediaAdapter.playMp4(fileName);
            } else if (audioType.equalsIgnoreCase("wav")) {
                mediaAdapter.playWav(fileName);
            }
        } else {
            System.out.println("Invalid media. " + audioType + " format not supported");
        }
    }
}

// Database Adapter Example
interface DatabaseConnection {
    void connect();
    void disconnect();
    List<Map<String, Object>> executeQuery(String query);
    boolean executeUpdate(String query);
}

// Legacy database systems
class OldMySQLDatabase {
    public void establishConnection() {
        System.out.println("Connected to legacy MySQL database");
    }
    
    public void closeConnection() {
        System.out.println("Disconnected from legacy MySQL database");
    }
    
    public String runQuery(String sql) {
        return "MySQL Result: " + sql;
    }
    
    public int runUpdate(String sql) {
        System.out.println("MySQL Update executed: " + sql);
        return 1; // rows affected
    }
}

class LegacyOracleDatabase {
    public void openConnection() {
        System.out.println("Connected to legacy Oracle database");
    }
    
    public void terminateConnection() {
        System.out.println("Disconnected from legacy Oracle database");
    }
    
    public Object[] fetchData(String sql) {
        return new Object[]{"Oracle Result: " + sql};
    }
    
    public boolean modifyData(String sql) {
        System.out.println("Oracle Update executed: " + sql);
        return true;
    }
}

// Adapters for legacy databases
class MySQLAdapter implements DatabaseConnection {
    private OldMySQLDatabase mysqlDb;
    
    public MySQLAdapter() {
        this.mysqlDb = new OldMySQLDatabase();
    }
    
    @Override
    public void connect() {
        mysqlDb.establishConnection();
    }
    
    @Override
    public void disconnect() {
        mysqlDb.closeConnection();
    }
    
    @Override
    public List<Map<String, Object>> executeQuery(String query) {
        String result = mysqlDb.runQuery(query);
        List<Map<String, Object>> resultList = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("result", result);
        resultList.add(row);
        return resultList;
    }
    
    @Override
    public boolean executeUpdate(String query) {
        return mysqlDb.runUpdate(query) > 0;
    }
}

class OracleAdapter implements DatabaseConnection {
    private LegacyOracleDatabase oracleDb;
    
    public OracleAdapter() {
        this.oracleDb = new LegacyOracleDatabase();
    }
    
    @Override
    public void connect() {
        oracleDb.openConnection();
    }
    
    @Override
    public void disconnect() {
        oracleDb.terminateConnection();
    }
    
    @Override
    public List<Map<String, Object>> executeQuery(String query) {
        Object[] results = oracleDb.fetchData(query);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Object result : results) {
            Map<String, Object> row = new HashMap<>();
            row.put("result", result);
            resultList.add(row);
        }
        return resultList;
    }
    
    @Override
    public boolean executeUpdate(String query) {
        return oracleDb.modifyData(query);
    }
}

// Database factory using adapters
class DatabaseFactory {
    public static DatabaseConnection createConnection(String type) {
        switch (type.toLowerCase()) {
            case "mysql":
                return new MySQLAdapter();
            case "oracle":
                return new OracleAdapter();
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }
}

// Payment Gateway Adapter Example
interface PaymentGateway {
    boolean processPayment(double amount, String currency, String cardNumber);
    String getTransactionId();
    boolean refund(String transactionId, double amount);
}

// Third-party payment services with different interfaces
class StripePaymentService {
    public String charge(double amount, String currency, String token) {
        String transactionId = "stripe_" + System.currentTimeMillis();
        System.out.println("Stripe: Charged $" + amount + " " + currency + " with token " + token);
        return transactionId;
    }
    
    public boolean issueRefund(String chargeId, double amount) {
        System.out.println("Stripe: Refunded $" + amount + " for charge " + chargeId);
        return true;
    }
}

class PayPalService {
    public Map<String, Object> makePayment(double amount, String currency, String account) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("payment_id", "paypal_" + System.currentTimeMillis());
        result.put("status", "completed");
        System.out.println("PayPal: Payment of $" + amount + " " + currency + " processed for " + account);
        return result;
    }
    
    public boolean processRefund(String paymentId, double amount) {
        System.out.println("PayPal: Refund of $" + amount + " processed for payment " + paymentId);
        return true;
    }
}

// Payment Gateway Adapters
class StripeAdapter implements PaymentGateway {
    private StripePaymentService stripeService;
    private String lastTransactionId;
    
    public StripeAdapter() {
        this.stripeService = new StripePaymentService();
    }
    
    @Override
    public boolean processPayment(double amount, String currency, String cardNumber) {
        try {
            // Convert card number to token (simplified)
            String token = "tok_" + cardNumber.hashCode();
            lastTransactionId = stripeService.charge(amount, currency, token);
            return lastTransactionId != null;
        } catch (Exception e) {
            System.out.println("Stripe payment failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getTransactionId() {
        return lastTransactionId;
    }
    
    @Override
    public boolean refund(String transactionId, double amount) {
        return stripeService.issueRefund(transactionId, amount);
    }
}

class PayPalAdapter implements PaymentGateway {
    private PayPalService paypalService;
    private String lastTransactionId;
    
    public PayPalAdapter() {
        this.paypalService = new PayPalService();
    }
    
    @Override
    public boolean processPayment(double amount, String currency, String cardNumber) {
        try {
            // Convert card number to account (simplified)
            String account = "account_" + cardNumber.hashCode();
            Map<String, Object> result = paypalService.makePayment(amount, currency, account);
            
            if ((Boolean) result.get("success")) {
                lastTransactionId = (String) result.get("payment_id");
                return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println("PayPal payment failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getTransactionId() {
        return lastTransactionId;
    }
    
    @Override
    public boolean refund(String transactionId, double amount) {
        return paypalService.processRefund(transactionId, amount);
    }
}

// Payment processor that works with any payment gateway
class PaymentProcessor {
    private PaymentGateway gateway;
    
    public PaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }
    
    public boolean processTransaction(double amount, String currency, String cardNumber) {
        System.out.println("Processing payment of $" + amount + " " + currency);
        
        boolean success = gateway.processPayment(amount, currency, cardNumber);
        
        if (success) {
            System.out.println("Payment successful! Transaction ID: " + gateway.getTransactionId());
        } else {
            System.out.println("Payment failed!");
        }
        
        return success;
    }
    
    public boolean processRefund(String transactionId, double amount) {
        return gateway.refund(transactionId, amount);
    }
}

// Demo class
class AdapterPatternDemo {
    public static void main(String[] args) {
        System.out.println("=== Adapter Pattern Demo ===\n");
        
        // 1. Media Player Adapter
        System.out.println("1. Media Player Adapter:");
        demonstrateMediaPlayerAdapter();
        
        // 2. Database Adapter
        System.out.println("\n2. Database Adapter:");
        demonstrateDatabaseAdapter();
        
        // 3. Payment Gateway Adapter
        System.out.println("\n3. Payment Gateway Adapter:");
        demonstratePaymentGatewayAdapter();
    }
    
    private static void demonstrateMediaPlayerAdapter() {
        AudioPlayer audioPlayer = new AudioPlayer();
        
        audioPlayer.play("mp3", "beyond_the_horizon.mp3");
        audioPlayer.play("mp4", "alone.mp4");
        audioPlayer.play("wav", "far_far_away.wav");
        audioPlayer.play("avi", "mind_me.avi");
    }
    
    private static void demonstrateDatabaseAdapter() {
        // Using MySQL through adapter
        DatabaseConnection mysqlConnection = DatabaseFactory.createConnection("mysql");
        mysqlConnection.connect();
        
        List<Map<String, Object>> results = mysqlConnection.executeQuery("SELECT * FROM users");
        System.out.println("Query results: " + results);
        
        boolean updateSuccess = mysqlConnection.executeUpdate("UPDATE users SET name='John' WHERE id=1");
        System.out.println("Update successful: " + updateSuccess);
        
        mysqlConnection.disconnect();
        
        System.out.println();
        
        // Using Oracle through adapter
        DatabaseConnection oracleConnection = DatabaseFactory.createConnection("oracle");
        oracleConnection.connect();
        
        List<Map<String, Object>> oracleResults = oracleConnection.executeQuery("SELECT * FROM employees");
        System.out.println("Query results: " + oracleResults);
        
        oracleConnection.disconnect();
    }
    
    private static void demonstratePaymentGatewayAdapter() {
        // Process payments using different gateways with the same interface
        
        // Using Stripe
        PaymentGateway stripeGateway = new StripeAdapter();
        PaymentProcessor stripeProcessor = new PaymentProcessor(stripeGateway);
        
        boolean stripeSuccess = stripeProcessor.processTransaction(100.0, "USD", "4242424242424242");
        if (stripeSuccess) {
            stripeProcessor.processRefund(stripeGateway.getTransactionId(), 25.0);
        }
        
        System.out.println();
        
        // Using PayPal
        PaymentGateway paypalGateway = new PayPalAdapter();
        PaymentProcessor paypalProcessor = new PaymentProcessor(paypalGateway);
        
        boolean paypalSuccess = paypalProcessor.processTransaction(75.0, "EUR", "1234567890123456");
        if (paypalSuccess) {
            paypalProcessor.processRefund(paypalGateway.getTransactionId(), 10.0);
        }
    }
}

/*
 * Adapter Pattern Benefits:
 * 
 * ✅ Advantages:
 * - Allows incompatible classes to work together
 * - Promotes code reuse of existing functionality
 * - Separates interface from implementation concerns
 * - Enables integration with third-party libraries
 * - Supports the Open/Closed Principle
 * 
 * ❌ Disadvantages:
 * - Increases overall complexity of the code
 * - Sometimes it's simpler to just change the service class
 * - Can make code harder to understand
 * 
 * When to use:
 * - When you want to integrate legacy code with new systems
 * - When you need to use a third-party library with an incompatible interface
 * - When you want to create a reusable class that cooperates with unrelated classes
 * - When you need to use several existing subclasses, but it's impractical to adapt their interface by subclassing
 */
