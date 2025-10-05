/**
 * LRU Cache Implementation
 * 
 * Design and implement a data structure for Least Recently Used (LRU) cache.
 * It should support get and put operations.
 * 
 * Requirements:
 * - get(key): Return the value if key exists, otherwise return -1
 * - put(key, value): Insert or update the value. If cache reaches capacity,
 *   remove the least recently used item before inserting new item
 * - Both operations should run in O(1) average time complexity
 * 
 * Real-world applications:
 * - CPU caches
 * - Web browser caches
 * - Database buffer pools
 * - Operating system page replacement
 */

import java.util.*;

/**
 * LRU Cache using HashMap and Doubly Linked List
 * Time Complexity: O(1) for both get and put operations
 * Space Complexity: O(capacity)
 */
class LRUCache {
    
    // Node class for doubly linked list
    class Node {
        int key;
        int value;
        Node prev;
        Node next;
        
        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private final int capacity;
    private final Map<Integer, Node> cache;
    private final Node head; // Dummy head (most recently used)
    private final Node tail; // Dummy tail (least recently used)
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        
        // Initialize dummy head and tail nodes
        this.head = new Node(-1, -1);
        this.tail = new Node(-1, -1);
        head.next = tail;
        tail.prev = head;
    }
    
    /**
     * Get value for the given key
     * If key exists, move it to front (most recently used)
     */
    public int get(int key) {
        Node node = cache.get(key);
        
        if (node == null) {
            return -1; // Key not found
        }
        
        // Move accessed node to front (mark as recently used)
        moveToFront(node);
        return node.value;
    }
    
    /**
     * Put key-value pair in cache
     * If key exists, update value and move to front
     * If cache is full, remove least recently used item
     */
    public void put(int key, int value) {
        Node existingNode = cache.get(key);
        
        if (existingNode != null) {
            // Key already exists, update value and move to front
            existingNode.value = value;
            moveToFront(existingNode);
            return;
        }
        
        // Key doesn't exist, create new node
        Node newNode = new Node(key, value);
        
        if (cache.size() >= capacity) {
            // Cache is full, remove least recently used item
            removeLRU();
        }
        
        // Add new node to cache and front of list
        cache.put(key, newNode);
        addToFront(newNode);
    }
    
    /**
     * Move existing node to front of list (mark as most recently used)
     */
    private void moveToFront(Node node) {
        removeNode(node);
        addToFront(node);
    }
    
    /**
     * Add node right after head (most recently used position)
     */
    private void addToFront(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }
    
    /**
     * Remove node from its current position in the list
     */
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    /**
     * Remove least recently used item (node just before tail)
     */
    private void removeLRU() {
        Node lruNode = tail.prev;
        cache.remove(lruNode.key);
        removeNode(lruNode);
    }
    
    /**
     * Get current cache size
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Check if cache is empty
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }
    
    /**
     * Clear all items from cache
     */
    public void clear() {
        cache.clear();
        head.next = tail;
        tail.prev = head;
    }
    
    /**
     * Get all keys in order from most recently used to least recently used
     */
    public List<Integer> getKeysInOrder() {
        List<Integer> keys = new ArrayList<>();
        Node current = head.next;
        
        while (current != tail) {
            keys.add(current.key);
            current = current.next;
        }
        
        return keys;
    }
    
    /**
     * Display cache state for debugging
     */
    public void displayCache() {
        System.out.println("Cache state (MRU -> LRU):");
        Node current = head.next;
        List<String> items = new ArrayList<>();
        
        while (current != tail) {
            items.add("(" + current.key + ":" + current.value + ")");
            current = current.next;
        }
        
        System.out.println(String.join(" -> ", items));
        System.out.println("Size: " + cache.size() + "/" + capacity);
    }
}

/**
 * Thread-Safe LRU Cache Implementation
 */
class ThreadSafeLRUCache {
    private final LRUCache cache;
    private final Object lock = new Object();
    
    public ThreadSafeLRUCache(int capacity) {
        this.cache = new LRUCache(capacity);
    }
    
    public int get(int key) {
        synchronized (lock) {
            return cache.get(key);
        }
    }
    
    public void put(int key, int value) {
        synchronized (lock) {
            cache.put(key, value);
        }
    }
    
    public int size() {
        synchronized (lock) {
            return cache.size();
        }
    }
    
    public void clear() {
        synchronized (lock) {
            cache.clear();
        }
    }
}

/**
 * Generic LRU Cache Implementation
 */
class GenericLRUCache<K, V> {
    
