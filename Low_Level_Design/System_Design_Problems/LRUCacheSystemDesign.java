// LRU Cache - System Design Problem Implementation

/**
 * LRU (Least Recently Used) Cache Implementation
 * 
 * Real-world Use Case: Web Application Cache System
 * - Cache frequently accessed web pages and API responses
 * - Automatic eviction of least recently used items when capacity is full
 * - O(1) time complexity for get and put operations
 * - Thread-safe implementation for concurrent access
 * - Monitoring and statistics for cache performance
 * 
 * Key Components:
 * 1. HashMap - O(1) access to cache entries
 * 2. Doubly Linked List - O(1) insertion/deletion for LRU ordering
 * 3. Thread Safety - Synchronized methods or concurrent data structures
 * 4. Generic Implementation - Support any key-value types
 * 5. Eviction Policy - LRU algorithm implementation
 */

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Cache statistics for monitoring
class CacheStats {
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    private final AtomicLong puts = new AtomicLong(0);
    private final LocalDateTime startTime = LocalDateTime.now();
    
    public void recordHit() { hits.incrementAndGet(); }
    public void recordMiss() { misses.incrementAndGet(); }
    public void recordEviction() { evictions.incrementAndGet(); }
    public void recordPut() { puts.incrementAndGet(); }
    
    public long getHits() { return hits.get(); }
    public long getMisses() { return misses.get(); }
    public long getEvictions() { return evictions.get(); }
    public long getPuts() { return puts.get(); }
    
    public double getHitRate() {
        long totalRequests = hits.get() + misses.get();
        return totalRequests == 0 ? 0.0 : (double) hits.get() / totalRequests;
    }
    
    public LocalDateTime getStartTime() { return startTime; }
    
    public void reset() {
        hits.set(0);
        misses.set(0);
        evictions.set(0);
        puts.set(0);
    }
    
    @Override
    public String toString() {
        return String.format(
            "CacheStats{hits=%d, misses=%d, evictions=%d, puts=%d, hitRate=%.2f%%, uptime=%s}",
            getHits(), getMisses(), getEvictions(), getPuts(), 
            getHitRate() * 100, 
            java.time.Duration.between(startTime, LocalDateTime.now())
        );
    }
}

// Cache entry with metadata
class CacheEntry<V> {
    private final V value;
    private final LocalDateTime createdAt;
    private volatile LocalDateTime lastAccessedAt;
    private final long size; // For size-based eviction (optional)
    
    public CacheEntry(V value, long size) {
        this.value = value;
        this.size = size;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = createdAt;
    }
    
    public CacheEntry(V value) {
        this(value, 1); // Default size
    }
    
    public V getValue() {
        this.lastAccessedAt = LocalDateTime.now();
        return value;
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public long getSize() { return size; }
}

// Node for doubly linked list
class DLLNode<K, V> {
    K key;
    CacheEntry<V> entry;
    DLLNode<K, V> prev;
    DLLNode<K, V> next;
    
    public DLLNode(K key, CacheEntry<V> entry) {
        this.key = key;
        this.entry = entry;
    }
    
    public DLLNode() {
        // Sentinel node
    }
}

// Thread-safe LRU Cache implementation
public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, DLLNode<K, V>> cache;
    private final DLLNode<K, V> head; // Most recently used
    private final DLLNode<K, V> tail; // Least recently used
    private final ReadWriteLock lock;
    private final CacheStats stats;
    private volatile int currentSize;
    
    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.stats = new CacheStats();
        this.currentSize = 0;
        
