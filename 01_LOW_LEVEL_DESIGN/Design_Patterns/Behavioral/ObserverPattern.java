/**
 * Observer Pattern Implementation
 * 
 * The Observer pattern defines a one-to-many dependency between objects
 * so that when one object changes state, all dependents are notified
 * and updated automatically.
 * 
 * Real-world examples:
 * - Event handling systems
 * - Model-View-Controller (MVC)
 * - News subscriptions
 * - Stock price monitoring
 * - Social media notifications
 */

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Subject interface
interface Subject {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObservers();
}

// Observer interface
interface Observer {
    void update(Subject subject);
    String getObserverName();
}

// Concrete Subject - Stock Price Monitor
class Stock implements Subject {
    private final String symbol;
    private double price;
    private double change;
    private double changePercent;
    private final List<Observer> observers = new ArrayList<>();
    private LocalDateTime lastUpdated;
    
    public Stock(String symbol, double initialPrice) {
        this.symbol = symbol;
        this.price = initialPrice;
        this.change = 0.0;
        this.changePercent = 0.0;
        this.lastUpdated = LocalDateTime.now();
    }
    
    @Override
    public void attach(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("Observer " + observer.getObserverName() + " attached to " + symbol);
        }
    }
    
    @Override
    public void detach(Observer observer) {
        if (observers.remove(observer)) {
            System.out.println("Observer " + observer.getObserverName() + " detached from " + symbol);
        }
    }
    
    @Override
    public void notifyObservers() {
        System.out.println("\\nNotifying " + observers.size() + " observers about " + symbol + " price change...");
        for (Observer observer : observers) {
            observer.update(this);
        }
    }
    
    public void setPrice(double newPrice) {
        double oldPrice = this.price;
        this.price = newPrice;
        this.change = newPrice - oldPrice;
        this.changePercent = (oldPrice != 0) ? (change / oldPrice) * 100 : 0;
        this.lastUpdated = LocalDateTime.now();
        
        System.out.println("\\n" + symbol + " price updated: $" + String.format("%.2f", oldPrice) + 
                         " -> $" + String.format("%.2f", newPrice));
        
        notifyObservers();
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public double getChange() { return change; }
    public double getChangePercent() { return changePercent; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public int getObserverCount() { return observers.size(); }
}

// Concrete Observers
class StockDisplay implements Observer {
    private final String displayName;
    private final String displayType;
    
    public StockDisplay(String displayName, String displayType) {
        this.displayName = displayName;
        this.displayType = displayType;
    }
    
    @Override
    public void update(Subject subject) {
        if (subject instanceof Stock) {
            Stock stock = (Stock) subject;
            displayStockInfo(stock);
        }
    }
    
    private void displayStockInfo(Stock stock) {
        String changeIndicator = stock.getChange() >= 0 ? "▲" : "▼";
        String changeColor = stock.getChange() >= 0 ? "GREEN" : "RED";
        
        System.out.println("[" + displayType + " - " + displayName + "] " +
                         stock.getSymbol() + ": $" + String.format("%.2f", stock.getPrice()) +
                         " " + changeIndicator + " $" + String.format("%.2f", Math.abs(stock.getChange())) +
                         " (" + String.format("%.2f", stock.getChangePercent()) + "%) [" + changeColor + "]");
    }
    
    @Override
    public String getObserverName() {
        return displayName + " (" + displayType + ")";
    }
}

class StockAlert implements Observer {
    private final String alertName;
    private final double thresholdPrice;
    private final String alertType; // "above" or "below"
    private boolean alertTriggered = false;
    
    public StockAlert(String alertName, double thresholdPrice, String alertType) {
        this.alertName = alertName;
        this.thresholdPrice = thresholdPrice;
        this.alertType = alertType.toLowerCase();
    }
    
    @Override
    public void update(Subject subject) {
        if (subject instanceof Stock) {
            Stock stock = (Stock) subject;
            checkAlert(stock);
        }
    }
    
    private void checkAlert(Stock stock) {
        boolean shouldAlert = false;
        
        if (alertType.equals("above") && stock.getPrice() > thresholdPrice) {
            shouldAlert = true;
        } else if (alertType.equals("below") && stock.getPrice() < thresholdPrice) {
            shouldAlert = true;
        }
        
        if (shouldAlert && !alertTriggered) {
            System.out.println("🚨 ALERT [" + alertName + "]: " + stock.getSymbol() +
                             " is " + alertType + " threshold $" + String.format("%.2f", thresholdPrice) +
                             " (Current: $" + String.format("%.2f", stock.getPrice()) + ")");
            alertTriggered = true;
        } else if (!shouldAlert && alertTriggered) {
            // Reset alert when condition is no longer met
            alertTriggered = false;
        }
    }
    
    @Override
    public String getObserverName() {
        return alertName + " (Alert)";
    }
}

class StockLogger implements Observer {
    private final String loggerName;
    private final List<String> logs = new ArrayList<>();
    
    public StockLogger(String loggerName) {
        this.loggerName = loggerName;
    }
    
    @Override
    public void update(Subject subject) {
        if (subject instanceof Stock) {
            Stock stock = (Stock) subject;
            logStockChange(stock);
        }
    }
    
    private void logStockChange(Stock stock) {
        String timestamp = stock.getLastUpdated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = timestamp + " | " + stock.getSymbol() + " | $" + 
                         String.format("%.2f", stock.getPrice()) + " | Change: $" + 
                         String.format("%.2f", stock.getChange()) + " (" + 
                         String.format("%.2f", stock.getChangePercent()) + "%)";
        
        logs.add(logEntry);
        System.out.println("[" + loggerName + "] Logged: " + logEntry);
    }
    
    public void printLogs() {
        System.out.println("\\n=== " + loggerName + " Logs ===");
        for (String log : logs) {
            System.out.println(log);
        }
    }
    
    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }
    
    @Override
    public String getObserverName() {
        return loggerName + " (Logger)";
    }
}

