// Observer Pattern LLD Implementation

/**
 * Observer Pattern - Low Level Design Implementation
 * 
 * Real-world Use Case: Stock Trading System
 * - Multiple investors want to be notified when stock prices change
 * - Different types of notifications (Email, SMS, Push)
 * - Dynamic subscription/unsubscription
 * 
 * Key Components:
 * 1. Subject (Observable) - Stock
 * 2. Observer - Investor/Notification Service
 * 3. ConcreteSubject - Specific Stock
 * 4. ConcreteObserver - Specific Notification Type
 */

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

// Observer interface
interface StockObserver {
    void update(Stock stock, BigDecimal oldPrice, BigDecimal newPrice);
    String getObserverId();
    NotificationType getNotificationType();
}

// Subject interface
interface StockSubject {
    void addObserver(StockObserver observer);
    void removeObserver(StockObserver observer);
    void notifyObservers(BigDecimal oldPrice, BigDecimal newPrice);
}

// Notification types
enum NotificationType {
    EMAIL, SMS, PUSH_NOTIFICATION, DASHBOARD_UPDATE
}

// Stock price event
class StockPriceEvent {
    private final String stockSymbol;
    private final BigDecimal oldPrice;
    private final BigDecimal newPrice;
    private final LocalDateTime timestamp;
    private final BigDecimal changePercentage;
    
    public StockPriceEvent(String stockSymbol, BigDecimal oldPrice, BigDecimal newPrice) {
        this.stockSymbol = stockSymbol;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.timestamp = LocalDateTime.now();
        this.changePercentage = calculateChangePercentage(oldPrice, newPrice);
    }
    
    private BigDecimal calculateChangePercentage(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return newPrice.subtract(oldPrice)
                      .divide(oldPrice, 4, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }
    
    // Getters
    public String getStockSymbol() { return stockSymbol; }
    public BigDecimal getOldPrice() { return oldPrice; }
    public BigDecimal getNewPrice() { return newPrice; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public BigDecimal getChangePercentage() { return changePercentage; }
}

// Concrete Subject - Stock implementation
class Stock implements StockSubject {
    private final String symbol;
    private final String companyName;
    private BigDecimal currentPrice;
    private final List<StockObserver> observers;
    private final List<StockPriceEvent> priceHistory;
    
    public Stock(String symbol, String companyName, BigDecimal initialPrice) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = initialPrice;
        this.observers = new CopyOnWriteArrayList<>(); // Thread-safe
        this.priceHistory = new ArrayList<>();
    }
    
    @Override
    public void addObserver(StockObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("Added observer: " + observer.getObserverId() + 
                             " for stock: " + symbol);
        }
    }
    
    @Override
    public void removeObserver(StockObserver observer) {
        if (observers.remove(observer)) {
            System.out.println("Removed observer: " + observer.getObserverId() + 
                             " from stock: " + symbol);
        }
    }
    
    @Override
    public void notifyObservers(BigDecimal oldPrice, BigDecimal newPrice) {
        StockPriceEvent event = new StockPriceEvent(symbol, oldPrice, newPrice);
        priceHistory.add(event);
        
        System.out.println("\\n🔔 Notifying " + observers.size() + 
                          " observers for " + symbol + " price change");
        
        // Notify all observers asynchronously for better performance
        observers.parallelStream().forEach(observer -> {
            try {
                observer.update(this, oldPrice, newPrice);
            } catch (Exception e) {
                System.err.println("Error notifying observer " + 
                                 observer.getObserverId() + ": " + e.getMessage());
            }
        });
    }
    
    // Business method that triggers notifications
    public void setPrice(BigDecimal newPrice) {
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Stock price cannot be negative");
        }
        
        BigDecimal oldPrice = this.currentPrice;
        this.currentPrice = newPrice;
        
        // Only notify if price actually changed
        if (oldPrice.compareTo(newPrice) != 0) {
            notifyObservers(oldPrice, newPrice);
        }
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public List<StockPriceEvent> getPriceHistory() { return new ArrayList<>(priceHistory); }
    public int getObserverCount() { return observers.size(); }
}

// Abstract base class for observers to avoid code duplication
abstract class BaseStockObserver implements StockObserver {
    protected final String observerId;
    protected final NotificationType notificationType;
    protected final Set<String> subscribedStocks;
    
    public BaseStockObserver(String observerId, NotificationType notificationType) {
        this.observerId = observerId;
        this.notificationType = notificationType;
        this.subscribedStocks = new HashSet<>();
    }
    
    @Override
    public String getObserverId() { return observerId; }
    
    @Override
    public NotificationType getNotificationType() { return notificationType; }
    
    public void subscribeToStock(String stockSymbol) {
        subscribedStocks.add(stockSymbol);
    }
    
    public void unsubscribeFromStock(String stockSymbol) {
        subscribedStocks.remove(stockSymbol);
    }
    