    class Node {
        K key;
        V value;
        Node prev;
        Node next;
        
        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private final int capacity;
    private final Map<K, Node> cache;
    private final Node head;
    private final Node tail;
    
    public GenericLRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.head = new Node(null, null);
        this.tail = new Node(null, null);
        head.next = tail;
        tail.prev = head;
    }
    
    public V get(K key) {
        Node node = cache.get(key);
        
        if (node == null) {
            return null;
        }
        
        moveToFront(node);
        return node.value;
    }
    
    public void put(K key, V value) {
        Node existingNode = cache.get(key);
        
        if (existingNode != null) {
            existingNode.value = value;
            moveToFront(existingNode);
            return;
        }
        
        Node newNode = new Node(key, value);
        
        if (cache.size() >= capacity) {
            removeLRU();
        }
        
        cache.put(key, newNode);
        addToFront(newNode);
    }
    
    private void moveToFront(Node node) {
        removeNode(node);
        addToFront(node);
    }
    
    private void addToFront(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private void removeLRU() {
        Node lruNode = tail.prev;
        cache.remove(lruNode.key);
        removeNode(lruNode);
    }
    
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }
    
    public int size() {
        return cache.size();
    }
    
    public boolean isEmpty() {
        return cache.isEmpty();
    }
    
    public Set<K> keySet() {
        return new HashSet<>(cache.keySet());
    }
}

/**
 * LRU Cache with TTL (Time To Live) support
 */
class LRUCacheWithTTL {
    
    class Node {
        int key;
        int value;
        long expirationTime;
        Node prev;
        Node next;
        
        Node(int key, int value, long ttlMs) {
            this.key = key;
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + ttlMs;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
    
    private final int capacity;
    private final long defaultTTL;
    private final Map<Integer, Node> cache;
    private final Node head;
    private final Node tail;
    
    public LRUCacheWithTTL(int capacity, long defaultTTLMs) {
        this.capacity = capacity;
        this.defaultTTL = defaultTTLMs;
        this.cache = new HashMap<>();
        this.head = new Node(-1, -1, 0);
        this.tail = new Node(-1, -1, 0);
        head.next = tail;
        tail.prev = head;
    }
    
    public int get(int key) {
        Node node = cache.get(key);
        
        if (node == null || node.isExpired()) {
            if (node != null) {
                // Remove expired node
                cache.remove(key);
                removeNode(node);
            }
            return -1;
        }
        
        moveToFront(node);
        return node.value;
    }
    
    public void put(int key, int value) {
        put(key, value, defaultTTL);
    }
    
    public void put(int key, int value, long ttlMs) {
        Node existingNode = cache.get(key);
        
        if (existingNode != null) {
            existingNode.value = value;
            existingNode.expirationTime = System.currentTimeMillis() + ttlMs;
            moveToFront(existingNode);
            return;
        }
        
        // Clean up expired entries
        cleanupExpired();
        
        Node newNode = new Node(key, value, ttlMs);
        
        if (cache.size() >= capacity) {
            removeLRU();
        }
        
        cache.put(key, newNode);
        addToFront(newNode);
    }
    
    private void cleanupExpired() {
        Iterator<Map.Entry<Integer, Node>> iterator = cache.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<Integer, Node> entry = iterator.next();
            Node node = entry.getValue();
            
            if (node.isExpired()) {
                iterator.remove();
                removeNode(node);
            }
        }
    }
    
    private void moveToFront(Node node) {
        removeNode(node);
        addToFront(node);
    }
    
    private void addToFront(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private void removeLRU() {
        Node lruNode = tail.prev;
        cache.remove(lruNode.key);
        removeNode(lruNode);
    }
}

/**
 * Demo and test class
 */
class LRUCacheDemo {
    public static void main(String[] args) {
        System.out.println("=== LRU Cache Demo ===\n");
        
        // Basic LRU Cache Demo
        System.out.println("1. Basic LRU Cache Operations:");
        demonstrateBasicLRU();
        
        // Cache eviction demo
        System.out.println("\n2. Cache Eviction Demo:");
        demonstrateCacheEviction();
        
        // Generic LRU Cache demo
        System.out.println("\n3. Generic LRU Cache Demo:");
        demonstrateGenericLRU();
        
        // TTL Cache demo
        System.out.println("\n4. LRU Cache with TTL Demo:");
        demonstrateTTLCache();
        
        // Performance test
        System.out.println("\n5. Performance Test:");
        performanceTest();
    }
    