// News Subscription System Example
class NewsAgency implements Subject {
    private String news;
    private String category;
    private final List<Observer> observers = new ArrayList<>();
    private LocalDateTime publishTime;
    
    @Override
    public void attach(Observer observer) {
        observers.add(observer);
        System.out.println("Subscriber " + observer.getObserverName() + " added to news agency");
    }
    
    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
        System.out.println("Subscriber " + observer.getObserverName() + " removed from news agency");
    }
    
    @Override
    public void notifyObservers() {
        System.out.println("\\nBroadcasting news to " + observers.size() + " subscribers...");
        for (Observer observer : observers) {
            observer.update(this);
        }
    }
    
    public void publishNews(String news, String category) {
        this.news = news;
        this.category = category;
        this.publishTime = LocalDateTime.now();
        
        System.out.println("\\n📰 News Published [" + category + "]: " + news);
        notifyObservers();
    }
    
    // Getters
    public String getNews() { return news; }
    public String getCategory() { return category; }
    public LocalDateTime getPublishTime() { return publishTime; }
}

class NewsSubscriber implements Observer {
    private final String subscriberName;
    private final Set<String> interestedCategories;
    
    public NewsSubscriber(String subscriberName, String... categories) {
        this.subscriberName = subscriberName;
        this.interestedCategories = new HashSet<>(Arrays.asList(categories));
    }
    
    @Override
    public void update(Subject subject) {
        if (subject instanceof NewsAgency) {
            NewsAgency agency = (NewsAgency) subject;
            receiveNews(agency);
        }
    }
    
    private void receiveNews(NewsAgency agency) {
        if (interestedCategories.contains(agency.getCategory()) || interestedCategories.contains("ALL")) {
            String timestamp = agency.getPublishTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println("[" + subscriberName + "] Received [" + agency.getCategory() + 
                             "] at " + timestamp + ": " + agency.getNews());
        }
    }
    
    public void addInterest(String category) {
        interestedCategories.add(category);
        System.out.println(subscriberName + " is now interested in " + category + " news");
    }
    
    public void removeInterest(String category) {
        interestedCategories.remove(category);
        System.out.println(subscriberName + " is no longer interested in " + category + " news");
    }
    
    @Override
    public String getObserverName() {
        return subscriberName;
    }
}

// Demo class
class ObserverPatternDemo {
    public static void main(String[] args) {
        System.out.println("=== Observer Pattern Demo ===\\n");
        
        // 1. Stock Price Monitoring System
        System.out.println("1. Stock Price Monitoring System:");
        demonstrateStockMonitoring();
        
        // 2. News Subscription System
        System.out.println("\\n\\n2. News Subscription System:");
        demonstrateNewsSubscription();
    }
    