    protected boolean isSubscribedTo(String stockSymbol) {
        return subscribedStocks.contains(stockSymbol);
    }
}

// Concrete Observer - Email Notification
class EmailNotificationObserver extends BaseStockObserver {
    private final String emailAddress;
    private final BigDecimal priceChangeThreshold;
    
    public EmailNotificationObserver(String observerId, String emailAddress, 
                                   BigDecimal priceChangeThreshold) {
        super(observerId, NotificationType.EMAIL);
        this.emailAddress = emailAddress;
        this.priceChangeThreshold = priceChangeThreshold;
    }
    
    @Override
    public void update(Stock stock, BigDecimal oldPrice, BigDecimal newPrice) {
        if (!isSubscribedTo(stock.getSymbol())) return;
        
        BigDecimal changePercentage = calculateChangePercentage(oldPrice, newPrice);
        
        // Only send email if change exceeds threshold
        if (changePercentage.abs().compareTo(priceChangeThreshold) >= 0) {
            sendEmailNotification(stock, oldPrice, newPrice, changePercentage);
        }
    }
    
    private BigDecimal calculateChangePercentage(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return newPrice.subtract(oldPrice)
                      .divide(oldPrice, 4, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }
    
    private void sendEmailNotification(Stock stock, BigDecimal oldPrice, 
                                     BigDecimal newPrice, BigDecimal changePercentage) {
        String direction = changePercentage.compareTo(BigDecimal.ZERO) > 0 ? "📈" : "📉";
        
        System.out.println("📧 EMAIL to " + emailAddress + ":");
        System.out.println("   Subject: " + direction + " " + stock.getCompanyName() + 
                          " (" + stock.getSymbol() + ") Price Alert");
        System.out.println("   Price changed from $" + oldPrice + " to $" + newPrice);
        System.out.println("   Change: " + changePercentage + "%");
        System.out.println("   Timestamp: " + LocalDateTime.now());
    }
}

// Concrete Observer - SMS Notification
class SMSNotificationObserver extends BaseStockObserver {
    private final String phoneNumber;
    private final BigDecimal significantChangeThreshold;
    
    public SMSNotificationObserver(String observerId, String phoneNumber, 
                                 BigDecimal significantChangeThreshold) {
        super(observerId, NotificationType.SMS);
        this.phoneNumber = phoneNumber;
        this.significantChangeThreshold = significantChangeThreshold;
    }
    
    @Override
    public void update(Stock stock, BigDecimal oldPrice, BigDecimal newPrice) {
        if (!isSubscribedTo(stock.getSymbol())) return;
        
        BigDecimal changePercentage = calculateChangePercentage(oldPrice, newPrice);
        
        // SMS only for significant changes
        if (changePercentage.abs().compareTo(significantChangeThreshold) >= 0) {
            sendSMSNotification(stock, newPrice, changePercentage);
        }
    }
    
    private BigDecimal calculateChangePercentage(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return newPrice.subtract(oldPrice)
                      .divide(oldPrice, 4, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }
    
    private void sendSMSNotification(Stock stock, BigDecimal newPrice, BigDecimal changePercentage) {
        String direction = changePercentage.compareTo(BigDecimal.ZERO) > 0 ? "UP" : "DOWN";
        
        System.out.println("📱 SMS to " + phoneNumber + ":");
        System.out.println("   " + stock.getSymbol() + " is " + direction + " " + 
                          changePercentage.abs() + "% to $" + newPrice);
    }
}

// Concrete Observer - Dashboard Update
class DashboardObserver extends BaseStockObserver {
    private final Map<String, BigDecimal> currentPrices;
    private final String userId;
    
    public DashboardObserver(String observerId, String userId) {
        super(observerId, NotificationType.DASHBOARD_UPDATE);
        this.currentPrices = new HashMap<>();
        this.userId = userId;
    }
    
    @Override
    public void update(Stock stock, BigDecimal oldPrice, BigDecimal newPrice) {
        if (!isSubscribedTo(stock.getSymbol())) return;
        
        currentPrices.put(stock.getSymbol(), newPrice);
        updateDashboard(stock, newPrice);
    }
    
    private void updateDashboard(Stock stock, BigDecimal newPrice) {
        System.out.println("🖥️  DASHBOARD UPDATE for user " + userId + ":");
        System.out.println("   " + stock.getSymbol() + " -> $" + newPrice);
        System.out.println("   Portfolio updated in real-time");
    }
    
    public Map<String, BigDecimal> getCurrentPrices() {
        return new HashMap<>(currentPrices);
    }
}

// Stock Market Simulator for demonstration
class StockMarketSimulator {
    private final Map<String, Stock> stocks;
    private final Random random;
    
    public StockMarketSimulator() {
        this.stocks = new HashMap<>();
        this.random = new Random();
        initializeStocks();
    }
    
