# Java Core Concepts - Interview Questions & Answers

## Table of Contents
1. [Object-Oriented Programming](#object-oriented-programming)
2. [Collections Framework](#collections-framework)
3. [Multithreading & Concurrency](#multithreading--concurrency)
4. [Java 8+ Features](#java-8-features)
5. [Memory Management](#memory-management)
6. [Exception Handling](#exception-handling)

---

## Object-Oriented Programming

### Q1: Explain the four pillars of OOP with real-world examples

**Answer:**

#### 1. Encapsulation
Bundling data and methods that operate on that data within a single unit (class).

```java
public class BankAccount {
    private double balance;  // Private data
    private String accountNumber;
    
    // Public methods to access private data
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            logTransaction("Deposit", amount);
        } else {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
    
    public boolean withdraw(double amount) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
            logTransaction("Withdrawal", amount);
            return true;
        }
        return false;
    }
    
    public double getBalance() {
        return balance;
    }
    
    private void logTransaction(String type, double amount) {
        System.out.println(type + " of $" + amount + " processed");
    }
}
```

**Benefits:**
- Data hiding and security
- Code modularity and maintainability
- Control over data access

#### 2. Inheritance
Mechanism where a new class acquires properties and behaviors of an existing class.

```java
// Base class
public abstract class Vehicle {
    protected String brand;
    protected String model;
    protected int year;
    
    public Vehicle(String brand, String model, int year) {
        this.brand = brand;
        this.model = model;
        this.year = year;
    }
    
    public void start() {
        System.out.println(brand + " " + model + " is starting...");
    }
    
    public abstract double getFuelEfficiency();
    public abstract void displaySpecs();
}

// Derived class
public class Car extends Vehicle {
    private int numberOfDoors;
    private double engineSize;
    
    public Car(String brand, String model, int year, int doors, double engineSize) {
        super(brand, model, year);
        this.numberOfDoors = doors;
        this.engineSize = engineSize;
    }
    
    @Override
    public double getFuelEfficiency() {
        // Car-specific fuel efficiency calculation
        return 25.0 - (engineSize * 2);
    }
    
    @Override
    public void displaySpecs() {
        System.out.println("Car: " + brand + " " + model + " (" + year + ")");
        System.out.println("Doors: " + numberOfDoors + ", Engine: " + engineSize + "L");
    }
    
    // Car-specific method
    public void honk() {
        System.out.println("Beep beep!");
    }
}

public class Truck extends Vehicle {
    private double cargoCapacity;
    
    public Truck(String brand, String model, int year, double cargoCapacity) {
        super(brand, model, year);
        this.cargoCapacity = cargoCapacity;
    }
    
    @Override
    public double getFuelEfficiency() {
        // Truck-specific fuel efficiency calculation
        return 15.0 - (cargoCapacity / 1000);
    }
    
    @Override
    public void displaySpecs() {
        System.out.println("Truck: " + brand + " " + model + " (" + year + ")");
        System.out.println("Cargo Capacity: " + cargoCapacity + " lbs");
    }
}
```

#### 3. Polymorphism
Ability of objects to take multiple forms.

**Compile-time Polymorphism (Method Overloading):**
```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
    
    public double add(double a, double b) {
        return a + b;
    }
    
    public int add(int a, int b, int c) {
        return a + b + c;
    }
    
    public String add(String a, String b) {
        return a + b;
    }
}
```

**Runtime Polymorphism (Method Overriding):**
```java
public class VehicleDemo {
    public static void main(String[] args) {
        Vehicle[] vehicles = {
            new Car("Toyota", "Camry", 2023, 4, 2.5),
            new Truck("Ford", "F-150", 2023, 8000)
        };
        
        for (Vehicle vehicle : vehicles) {
            vehicle.start();           // Common method
            vehicle.displaySpecs();    // Polymorphic behavior
            System.out.println("Fuel Efficiency: " + vehicle.getFuelEfficiency());
            System.out.println("---");
        }
    }
}
```

#### 4. Abstraction
Hiding implementation details while showing only essential features.

```java
// Abstract class example
public abstract class PaymentProcessor {
    protected String merchantId;
    protected double transactionFee;
    
    public PaymentProcessor(String merchantId, double transactionFee) {
        this.merchantId = merchantId;
        this.transactionFee = transactionFee;
    }
    
    // Template method pattern
    public final PaymentResult processPayment(double amount) {
        if (!validateAmount(amount)) {
            return new PaymentResult(false, "Invalid amount");
        }
        
        double totalAmount = amount + calculateFees(amount);
        boolean success = executePayment(totalAmount);
        
        if (success) {
            logTransaction(amount, totalAmount);
            sendConfirmation();
        }
        
        return new PaymentResult(success, success ? "Payment successful" : "Payment failed");
    }
    
    // Abstract methods to be implemented by subclasses
    protected abstract boolean executePayment(double amount);
    protected abstract void logTransaction(double amount, double totalAmount);
    
    // Concrete methods
    private boolean validateAmount(double amount) {
        return amount > 0 && amount <= 10000;
    }
    
    private double calculateFees(double amount) {
        return amount * transactionFee;
    }
    
    private void sendConfirmation() {
        System.out.println("Payment confirmation sent");
    }
}

// Interface example
public interface Drawable {
    void draw();
    void resize(double factor);
    
    // Default method (Java 8+)
    default void print() {
        System.out.println("Printing drawable object...");
    }
    
    // Static method (Java 8+)
    static void info() {
        System.out.println("Drawable interface v2.0");
    }
}
```

### Q2: What's the difference between Abstract Classes and Interfaces?

**Answer:**

| Feature | Abstract Class | Interface |
|---------|----------------|-----------|
| **Multiple Inheritance** | No (single inheritance) | Yes (multiple interface implementation) |
| **Method Types** | Abstract + Concrete methods | Abstract + Default + Static (Java 8+) |
| **Variables** | All types (instance, static, final) | public static final only |
| **Constructors** | Can have constructors | Cannot have constructors |
| **Access Modifiers** | All access modifiers | public only (methods) |
| **When to Use** | "IS-A" relationship, shared code | "CAN-DO" relationship, contract |

**Example Scenario:**
```java
// Abstract class for shared behavior
public abstract class Animal {
    protected String name;
    protected int age;
    
    public Animal(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // Concrete method
    public void sleep() {
        System.out.println(name + " is sleeping");
    }
    
    // Abstract method
    public abstract void makeSound();
}

// Interfaces for capabilities
public interface Flyable {
    double MAX_ALTITUDE = 10000; // public static final
    
    void fly();
    
    default void land() {
        System.out.println("Landing safely");
    }
}

public interface Swimmable {
    void swim();
}

// Implementation
public class Duck extends Animal implements Flyable, Swimmable {
    public Duck(String name, int age) {
        super(name, age);
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + " says: Quack!");
    }
    
    @Override
    public void fly() {
        System.out.println(name + " is flying at " + MAX_ALTITUDE + " feet");
    }
    
    @Override
    public void swim() {
        System.out.println(name + " is swimming in the pond");
    }
}
```

---

## Collections Framework

### Q3: Explain the Java Collections hierarchy and when to use each collection

**Answer:**

#### Collections Hierarchy:
```
Collection (Interface)
├── List (Interface)
│   ├── ArrayList (Class)
│   ├── LinkedList (Class)
│   └── Vector (Class)
├── Set (Interface)
│   ├── HashSet (Class)
│   ├── LinkedHashSet (Class)
│   └── TreeSet (Class)
└── Queue (Interface)
    ├── PriorityQueue (Class)
    └── Deque (Interface)
        └── ArrayDeque (Class)

Map (Interface) - Not part of Collection
├── HashMap (Class)
├── LinkedHashMap (Class)
├── TreeMap (Class)
└── Hashtable (Class)
```

#### Detailed Comparison:

**List Implementations:**
```java
public class ListComparison {
    public static void demonstrateListPerformance() {
        // ArrayList - Dynamic array, good for random access
        List<Integer> arrayList = new ArrayList<>();
        // Best for: Frequent read operations, random access
        // Time Complexity: get(i) = O(1), add = O(1) amortized, insert = O(n)
        
        // LinkedList - Doubly linked list, good for insertions
        List<Integer> linkedList = new LinkedList<>();
        // Best for: Frequent insertions/deletions, unknown size
        // Time Complexity: get(i) = O(n), add = O(1), insert = O(1) if you have reference
        
        // Performance comparison
        long start, end;
        int n = 100000;
        
        // ArrayList insertion at beginning (worst case)
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            arrayList.add(0, i); // O(n) operation
        }
        end = System.nanoTime();
        System.out.println("ArrayList insert at beginning: " + (end - start) / 1000000 + "ms");
        
        // LinkedList insertion at beginning (best case)
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            linkedList.add(0, i); // O(1) operation
        }
        end = System.nanoTime();
        System.out.println("LinkedList insert at beginning: " + (end - start) / 1000000 + "ms");
    }
}
```

**Set Implementations:**
```java
public class SetComparison {
    public static void demonstrateSetBehavior() {
        // HashSet - Hash table, no ordering
        Set<String> hashSet = new HashSet<>();
        hashSet.addAll(Arrays.asList("banana", "apple", "cherry", "date"));
        System.out.println("HashSet: " + hashSet); // Unordered
        
        // LinkedHashSet - Hash table + linked list, insertion order
        Set<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.addAll(Arrays.asList("banana", "apple", "cherry", "date"));
        System.out.println("LinkedHashSet: " + linkedHashSet); // Insertion order
        
        // TreeSet - Red-black tree, sorted order
        Set<String> treeSet = new TreeSet<>();
        treeSet.addAll(Arrays.asList("banana", "apple", "cherry", "date"));
        System.out.println("TreeSet: " + treeSet); // Sorted order
        
        // Performance comparison
        // HashSet: add/remove/contains = O(1) average
        // LinkedHashSet: add/remove/contains = O(1) average, maintains order
        // TreeSet: add/remove/contains = O(log n)
    }
}
```

**Map Implementations:**
```java
public class MapComparison {
    public static void demonstrateMapBehavior() {
        // HashMap - Hash table, no ordering, allows null
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("John", 25);
        hashMap.put("Jane", 30);
        hashMap.put(null, 35); // Allows null key
        
        // LinkedHashMap - Hash table + linked list, insertion/access order
        Map<String, Integer> linkedHashMap = new LinkedHashMap<>(16, 0.75f, true); // access order
        linkedHashMap.put("John", 25);
        linkedHashMap.put("Jane", 30);
        linkedHashMap.put("Bob", 35);
        linkedHashMap.get("John"); // Moves John to end in access order mode
        
        // TreeMap - Red-black tree, sorted by keys
        Map<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("Charlie", 40);
        treeMap.put("Alice", 20);
        treeMap.put("Bob", 30);
        System.out.println("TreeMap: " + treeMap); // Sorted by keys
        
        // Hashtable - Synchronized HashMap, no null keys/values
        Map<String, Integer> hashtable = new Hashtable<>();
        hashtable.put("Thread1", 100);
        hashtable.put("Thread2", 200);
        // hashtable.put(null, 300); // Would throw NullPointerException
    }
}
```

### Q4: How does HashMap work internally? Explain collision handling.

**Answer:**

#### HashMap Internal Structure:

```java
public class HashMapInternals {
    // Simplified HashMap structure
    private static class Node<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next; // For chaining
        
        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }
    
    private Node<K, V>[] table; // Array of buckets
    private int size;
    private int threshold; // Resize threshold
    private final float loadFactor = 0.75f;
    
    // Hash function
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
    
    // Get bucket index
    static int indexFor(int hash, int length) {
        return hash & (length - 1); // Equivalent to hash % length for power of 2
    }
    
    public V put(K key, V value) {
        int hash = hash(key);
        int index = indexFor(hash, table.length);
        
        // Check if key already exists
        for (Node<K, V> node = table[index]; node != null; node = node.next) {
            if (node.hash == hash && Objects.equals(key, node.key)) {
                V oldValue = node.value;
                node.value = value;
                return oldValue;
            }
        }
        
        // Add new node
        addNode(hash, key, value, index);
        return null;
    }
    
    private void addNode(int hash, K key, V value, int bucketIndex) {
        Node<K, V> newNode = new Node<>(hash, key, value, table[bucketIndex]);
        table[bucketIndex] = newNode;
        
        if (++size > threshold) {
            resize();
        }
    }
}
```

#### Collision Handling Evolution:

**Java 7 and Earlier - Chaining:**
```java
// Bucket 0: [Node1] -> [Node2] -> [Node3] -> null
// All nodes with same hash index form a linked list
```

**Java 8+ - Chaining + Tree Conversion:**
```java
public class Java8HashMapFeatures {
    // When bucket has > 8 nodes, convert to Red-Black Tree
    // When bucket has < 6 nodes, convert back to linked list
    
    static final int TREEIFY_THRESHOLD = 8;
    static final int UNTREEIFY_THRESHOLD = 6;
    
    // Tree node for buckets with many collisions
    static final class TreeNode<K, V> extends Node<K, V> {
        TreeNode<K, V> parent;
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        TreeNode<K, V> prev;
        boolean red;
        
        // Tree operations provide O(log n) instead of O(n) for large buckets
    }
}
```

#### Load Factor and Resizing:

```java
public class HashMapResizing {
    public void demonstrateResizing() {
        Map<Integer, String> map = new HashMap<>(4); // Initial capacity 4
        
        // Load factor 0.75 means resize when 75% full
        // Capacity 4 * 0.75 = 3, so resize after 3 elements
        
        map.put(1, "One");   // Size 1
        map.put(2, "Two");   // Size 2
        map.put(3, "Three"); // Size 3 - triggers resize to capacity 8
        map.put(4, "Four");  // Size 4 in new larger array
        
        // Resizing process:
        // 1. Create new array with double capacity
        // 2. Rehash all existing entries
        // 3. Update threshold
    }
    
    // Custom hash function example
    public static class BadHashExample {
        private int value;
        
        @Override
        public int hashCode() {
            return 1; // Terrible hash function - all objects go to same bucket!
        }
        
        // This would turn HashMap into a linked list - O(n) performance
    }
    
    public static class GoodHashExample {
        private String name;
        private int age;
        
        @Override
        public int hashCode() {
            return Objects.hash(name, age); // Good distribution
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            GoodHashExample that = (GoodHashExample) obj;
            return age == that.age && Objects.equals(name, that.name);
        }
    }
}
```

---

## Multithreading & Concurrency

### Q5: Explain different ways to create threads and their differences

**Answer:**

#### Method 1: Extending Thread Class
```java
public class ThreadExtension extends Thread {
    private String threadName;
    
    public ThreadExtension(String name) {
        this.threadName = name;
        System.out.println("Creating thread: " + name);
    }
    
    @Override
    public void run() {
        System.out.println("Thread " + threadName + " is running");
        
        try {
            for (int i = 1; i <= 5; i++) {
                System.out.println(threadName + " - Count: " + i);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Thread " + threadName + " interrupted");
        }
        
        System.out.println("Thread " + threadName + " exiting");
    }
}

// Usage
public class ThreadDemo1 {
    public static void main(String[] args) {
        ThreadExtension t1 = new ThreadExtension("Thread-1");
        ThreadExtension t2 = new ThreadExtension("Thread-2");
        
        t1.start(); // Start thread execution
        t2.start();
        
        // DON'T call run() directly - it won't create new thread
        // t1.run(); // This runs in main thread, not separate thread
    }
}
```

#### Method 2: Implementing Runnable Interface (Recommended)
```java
public class RunnableImplementation implements Runnable {
    private String threadName;
    private volatile boolean running = true;
    
    public RunnableImplementation(String name) {
        this.threadName = name;
    }
    
    @Override
    public void run() {
        System.out.println("Starting " + threadName);
        
        int count = 0;
        while (running && count < 10) {
            System.out.println(threadName + " - Count: " + count++);
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println(threadName + " interrupted");
                Thread.currentThread().interrupt(); // Restore interrupt status
                break;
            }
        }
        
        System.out.println(threadName + " finished");
    }
    
    public void stop() {
        running = false;
    }
}

// Usage with better control
public class ThreadDemo2 {
    public static void main(String[] args) throws InterruptedException {
        RunnableImplementation task1 = new RunnableImplementation("Worker-1");
        RunnableImplementation task2 = new RunnableImplementation("Worker-2");
        
        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        
        t1.start();
        t2.start();
        
        // Let threads run for 3 seconds, then stop them
        Thread.sleep(3000);
        task1.stop();
        task2.stop();
        
        // Wait for threads to finish
        t1.join();
        t2.join();
        
        System.out.println("Main thread finished");
    }
}
```

#### Method 3: Using Lambda Expressions (Java 8+)
```java
public class ModernThreadCreation {
    public static void main(String[] args) {
        // Lambda with Runnable
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Lambda Thread: " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // Method reference
        Thread t2 = new Thread(ModernThreadCreation::printNumbers);
        
        t1.start();
        t2.start();
    }
    
    public static void printNumbers() {
        for (int i = 10; i < 15; i++) {
            System.out.println("Method Reference Thread: " + i);
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

#### Method 4: Using ExecutorService (Best Practice)
```java
import java.util.concurrent.*;

public class ExecutorServiceExample {
    public static void main(String[] args) {
        // Fixed thread pool
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Submit tasks
        Future<String> future1 = executor.submit(() -> {
            Thread.sleep(2000);
            return "Task 1 completed by " + Thread.currentThread().getName();
        });
        
        Future<String> future2 = executor.submit(() -> {
            Thread.sleep(1000);
            return "Task 2 completed by " + Thread.currentThread().getName();
        });
        
        executor.submit(() -> {
            System.out.println("Task 3 running on " + Thread.currentThread().getName());
        });
        
        try {
            // Get results (blocking)
            System.out.println(future1.get());
            System.out.println(future2.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        
        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
```

### Q6: Explain synchronization mechanisms in Java

**Answer:**

#### 1. Synchronized Methods and Blocks

```java
public class BankAccount {
    private double balance;
    private final Object lock = new Object();
    
    // Synchronized method - locks entire object
    public synchronized void deposit(double amount) {
        System.out.println(Thread.currentThread().getName() + " depositing " + amount);
        balance += amount;
        System.out.println("New balance: " + balance);
    }
    
    // Synchronized block - more granular control
    public void withdraw(double amount) {
        synchronized (this) {
            if (balance >= amount) {
                System.out.println(Thread.currentThread().getName() + " withdrawing " + amount);
                balance -= amount;
                System.out.println("New balance: " + balance);
            } else {
                System.out.println("Insufficient funds for " + amount);
            }
        }
    }
    
    // Using private lock object
    public void transfer(BankAccount target, double amount) {
        synchronized (lock) {
            if (this.balance >= amount) {
                this.balance -= amount;
                target.deposit(amount);
                System.out.println("Transferred " + amount);
            }
        }
    }
    
    public synchronized double getBalance() {
        return balance;
    }
}
```

#### 2. Volatile Keyword

```java
public class VolatileExample {
    private volatile boolean flag = false; // Ensures visibility across threads
    private int counter = 0;
    
    public void writer() {
        counter = 42;
        flag = true; // This write is visible to all threads immediately
    }
    
    public void reader() {
        while (!flag) {
            // Wait for flag to become true
            Thread.yield();
        }
        System.out.println("Counter value: " + counter); // Will print 42
    }
    
    // Without volatile, the reader might never see flag=true
    // due to CPU caching and optimization
}
```

#### 3. Wait, Notify, and NotifyAll

```java
public class ProducerConsumerExample {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 5;
    private final Object lock = new Object();
    
    public void produce() throws InterruptedException {
        int value = 0;
        while (true) {
            synchronized (lock) {
                // Wait while queue is full
                while (queue.size() == CAPACITY) {
                    System.out.println("Producer waiting - queue full");
                    lock.wait(); // Releases lock and waits
                }
                
                queue.add(value++);
                System.out.println("Produced: " + (value - 1) + ", Queue size: " + queue.size());
                
                lock.notifyAll(); // Wake up waiting consumers
            }
            Thread.sleep(1000);
        }
    }
    
    public void consume() throws InterruptedException {
        while (true) {
            synchronized (lock) {
                // Wait while queue is empty
                while (queue.isEmpty()) {
                    System.out.println("Consumer waiting - queue empty");
                    lock.wait(); // Releases lock and waits
                }
                
                int value = queue.poll();
                System.out.println("Consumed: " + value + ", Queue size: " + queue.size());
                
                lock.notifyAll(); // Wake up waiting producers
            }
            Thread.sleep(1500);
        }
    }
}
```

#### 4. ReentrantLock and Condition

```java
import java.util.concurrent.locks.*;

public class AdvancedSynchronization {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final Queue<String> queue = new LinkedList<>();
    private final int CAPACITY = 3;
    
    public void put(String item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == CAPACITY) {
                notFull.await(); // Wait for space
            }
            queue.add(item);
            System.out.println("Put: " + item);
            notEmpty.signal(); // Signal waiting consumers
        } finally {
            lock.unlock();
        }
    }
    
    public String take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await(); // Wait for items
            }
            String item = queue.poll();
            System.out.println("Took: " + item);
            notFull.signal(); // Signal waiting producers
            return item;
        } finally {
            lock.unlock();
        }
    }
    
    // Demonstrate lock features
    public void demonstrateLockFeatures() {
        // Try lock with timeout
        try {
            if (lock.tryLock(2, TimeUnit.SECONDS)) {
                try {
                    // Critical section
                    System.out.println("Acquired lock");
                } finally {
                    lock.unlock();
                }
            } else {
                System.out.println("Could not acquire lock within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check lock status
        System.out.println("Lock held by current thread: " + lock.isHeldByCurrentThread());
        System.out.println("Lock has queued threads: " + lock.hasQueuedThreads());
    }
}
```

#### 5. Atomic Classes

```java
import java.util.concurrent.atomic.*;

public class AtomicExample {
    private AtomicInteger counter = new AtomicInteger(0);
    private AtomicReference<String> message = new AtomicReference<>("Initial");
    private AtomicBoolean flag = new AtomicBoolean(false);
    
    public void demonstrateAtomicOperations() {
        // Atomic increment
        int newValue = counter.incrementAndGet();
        System.out.println("New counter value: " + newValue);
        
        // Compare and swap
        boolean updated = counter.compareAndSet(1, 10);
        System.out.println("CAS update successful: " + updated);
        
        // Atomic reference update
        String oldMessage = message.getAndSet("Updated message");
        System.out.println("Old message: " + oldMessage);
        
        // Atomic boolean
        if (flag.compareAndSet(false, true)) {
            System.out.println("Flag set to true");
        }
    }
    
    // Lock-free counter implementation
    public class LockFreeCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        
        public int increment() {
            return count.incrementAndGet();
        }
        
        public int decrement() {
            return count.decrementAndGet();
        }
        
        public int get() {
            return count.get();
        }
    }
}
```

---

## Java 8+ Features

### Q7: Explain Stream API with practical examples

**Answer:**

#### Stream Creation and Basic Operations

```java
import java.util.stream.*;
import java.util.*;

public class StreamAPIExamples {
    
    // Sample data
    public static class Employee {
        private String name;
        private String department;
        private double salary;
        private int age;
        
        public Employee(String name, String department, double salary, int age) {
            this.name = name;
            this.department = department;
            this.salary = salary;
            this.age = age;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public double getSalary() { return salary; }
        public int getAge() { return age; }
        
        @Override
        public String toString() {
            return String.format("%s (%s, $%.0f, %d years)", name, department, salary, age);
        }
    }
    
    public static void main(String[] args) {
        List<Employee> employees = Arrays.asList(
            new Employee("John", "Engineering", 75000, 28),
            new Employee("Jane", "Engineering", 85000, 32),
            new Employee("Bob", "Sales", 55000, 25),
            new Employee("Alice", "Marketing", 65000, 29),
            new Employee("Charlie", "Engineering", 95000, 35),
            new Employee("Diana", "Sales", 60000, 27),
            new Employee("Eve", "Marketing", 70000, 30)
        );
        
        demonstrateBasicOperations(employees);
        demonstrateAdvancedOperations(employees);
        demonstrateCollectors(employees);
    }
    
    public static void demonstrateBasicOperations(List<Employee> employees) {
        System.out.println("=== Basic Stream Operations ===");
        
        // Filter - Find engineering employees with salary > 80000
        List<Employee> highPaidEngineers = employees.stream()
            .filter(emp -> emp.getDepartment().equals("Engineering"))
            .filter(emp -> emp.getSalary() > 80000)
            .collect(Collectors.toList());
        
        System.out.println("High-paid Engineers:");
        highPaidEngineers.forEach(System.out::println);
        
        // Map - Get list of employee names
        List<String> names = employees.stream()
            .map(Employee::getName)
            .collect(Collectors.toList());
        
        System.out.println("\nEmployee Names: " + names);
        
        // FlatMap - Get unique characters from all names
        List<Character> uniqueChars = employees.stream()
            .map(Employee::getName)
            .flatMap(name -> name.chars().mapToObj(c -> (char) c))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        System.out.println("Unique characters in names: " + uniqueChars);
        
        // Sorted - Sort by salary descending
        List<Employee> sortedBySalary = employees.stream()
            .sorted((e1, e2) -> Double.compare(e2.getSalary(), e1.getSalary()))
            .collect(Collectors.toList());
        
        System.out.println("\nEmployees sorted by salary (desc):");
        sortedBySalary.forEach(System.out::println);
    }
    
    public static void demonstrateAdvancedOperations(List<Employee> employees) {
        System.out.println("\n=== Advanced Stream Operations ===");
        
        // Find operations
        Optional<Employee> highestPaid = employees.stream()
            .max(Comparator.comparingDouble(Employee::getSalary));
        
        highestPaid.ifPresent(emp -> 
            System.out.println("Highest paid: " + emp));
        
        // Any/All/None match
        boolean hasHighEarner = employees.stream()
            .anyMatch(emp -> emp.getSalary() > 90000);
        System.out.println("Has high earner (>90k): " + hasHighEarner);
        
        boolean allAdults = employees.stream()
            .allMatch(emp -> emp.getAge() >= 18);
        System.out.println("All adults: " + allAdults);
        
        // Reduce operations
        double totalSalary = employees.stream()
            .mapToDouble(Employee::getSalary)
            .reduce(0, Double::sum);
        System.out.println("Total salary: $" + totalSalary);
        
        Optional<String> longestName = employees.stream()
            .map(Employee::getName)
            .reduce((name1, name2) -> name1.length() > name2.length() ? name1 : name2);
        
        longestName.ifPresent(name -> 
            System.out.println("Longest name: " + name));
        
        // Parallel streams for better performance on large datasets
        double avgSalaryParallel = employees.parallelStream()
            .mapToDouble(Employee::getSalary)
            .average()
            .orElse(0);
        System.out.println("Average salary (parallel): $" + avgSalaryParallel);
    }
    
    public static void demonstrateCollectors(List<Employee> employees) {
        System.out.println("\n=== Collectors Examples ===");
        
        // Group by department
        Map<String, List<Employee>> byDepartment = employees.stream()
            .collect(Collectors.groupingBy(Employee::getDepartment));
        
        System.out.println("Grouped by department:");
        byDepartment.forEach((dept, empList) -> {
            System.out.println(dept + ": " + empList.size() + " employees");
        });
        
        // Group by department and calculate average salary
        Map<String, Double> avgSalaryByDept = employees.stream()
            .collect(Collectors.groupingBy(
                Employee::getDepartment,
                Collectors.averagingDouble(Employee::getSalary)
            ));
        
        System.out.println("\nAverage salary by department:");
        avgSalaryByDept.forEach((dept, avg) -> 
            System.out.println(dept + ": $" + String.format("%.2f", avg)));
        
        // Partition by high/low salary
        Map<Boolean, List<Employee>> partitionedBySalary = employees.stream()
            .collect(Collectors.partitioningBy(emp -> emp.getSalary() > 70000));
        
        System.out.println("\nHigh salary employees (>70k): " + 
            partitionedBySalary.get(true).size());
        System.out.println("Low salary employees (<=70k): " + 
            partitionedBySalary.get(false).size());
        
        // Custom collector - joining names with salary info
        String empSummary = employees.stream()
            .map(emp -> emp.getName() + "($" + (int)emp.getSalary() + ")")
            .collect(Collectors.joining(", ", "Employees: [", "]"));
        
        System.out.println("\n" + empSummary);
        
        // Statistics
        DoubleSummaryStatistics salaryStats = employees.stream()
            .collect(Collectors.summarizingDouble(Employee::getSalary));
        
        System.out.println("\nSalary Statistics:");
        System.out.println("Count: " + salaryStats.getCount());
        System.out.println("Sum: $" + salaryStats.getSum());
        System.out.println("Average: $" + String.format("%.2f", salaryStats.getAverage()));
        System.out.println("Min: $" + salaryStats.getMin());
        System.out.println("Max: $" + salaryStats.getMax());
    }
}
```

### Q8: Explain Optional class and its benefits

**Answer:**

#### Optional Usage and Best Practices

```java
import java.util.Optional;
import java.util.List;
import java.util.Arrays;

public class OptionalExamples {
    
    public static class User {
        private String name;
        private String email;
        private Address address;
        
        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
        
        public User(String name, String email, Address address) {
            this.name = name;
            this.email = email;
            this.address = address;
        }
        
        public String getName() { return name; }
        public String getEmail() { return email; }
        public Optional<Address> getAddress() { return Optional.ofNullable(address); }
    }
    
    public static class Address {
        private String street;
        private String city;
        private String zipCode;
        
        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
        
        public String getStreet() { return street; }
        public String getCity() { return city; }
        public String getZipCode() { return zipCode; }
    }
    
    public static void main(String[] args) {
        demonstrateBasicOptional();
        demonstrateOptionalChaining();
        demonstrateOptionalWithStreams();
        demonstrateBestPractices();
    }
    
    public static void demonstrateBasicOptional() {
        System.out.println("=== Basic Optional Usage ===");
        
        // Creating Optional instances
        Optional<String> emptyOptional = Optional.empty();
        Optional<String> nonEmptyOptional = Optional.of("Hello World");
        Optional<String> nullableOptional = Optional.ofNullable(null);
        
        // Checking presence
        System.out.println("Empty optional has value: " + emptyOptional.isPresent());
        System.out.println("Non-empty optional has value: " + nonEmptyOptional.isPresent());
        
        // Getting values safely
        String value1 = nonEmptyOptional.orElse("Default Value");
        String value2 = emptyOptional.orElse("Default Value");
        
        System.out.println("Value 1: " + value1);
        System.out.println("Value 2: " + value2);
        
        // OrElseGet - lazy evaluation
        String value3 = emptyOptional.orElseGet(() -> {
            System.out.println("Computing default value...");
            return "Computed Default";
        });
        System.out.println("Value 3: " + value3);
        
        // OrElseThrow
        try {
            String value4 = emptyOptional.orElseThrow(() -> 
                new IllegalStateException("Value not present"));
        } catch (IllegalStateException e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }
    
    public static void demonstrateOptionalChaining() {
        System.out.println("\n=== Optional Chaining ===");
        
        User userWithAddress = new User("John", "john@example.com", 
            new Address("123 Main St", "Anytown", "12345"));
        User userWithoutAddress = new User("Jane", "jane@example.com");
        
        // Traditional null checking (avoid this)
        /*
        if (user != null && user.getAddress() != null && user.getAddress().getCity() != null) {
            String city = user.getAddress().getCity();
        }
        */
        
        // Optional chaining
        String city1 = Optional.ofNullable(userWithAddress)
            .flatMap(User::getAddress)
            .map(Address::getCity)
            .orElse("Unknown City");
        
        String city2 = Optional.ofNullable(userWithoutAddress)
            .flatMap(User::getAddress)
            .map(Address::getCity)
            .orElse("Unknown City");
        
        System.out.println("User 1 city: " + city1);
        System.out.println("User 2 city: " + city2);
        
        // Complex chaining with filtering
        String zipCode = Optional.ofNullable(userWithAddress)
            .flatMap(User::getAddress)
            .filter(addr -> addr.getCity().equals("Anytown"))
            .map(Address::getZipCode)
            .orElse("No zip code");
        
        System.out.println("Zip code for Anytown: " + zipCode);
    }
    
    public static void demonstrateOptionalWithStreams() {
        System.out.println("\n=== Optional with Streams ===");
        
        List<User> users = Arrays.asList(
            new User("Alice", "alice@example.com", new Address("456 Oak Ave", "Springfield", "67890")),
            new User("Bob", "bob@example.com"),
            new User("Charlie", "charlie@example.com", new Address("789 Pine St", "Riverside", "11111"))
        );
        
        // Find cities of users who have addresses
        List<String> cities = users.stream()
            .map(User::getAddress)           // Stream<Optional<Address>>
            .filter(Optional::isPresent)     // Filter out empty optionals
            .map(Optional::get)              // Extract Address from Optional
            .map(Address::getCity)           // Get city name
            .collect(Collectors.toList());
        
        System.out.println("Cities: " + cities);
        
        // Java 9+ - using Optional.stream()
        /*
        List<String> citiesJava9 = users.stream()
            .map(User::getAddress)
            .flatMap(Optional::stream)       // Flattens Optional to Stream
            .map(Address::getCity)
            .collect(Collectors.toList());
        */
        
        // Find first user from specific city
        Optional<User> userFromSpringfield = users.stream()
            .filter(user -> user.getAddress()
                .map(Address::getCity)
                .filter(city -> city.equals("Springfield"))
                .isPresent())
            .findFirst();
        
        userFromSpringfield.ifPresent(user -> 
            System.out.println("User from Springfield: " + user.getName()));
    }
    
    public static void demonstrateBestPractices() {
        System.out.println("\n=== Optional Best Practices ===");
        
        // DON'T do this - defeating the purpose of Optional
        Optional<String> optional = findUserEmail("john@example.com");
        /*
        if (optional.isPresent()) {
            String email = optional.get(); // BAD - can throw exception
        }
        */
        
        // DO this instead
        optional.ifPresent(email -> {
            System.out.println("Found email: " + email);
            sendEmail(email);
        });
        
        // Chain operations instead of nested if-else
        String result = findUserEmail("jane@example.com")
            .filter(email -> email.contains("@"))
            .map(String::toUpperCase)
            .map(email -> "Email: " + email)
            .orElse("No valid email found");
        
        System.out.println(result);
        
        // Use Optional for method return types, not fields
        // GOOD: public Optional<User> findUser(String id)
        // BAD: private Optional<String> name; // Don't use Optional as field type
        
        // Don't use Optional.of() with potentially null values
        String possiblyNull = null;
        // Optional<String> bad = Optional.of(possiblyNull); // Throws NPE
        Optional<String> good = Optional.ofNullable(possiblyNull); // Safe
        
        System.out.println("Safe optional: " + good.orElse("null value"));
    }
    
    public static Optional<String> findUserEmail(String query) {
        // Simulate database lookup
        if (query.equals("john@example.com")) {
            return Optional.of("john@example.com");
        }
        return Optional.empty();
    }
    
    public static void sendEmail(String email) {
        System.out.println("Sending email to: " + email);
    }
}
```

---

This completes the first part of the Java Core Concepts. Would you like me to continue with the remaining sections (Memory Management, Exception Handling) and then move on to the Data Structures file?