    private static void demonstrateStockMonitoring() {
        // Create stocks
        Stock appleStock = new Stock("AAPL", 150.00);
        Stock googleStock = new Stock("GOOGL", 2500.00);
        
        // Create observers
        StockDisplay mobileDisplay = new StockDisplay("Mobile App", "Mobile");
        StockDisplay webDisplay = new StockDisplay("Web Dashboard", "Web");
        StockDisplay tvDisplay = new StockDisplay("TV Display", "Television");
        
        StockAlert highAlert = new StockAlert("High Price Alert", 155.00, "above");
        StockAlert lowAlert = new StockAlert("Low Price Alert", 145.00, "below");
        
        StockLogger transactionLogger = new StockLogger("Transaction Logger");
        StockLogger auditLogger = new StockLogger("Audit Logger");
        
        // Attach observers to Apple stock
        appleStock.attach(mobileDisplay);
        appleStock.attach(webDisplay);
        appleStock.attach(highAlert);
        appleStock.attach(lowAlert);
        appleStock.attach(transactionLogger);
        
        // Attach some observers to Google stock
        googleStock.attach(tvDisplay);
        googleStock.attach(auditLogger);
        
        // Simulate price changes
        System.out.println("\\n=== Price Changes Simulation ===");
        appleStock.setPrice(152.50); // Should trigger displays and logger
        appleStock.setPrice(158.00); // Should trigger high alert
        appleStock.setPrice(143.00); // Should trigger low alert
        
        googleStock.setPrice(2520.00); // Should trigger TV display and audit logger
        
        // Detach an observer
        System.out.println("\\n=== Detaching Observer ===");
        appleStock.detach(webDisplay);
        appleStock.setPrice(160.00); // Web display should not be notified
        
        // Print logs
        transactionLogger.printLogs();
        auditLogger.printLogs();
    }
    
    private static void demonstrateNewsSubscription() {
        // Create news agency
        NewsAgency newsAgency = new NewsAgency();
        
        // Create subscribers with different interests
        NewsSubscriber techSubscriber = new NewsSubscriber("Tech Enthusiast", "TECHNOLOGY", "SCIENCE");
        NewsSubscriber businessSubscriber = new NewsSubscriber("Business Analyst", "BUSINESS", "FINANCE");
        NewsSubscriber generalSubscriber = new NewsSubscriber("General Reader", "ALL");
        NewsSubscriber sportsSubscriber = new NewsSubscriber("Sports Fan", "SPORTS");
        
        // Subscribe to news agency
        newsAgency.attach(techSubscriber);
        newsAgency.attach(businessSubscriber);
        newsAgency.attach(generalSubscriber);
        newsAgency.attach(sportsSubscriber);
        
        // Publish different types of news
        System.out.println("\\n=== Publishing News ===");
        newsAgency.publishNews("Apple releases new iPhone with advanced AI features", "TECHNOLOGY");
        newsAgency.publishNews("Stock market reaches new highs amid tech rally", "BUSINESS");
        newsAgency.publishNews("Scientists discover new exoplanet in habitable zone", "SCIENCE");
        newsAgency.publishNews("Local team wins championship in thrilling final", "SPORTS");
        
        // Modify subscriber interests
        System.out.println("\\n=== Modifying Interests ===");
        techSubscriber.addInterest("BUSINESS");
        sportsSubscriber.removeInterest("SPORTS");
        sportsSubscriber.addInterest("TECHNOLOGY");
        
        // Publish more news
        System.out.println("\\n=== Publishing More News ===");
        newsAgency.publishNews("Major tech company announces acquisition deal", "BUSINESS");
        newsAgency.publishNews("New breakthrough in quantum computing achieved", "TECHNOLOGY");
        
        // Unsubscribe a subscriber
        System.out.println("\\n=== Unsubscribing ===");
        newsAgency.detach(generalSubscriber);
        newsAgency.publishNews("Breaking: Economic policy changes announced", "POLITICS");
    }
}

/*
 * Observer Pattern Benefits:
 * 
 * ✅ Advantages:
 * - Loose coupling between subjects and observers
 * - Dynamic relationships - observers can be added/removed at runtime
 * - Supports broadcast communication
 * - Adheres to Open/Closed Principle
 * - Supports the principle of separation of concerns
 * 
 * ❌ Disadvantages:
 * - Can cause memory leaks if observers are not properly detached
 * - Order of notification is not guaranteed
 * - Can lead to unexpected cascading updates
 * - Debugging can be complex due to indirect relationships
 * 
 * When to use:
 * - When changes to one object require changing many objects
 * - When an object should notify other objects without knowing who they are
 * - When you need to maintain consistency between related objects
 * - In event-driven systems and MVC architectures
 * - For implementing distributed event handling systems
 */