    private void initializeStocks() {
        addStock("AAPL", "Apple Inc.", new BigDecimal("150.00"));
        addStock("GOOGL", "Alphabet Inc.", new BigDecimal("2500.00"));
        addStock("TSLA", "Tesla Inc.", new BigDecimal("800.00"));
        addStock("AMZN", "Amazon.com Inc.", new BigDecimal("3200.00"));
    }
    
    public void addStock(String symbol, String companyName, BigDecimal initialPrice) {
        stocks.put(symbol, new Stock(symbol, companyName, initialPrice));
    }
    
    public Stock getStock(String symbol) {
        return stocks.get(symbol);
    }
    
    public void simulateMarketMovement() {
        stocks.values().forEach(stock -> {
            // Random price change between -10% to +10%
            double changePercent = (random.nextDouble() - 0.5) * 0.2; // -0.1 to 0.1
            BigDecimal currentPrice = stock.getCurrentPrice();
            BigDecimal change = currentPrice.multiply(BigDecimal.valueOf(changePercent));
            BigDecimal newPrice = currentPrice.add(change).max(BigDecimal.ONE);
            
            stock.setPrice(newPrice);
        });
    }
    
    public void printMarketSummary() {
        System.out.println("\\n📊 MARKET SUMMARY:");
        System.out.println("===================");
        stocks.values().forEach(stock -> {
            System.out.println(stock.getSymbol() + " (" + stock.getCompanyName() + 
                             "): $" + stock.getCurrentPrice() + 
                             " [" + stock.getObserverCount() + " observers]");
        });
    }
}

// Main demonstration class
public class ObserverPatternLLD {
    public static void main(String[] args) {
        System.out.println("🏦 STOCK TRADING SYSTEM - Observer Pattern Demo");
        System.out.println("===============================================\\n");
        
        // Create market simulator
        StockMarketSimulator market = new StockMarketSimulator();
        
        // Create observers (investors/notification services)
        EmailNotificationObserver emailObserver = new EmailNotificationObserver(
            "email-001", "investor@email.com", new BigDecimal("2.0") // 2% threshold
        );
        
        SMSNotificationObserver smsObserver = new SMSNotificationObserver(
            "sms-001", "+1-555-0123", new BigDecimal("5.0") // 5% threshold
        );
        
        DashboardObserver dashboardObserver = new DashboardObserver(
            "dashboard-001", "user123"
        );
        
        // Subscribe observers to stocks
        Stock appleStock = market.getStock("AAPL");
        Stock googleStock = market.getStock("GOOGL");
        Stock teslaStock = market.getStock("TSLA");
        
        // Subscribe email observer to Apple and Google
        appleStock.addObserver(emailObserver);
        googleStock.addObserver(emailObserver);
        emailObserver.subscribeToStock("AAPL");
        emailObserver.subscribeToStock("GOOGL");
        
        // Subscribe SMS observer to Tesla (high volatility stock)
        teslaStock.addObserver(smsObserver);
        smsObserver.subscribeToStock("TSLA");
        
        // Subscribe dashboard to all stocks
        appleStock.addObserver(dashboardObserver);
        googleStock.addObserver(dashboardObserver);
        teslaStock.addObserver(dashboardObserver);
        dashboardObserver.subscribeToStock("AAPL");
        dashboardObserver.subscribeToStock("GOOGL");
        dashboardObserver.subscribeToStock("TSLA");
        
        // Print initial market state
        market.printMarketSummary();
        
        // Simulate market movements
        System.out.println("\\n🎲 SIMULATING MARKET MOVEMENTS...");
        System.out.println("==================================");
        
        for (int i = 1; i <= 3; i++) {
            System.out.println("\\n--- Market Movement " + i + " ---");
            market.simulateMarketMovement();
            
            // Pause for readability in demo
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // Exit loop if interrupted
            }
        }
        
        // Demonstrate dynamic subscription changes
        System.out.println("\\n🔄 DYNAMIC SUBSCRIPTION CHANGE");
        System.out.println("===============================");
        
        // Remove email observer from Apple
        appleStock.removeObserver(emailObserver);
        emailObserver.unsubscribeFromStock("AAPL");
        
        // Add SMS observer to Apple
        appleStock.addObserver(smsObserver);
        smsObserver.subscribeToStock("AAPL");
        
        // One more market movement
        System.out.println("\\n--- After Subscription Changes ---");
        market.simulateMarketMovement();
        
        // Final market summary
        market.printMarketSummary();
        
        // Show dashboard data
        System.out.println("\\nDashboard Current Prices:");
        dashboardObserver.getCurrentPrices().forEach((symbol, price) -> 
            System.out.println("  " + symbol + ": $" + price));
            
        System.out.println("\\n✅ Observer Pattern Demo Complete!");
    }
}