    private static void demonstrateBasicLRU() {
        LRUCache cache = new LRUCache(3);
        
        // Add some items
        cache.put(1, 10);
        cache.put(2, 20);
        cache.put(3, 30);
        cache.displayCache();
        
        // Access item 1 (moves to front)
        System.out.println("Get key 1: " + cache.get(1));
        cache.displayCache();
        
        // Add new item (should evict least recently used)
        cache.put(4, 40);
        cache.displayCache();
        
        // Try to get evicted item
        System.out.println("Get key 2 (evicted): " + cache.get(2));
    }
    
    private static void demonstrateCacheEviction() {
        LRUCache cache = new LRUCache(2);
        
        cache.put(1, 1);
        cache.put(2, 2);
        System.out.println("Get 1: " + cache.get(1)); // returns 1
        
        cache.put(3, 3); // evicts key 2
        System.out.println("Get 2: " + cache.get(2)); // returns -1 (not found)
        
        cache.put(4, 4); // evicts key 1
        System.out.println("Get 1: " + cache.get(1)); // returns -1 (not found)
        System.out.println("Get 3: " + cache.get(3)); // returns 3
        System.out.println("Get 4: " + cache.get(4)); // returns 4
    }
    
    private static void demonstrateGenericLRU() {
        GenericLRUCache<String, String> cache = new GenericLRUCache<>(3);
        
        cache.put("user:1", "John Doe");
        cache.put("user:2", "Jane Smith");
        cache.put("user:3", "Bob Johnson");
        
        System.out.println("Get user:1: " + cache.get("user:1"));
        System.out.println("Cache size: " + cache.size());
        
        cache.put("user:4", "Alice Brown"); // Should evict user:2
        
        System.out.println("Get user:2 (evicted): " + cache.get("user:2"));
        System.out.println("Contains user:4: " + cache.containsKey("user:4"));
    }
    
    private static void demonstrateTTLCache() {
        LRUCacheWithTTL cache = new LRUCacheWithTTL(3, 2000); // 2 second TTL
        
        cache.put(1, 100);
        cache.put(2, 200);
        
        System.out.println("Get 1 immediately: " + cache.get(1));
        
        try {
            Thread.sleep(1000); // Wait 1 second
            System.out.println("Get 1 after 1s: " + cache.get(1));
            
            Thread.sleep(1500); // Wait another 1.5 seconds (total 2.5s)
            System.out.println("Get 1 after 2.5s (expired): " + cache.get(1));
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void performanceTest() {
        int cacheSize = 1000;
        int operations = 100000;
        
        LRUCache cache = new LRUCache(cacheSize);
        Random random = new Random();
        
        long startTime = System.currentTimeMillis();
        
        // Perform random operations
        for (int i = 0; i < operations; i++) {
            int key = random.nextInt(cacheSize * 2); // 50% hit rate expected
            
            if (random.nextBoolean()) {
                // Put operation
                cache.put(key, key * 10);
            } else {
                // Get operation
                cache.get(key);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("Performance Test Results:");
        System.out.println("Operations: " + operations);
        System.out.println("Cache Size: " + cacheSize);
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Operations per second: " + (operations * 1000L / duration));
        System.out.println("Average time per operation: " + (duration * 1000.0 / operations) + "μs");
    }
}

/*
 * Key Design Decisions and Trade-offs:
 * 
 * 1. Data Structure Choice:
 *    - HashMap + Doubly Linked List provides O(1) operations
 *    - HashMap for fast key lookup
 *    - Doubly linked list for efficient insertion/deletion
 * 
 * 2. Memory vs Performance:
 *    - Extra memory for prev/next pointers
 *    - Dummy head/tail nodes simplify edge cases
 *    - Trade memory for code simplicity and performance
 * 
 * 3. Thread Safety:
 *    - Basic implementation is not thread-safe
 *    - ThreadSafeLRUCache shows synchronized wrapper
 *    - Could use concurrent data structures for better performance
 * 
 * 4. Generic Implementation:
 *    - Supports any key-value types
 *    - Type safety at compile time
 *    - Slightly more complex but more reusable
 * 
 * 5. TTL Support:
 *    - Adds expiration capability
 *    - Cleanup can be lazy or scheduled
 *    - Balance between accuracy and performance
 * 
 * Real-world Considerations:
 * - Monitoring cache hit rates
 * - Implementing cache warming strategies
 * - Handling cache stampede scenarios
 * - Persistence and recovery mechanisms
 * - Distributed caching with consistency
 */