        // Initialize sentinel nodes
        this.head = new DLLNode<>();
        this.tail = new DLLNode<>();
        head.next = tail;
        tail.prev = head;
    }
    
    /**
     * Get value from cache - O(1) time complexity
     */
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        lock.readLock().lock();
        try {
            DLLNode<K, V> node = cache.get(key);
            if (node == null) {
                stats.recordMiss();
                return null;
            }
            
            // Move to front (most recently used)
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                // Double-check in case another thread modified
                node = cache.get(key);
                if (node != null) {
                    moveToHead(node);
                    stats.recordHit();
                    return node.entry.getValue();
                } else {
                    stats.recordMiss();
                    return null;
                }
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Put key-value pair in cache - O(1) time complexity
     */
    public void put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        CacheEntry<V> entry = new CacheEntry<>(value);
        
        lock.writeLock().lock();
        try {
            DLLNode<K, V> existingNode = cache.get(key);
            
            if (existingNode != null) {
                // Update existing entry
                existingNode.entry = entry;
                moveToHead(existingNode);
            } else {
                // Add new entry
                DLLNode<K, V> newNode = new DLLNode<>(key, entry);
                
                if (currentSize >= capacity) {
                    // Remove least recently used
                    removeLRU();
                }
                
                cache.put(key, newNode);
                addToHead(newNode);
                currentSize++;
            }
            
            stats.recordPut();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Remove key from cache
     */
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            DLLNode<K, V> node = cache.get(key);
            if (node != null) {
                cache.remove(key);
                removeNode(node);
                currentSize--;
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Check if key exists in cache
     */
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get current cache size
     */
    public int size() {
        return currentSize;
    }
    
    /**
     * Check if cache is empty
     */
    public boolean isEmpty() {
        return currentSize == 0;
    }
    
    /**
     * Get cache capacity
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Clear all entries from cache
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            head.next = tail;
            tail.prev = head;
            currentSize = 0;
            stats.reset();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        return stats;
    }
    
    /**
     * Get all keys in LRU order (most recent first)
     */
    public List<K> getKeysInLRUOrder() {
        lock.readLock().lock();
        try {
            List<K> keys = new ArrayList<>();
            DLLNode<K, V> current = head.next;
            while (current != tail) {
                keys.add(current.key);
                current = current.next;
            }
            return keys;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get cache state for debugging
     */
    public Map<K, LocalDateTime> getCacheState() {
        lock.readLock().lock();
        try {
            Map<K, LocalDateTime> state = new LinkedHashMap<>();
            DLLNode<K, V> current = head.next;
            while (current != tail) {
                state.put(current.key, current.entry.getLastAccessedAt());
                current = current.next;
            }
            return state;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Private helper methods
    
    private void addToHead(DLLNode<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(DLLNode<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private void moveToHead(DLLNode<K, V> node) {
        removeNode(node);
        addToHead(node);
    }
    
    private void removeLRU() {
        DLLNode<K, V> lru = tail.prev;
        if (lru != head) {
            cache.remove(lru.key);
            removeNode(lru);
            currentSize--;
            stats.recordEviction();
        }
    }
    
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return String.format("LRUCache{capacity=%d, size=%d, hitRate=%.2f%%}", 
                capacity, currentSize, stats.getHitRate() * 100);
        } finally {
            lock.readLock().unlock();
        }
    }
}

// Web page cache entry for demonstration
class WebPage {
    private final String url;
    private final String title;
    private final String content;
    private final long contentLength;
    private final LocalDateTime lastModified;
    
    public WebPage(String url, String title, String content) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.contentLength = content.length();
        this.lastModified = LocalDateTime.now();
    }
    
    // Getters
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public long getContentLength() { return contentLength; }
    public LocalDateTime getLastModified() { return lastModified; }
    
    @Override
    public String toString() {
        return String.format("WebPage{url='%s', title='%s', size=%d bytes}", 
            url, title, contentLength);
    }
}

// API response cache entry
class APIResponse {
    private final String endpoint;
    private final Map<String, Object> data;
    private final int statusCode;
    private final LocalDateTime timestamp;
    
    public APIResponse(String endpoint, Map<String, Object> data, int statusCode) {
        this.endpoint = endpoint;
        this.data = new HashMap<>(data);
        this.statusCode = statusCode;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
    public String getEndpoint() { return endpoint; }
    public Map<String, Object> getData() { return new HashMap<>(data); }
    public int getStatusCode() { return statusCode; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("APIResponse{endpoint='%s', status=%d, dataSize=%d}", 
            endpoint, statusCode, data.size());
    }
}

// Cache performance tester
class CachePerformanceTester {
    
    public static void testCachePerformance(LRUCache<String, String> cache, int operations) {
        System.out.println(String.format("🚀 Running performance test with %d operations...", operations));
        
        long startTime = System.nanoTime();
        
        // Warm up phase
        for (int i = 0; i < 1000; i++) {
            cache.put("warm_" + i, "value_" + i);
        }
        
        // Performance test phase
        Random random = new Random();
        
        for (int i = 0; i < operations; i++) {
            String key = "key_" + random.nextInt(operations);
            
            if (random.nextBoolean()) {
                // Read operation
                cache.get(key);
            } else {
                // Write operation
                cache.put(key, "value_" + i);
            }
        }
        
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.println(String.format("✅ Performance test completed in %.2f ms", durationMs));
        System.out.println(String.format("📊 Operations per second: %.0f", operations / (durationMs / 1000)));
        System.out.println("📈 " + cache.getStats());
    }
    
    public static void testConcurrency(LRUCache<String, String> cache, int numThreads) {
        System.out.println(String.format("🔄 Testing concurrency with %d threads...", numThreads));
        
        Thread[] threads = new Thread[numThreads];
        final int operationsPerThread = 1000;
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                Random random = new Random();
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "thread_" + threadId + "_key_" + j;
                    
                    if (random.nextBoolean()) {
                        cache.get(key);
                    } else {
                        cache.put(key, "thread_" + threadId + "_value_" + j);
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.println(String.format("✅ Concurrency test completed in %.2f ms", durationMs));
        System.out.println(String.format("📊 Total operations: %d", numThreads * operationsPerThread));
        System.out.println("📈 " + cache.getStats());
    }
}

// Main demonstration class
public class LRUCacheSystemDesign {
    public static void main(String[] args) {
        System.out.println("💾 LRU CACHE SYSTEM - Design Problem Demo");
        System.out.println("==========================================\\n");
        
        // Test 1: Basic LRU Cache Operations
        System.out.println("🔧 TESTING BASIC OPERATIONS");
        System.out.println("===========================");
        
        LRUCache<String, String> basicCache = new LRUCache<>(3);
        
        System.out.println("\\n--- Adding entries to cache (capacity: 3) ---");
        basicCache.put("key1", "value1");
        basicCache.put("key2", "value2");
        basicCache.put("key3", "value3");
        
        System.out.println("Cache keys in LRU order: " + basicCache.getKeysInLRUOrder());
        System.out.println("Cache state: " + basicCache);
        
        System.out.println("\\n--- Accessing key1 (moves to front) ---");
        String value = basicCache.get("key1");
        System.out.println("Retrieved: " + value);
        System.out.println("Cache keys in LRU order: " + basicCache.getKeysInLRUOrder());
        
        System.out.println("\\n--- Adding key4 (should evict key2) ---");
        basicCache.put("key4", "value4");
        System.out.println("Cache keys in LRU order: " + basicCache.getKeysInLRUOrder());
        System.out.println("key2 exists: " + basicCache.containsKey("key2"));
        System.out.println("key4 exists: " + basicCache.containsKey("key4"));
        
        // Test 2: Web Page Cache
        System.out.println("\\n\\n🌐 TESTING WEB PAGE CACHE");
        System.out.println("==========================");
        
        LRUCache<String, WebPage> webCache = new LRUCache<>(5);
        
        // Simulate web page caching
        WebPage[] pages = {
            new WebPage("/home", "Home Page", "Welcome to our website..."),
            new WebPage("/about", "About Us", "Learn more about our company..."),
            new WebPage("/products", "Products", "Browse our product catalog..."),
            new WebPage("/contact", "Contact", "Get in touch with us..."),
            new WebPage("/blog", "Blog", "Read our latest articles...")
        };
        
        System.out.println("\\n--- Caching web pages ---");
        for (WebPage page : pages) {
            webCache.put(page.getUrl(), page);
            System.out.println("Cached: " + page);
        }
        
        System.out.println("\\nWeb cache state: " + webCache);
        System.out.println("Cached URLs: " + webCache.getKeysInLRUOrder());
        
        System.out.println("\\n--- Simulating page access patterns ---");
        // Simulate frequent access to home and products pages
        for (int i = 0; i < 3; i++) {
            webCache.get("/home");
            webCache.get("/products");
        }
        
        System.out.println("After access patterns: " + webCache.getKeysInLRUOrder());
        
        // Add new pages (should evict least used)
        WebPage newsPage = new WebPage("/news", "News", "Latest news and updates...");
        WebPage faqPage = new WebPage("/faq", "FAQ", "Frequently asked questions...");
        
        webCache.put("/news", newsPage);
        webCache.put("/faq", faqPage);
        
        System.out.println("After adding new pages: " + webCache.getKeysInLRUOrder());
        
        // Test 3: API Response Cache
        System.out.println("\\n\\n🔗 TESTING API RESPONSE CACHE");
        System.out.println("==============================");
        
        LRUCache<String, APIResponse> apiCache = new LRUCache<>(4);
        
        // Simulate API response caching
        Map<String, Object> userData = Map.of("id", 1, "name", "John Doe", "email", "john@example.com");
        Map<String, Object> orderData = Map.of("orderId", 12345, "total", 99.99, "status", "shipped");
        Map<String, Object> productData = Map.of("productId", 678, "name", "Laptop", "price", 1299.99);
        
        apiCache.put("/api/users/1", new APIResponse("/api/users/1", userData, 200));
        apiCache.put("/api/orders/12345", new APIResponse("/api/orders/12345", orderData, 200));
        apiCache.put("/api/products/678", new APIResponse("/api/products/678", productData, 200));
        
        System.out.println("\\n--- API responses cached ---");
        System.out.println("Cached endpoints: " + apiCache.getKeysInLRUOrder());
        
        // Simulate API access
        APIResponse response = apiCache.get("/api/users/1");
        if (response != null) {
            System.out.println("Cache hit for user data: " + response);
        }
        
        // Cache miss simulation
        APIResponse missingResponse = apiCache.get("/api/users/999");
        System.out.println("Cache miss for non-existent user: " + (missingResponse == null ? "null" : missingResponse));
        
        System.out.println("\\nAPI cache stats: " + apiCache.getStats());
        
        // Test 4: Performance Testing
        System.out.println("\\n\\n⚡ PERFORMANCE TESTING");
        System.out.println("======================");
        
        LRUCache<String, String> perfCache = new LRUCache<>(1000);
        CachePerformanceTester.testCachePerformance(perfCache, 10000);
        
        // Test 5: Concurrency Testing
        System.out.println("\\n\\n🔄 CONCURRENCY TESTING");
        System.out.println("======================");
        
        LRUCache<String, String> concurrentCache = new LRUCache<>(500);
        CachePerformanceTester.testConcurrency(concurrentCache, 10);
        
        // Test 6: Cache State Analysis
        System.out.println("\\n\\n📊 CACHE STATE ANALYSIS");
        System.out.println("=======================");
        
        System.out.println("\\nBasic Cache Final State:");
        System.out.println("Size: " + basicCache.size() + "/" + basicCache.getCapacity());
        System.out.println("Stats: " + basicCache.getStats());
        
        System.out.println("\\nWeb Cache Final State:");
        System.out.println("Size: " + webCache.size() + "/" + webCache.getCapacity());
        System.out.println("Stats: " + webCache.getStats());
        Map<String, LocalDateTime> webCacheState = webCache.getCacheState();
        webCacheState.forEach((url, lastAccess) -> 
            System.out.println("  " + url + " -> last accessed: " + 
                lastAccess.format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        
        System.out.println("\\nAPI Cache Final State:");
        System.out.println("Size: " + apiCache.size() + "/" + apiCache.getCapacity());
        System.out.println("Stats: " + apiCache.getStats());
        
        System.out.println("\\n✅ LRU Cache System Design Demo Complete!");
        
        System.out.println("\\n📚 KEY CONCEPTS DEMONSTRATED:");
        System.out.println("• O(1) Time Complexity - Both get and put operations");
        System.out.println("• Thread Safety - Concurrent access with ReadWriteLock");
        System.out.println("• LRU Eviction Policy - Automatic removal of least recently used items");
        System.out.println("• Generic Implementation - Support for any key-value types");
        System.out.println("• Cache Statistics - Hit rate, miss rate, and eviction tracking");
        System.out.println("• Real-world Applications - Web pages, API responses, data caching");
        System.out.println("• Performance Monitoring - Built-in metrics and state inspection");
        System.out.println("• Memory Management - Efficient space utilization with capacity limits");
    }
}
