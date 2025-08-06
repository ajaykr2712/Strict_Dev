# Data Structures Implementation in Java - Interview Guide

## Table of Contents
1. [Arrays and Strings](#arrays-and-strings)
2. [Linked Lists](#linked-lists)
3. [Stacks and Queues](#stacks-and-queues)
4. [Trees](#trees)
5. [Hash Tables](#hash-tables)
6. [Graphs](#graphs)
7. [Heaps](#heaps)

---

## Arrays and Strings

### Q1: Implement a Dynamic Array (like ArrayList) from scratch

**Problem**: Create a resizable array that can grow and shrink dynamically.

```java
public class DynamicArray<T> {
    private Object[] array;
    private int size;
    private int capacity;
    private static final int DEFAULT_CAPACITY = 10;
    
    public DynamicArray() {
        this.capacity = DEFAULT_CAPACITY;
        this.array = new Object[capacity];
        this.size = 0;
    }
    
    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity cannot be negative");
        }
        this.capacity = Math.max(initialCapacity, 1);
        this.array = new Object[capacity];
        this.size = 0;
    }
    
    // Add element to the end
    public void add(T element) {
        ensureCapacity();
        array[size++] = element;
    }
    
    // Add element at specific index
    public void add(int index, T element) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        ensureCapacity();
        
        // Shift elements to the right
        System.arraycopy(array, index, array, index + 1, size - index);
        array[index] = element;
        size++;
    }
    
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return (T) array[index];
    }
    
    public T set(int index, T element) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        T oldValue = get(index);
        array[index] = element;
        return oldValue;
    }
    
    public T remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        T removedElement = get(index);
        
        // Shift elements to the left
        int moveCount = size - index - 1;
        if (moveCount > 0) {
            System.arraycopy(array, index + 1, array, index, moveCount);
        }
        
        array[--size] = null; // Help GC and decrement size
        
        // Shrink array if needed
        if (size <= capacity / 4 && capacity > DEFAULT_CAPACITY) {
            resize(capacity / 2);
        }
        
        return removedElement;
    }
    
    public boolean remove(Object element) {
        int index = indexOf(element);
        if (index >= 0) {
            remove(index);
            return true;
        }
        return false;
    }
    
    public int indexOf(Object element) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(element, array[i])) {
                return i;
            }
        }
        return -1;
    }
    
    public boolean contains(Object element) {
        return indexOf(element) >= 0;
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public void clear() {
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
        size = 0;
    }
    
    private void ensureCapacity() {
        if (size >= capacity) {
            resize(capacity * 2);
        }
    }
    
    private void resize(int newCapacity) {
        Object[] newArray = new Object[newCapacity];
        System.arraycopy(array, 0, newArray, 0, size);
        array = newArray;
        capacity = newCapacity;
    }
    
    // Convert to array
    @SuppressWarnings("unchecked")
    public T[] toArray() {
        Object[] result = new Object[size];
        System.arraycopy(array, 0, result, 0, size);
        return (T[]) result;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(array[i]);
            if (i < size - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}

// Usage and testing
public class DynamicArrayTest {
    public static void main(String[] args) {
        DynamicArray<String> list = new DynamicArray<>();
        
        // Test adding
        list.add("Apple");
        list.add("Banana");
        list.add("Cherry");
        System.out.println("After adding: " + list);
        
        // Test insertion
        list.add(1, "Apricot");
        System.out.println("After insertion: " + list);
        
        // Test removal
        String removed = list.remove(2);
        System.out.println("Removed: " + removed);
        System.out.println("After removal: " + list);
        
        // Test capacity changes
        for (int i = 0; i < 20; i++) {
            list.add("Item" + i);
        }
        System.out.println("After adding 20 items, size: " + list.size());
    }
}
```

### Q2: String Algorithm Problems

```java
public class StringAlgorithms {
    
    // Check if two strings are anagrams
    public static boolean areAnagrams(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        }
        
        // Method 1: Using sorting - O(n log n)
        char[] chars1 = str1.toLowerCase().toCharArray();
        char[] chars2 = str2.toLowerCase().toCharArray();
        Arrays.sort(chars1);
        Arrays.sort(chars2);
        return Arrays.equals(chars1, chars2);
    }
    
    // Optimized anagram check - O(n)
    public static boolean areAnagramsOptimized(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        }
        
        int[] count = new int[26]; // Assuming lowercase English letters
        
        for (int i = 0; i < str1.length(); i++) {
            count[str1.charAt(i) - 'a']++;
            count[str2.charAt(i) - 'a']--;
        }
        
        for (int c : count) {
            if (c != 0) return false;
        }
        return true;
    }
    
    // Find longest substring without repeating characters
    public static int lengthOfLongestSubstring(String s) {
        Set<Character> window = new HashSet<>();
        int left = 0, maxLength = 0;
        
        for (int right = 0; right < s.length(); right++) {
            // Shrink window from left until no duplicates
            while (window.contains(s.charAt(right))) {
                window.remove(s.charAt(left));
                left++;
            }
            
            window.add(s.charAt(right));
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    // Check if string is palindrome
    public static boolean isPalindrome(String s) {
        // Clean string: remove non-alphanumeric and convert to lowercase
        StringBuilder cleaned = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                cleaned.append(Character.toLowerCase(c));
            }
        }
        
        String cleanStr = cleaned.toString();
        int left = 0, right = cleanStr.length() - 1;
        
        while (left < right) {
            if (cleanStr.charAt(left) != cleanStr.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }
    
    // Find first non-repeating character
    public static char firstNonRepeatingChar(String str) {
        Map<Character, Integer> charCount = new LinkedHashMap<>();
        
        // Count frequencies
        for (char c : str.toCharArray()) {
            charCount.put(c, charCount.getOrDefault(c, 0) + 1);
        }
        
        // Find first character with count 1
        for (Map.Entry<Character, Integer> entry : charCount.entrySet()) {
            if (entry.getValue() == 1) {
                return entry.getKey();
            }
        }
        
        return '\0'; // Not found
    }
    
    // Implement strStr() - find needle in haystack
    public static int strStr(String haystack, String needle) {
        if (needle.isEmpty()) return 0;
        if (haystack.length() < needle.length()) return -1;
        
        for (int i = 0; i <= haystack.length() - needle.length(); i++) {
            if (haystack.substring(i, i + needle.length()).equals(needle)) {
                return i;
            }
        }
        return -1;
    }
    
    // KMP algorithm for pattern matching - O(n + m)
    public static int kmpSearch(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        
        int[] lps = computeLPS(pattern);
        int i = 0; // text index
        int j = 0; // pattern index
        
        while (i < text.length()) {
            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }
            
            if (j == pattern.length()) {
                return i - j; // Found at index i-j
            } else if (i < text.length() && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        return -1; // Not found
    }
    
    private static int[] computeLPS(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0;
        int i = 1;
        
        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }
    
    // Group anagrams together
    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        
        for (String str : strs) {
            // Create signature by sorting characters
            char[] chars = str.toCharArray();
            Arrays.sort(chars);
            String signature = new String(chars);
            
            groups.computeIfAbsent(signature, k -> new ArrayList<>()).add(str);
        }
        
        return new ArrayList<>(groups.values());
    }
    
    public static void main(String[] args) {
        // Test anagrams
        System.out.println("Are 'listen' and 'silent' anagrams? " + 
            areAnagrams("listen", "silent"));
        
        // Test longest substring
        System.out.println("Longest substring without repeating chars in 'abcabcbb': " + 
            lengthOfLongestSubstring("abcabcbb"));
        
        // Test palindrome
        System.out.println("Is 'A man, a plan, a canal: Panama' palindrome? " + 
            isPalindrome("A man, a plan, a canal: Panama"));
        
        // Test first non-repeating
        System.out.println("First non-repeating char in 'leetcode': " + 
            firstNonRepeatingChar("leetcode"));
        
        // Test pattern matching
        System.out.println("Pattern 'abc' found in 'ababcab' at index: " + 
            kmpSearch("ababcab", "abc"));
        
        // Test group anagrams
        String[] words = {"eat", "tea", "tan", "ate", "nat", "bat"};
        System.out.println("Grouped anagrams: " + groupAnagrams(words));
    }
}
```

---

## Linked Lists

### Q3: Implement Singly Linked List with all operations

```java
public class SinglyLinkedList<T> {
    private Node<T> head;
    private int size;
    
    private static class Node<T> {
        T data;
        Node<T> next;
        
        Node(T data) {
            this.data = data;
        }
        
        Node(T data, Node<T> next) {
            this.data = data;
            this.next = next;
        }
    }
    
    public SinglyLinkedList() {
        this.head = null;
        this.size = 0;
    }
    
    // Add element at the beginning - O(1)
    public void addFirst(T data) {
        head = new Node<>(data, head);
        size++;
    }
    
    // Add element at the end - O(n)
    public void addLast(T data) {
        if (head == null) {
            addFirst(data);
            return;
        }
        
        Node<T> current = head;
        while (current.next != null) {
            current = current.next;
        }
        current.next = new Node<>(data);
        size++;
    }
    
    // Add element at specific index - O(n)
    public void add(int index, T data) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        if (index == 0) {
            addFirst(data);
            return;
        }
        
        Node<T> current = head;
        for (int i = 0; i < index - 1; i++) {
            current = current.next;
        }
        
        current.next = new Node<>(data, current.next);
        size++;
    }
    
    // Get element at index - O(n)
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }
    
    // Remove first element - O(1)
    public T removeFirst() {
        if (head == null) {
            throw new NoSuchElementException("List is empty");
        }
        
        T data = head.data;
        head = head.next;
        size--;
        return data;
    }
    
    // Remove element at index - O(n)
    public T remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        if (index == 0) {
            return removeFirst();
        }
        
        Node<T> current = head;
        for (int i = 0; i < index - 1; i++) {
            current = current.next;
        }
        
        T data = current.next.data;
        current.next = current.next.next;
        size--;
        return data;
    }
    
    // Remove first occurrence of element - O(n)
    public boolean remove(T data) {
        if (head == null) return false;
        
        if (Objects.equals(head.data, data)) {
            removeFirst();
            return true;
        }
        
        Node<T> current = head;
        while (current.next != null && !Objects.equals(current.next.data, data)) {
            current = current.next;
        }
        
        if (current.next != null) {
            current.next = current.next.next;
            size--;
            return true;
        }
        return false;
    }
    
    // Find index of element - O(n)
    public int indexOf(T data) {
        Node<T> current = head;
        for (int i = 0; i < size; i++) {
            if (Objects.equals(current.data, data)) {
                return i;
            }
            current = current.next;
        }
        return -1;
    }
    
    // Check if list contains element - O(n)
    public boolean contains(T data) {
        return indexOf(data) >= 0;
    }
    
    // Reverse the linked list - O(n)
    public void reverse() {
        Node<T> prev = null;
        Node<T> current = head;
        Node<T> next;
        
        while (current != null) {
            next = current.next;
            current.next = prev;
            prev = current;
            current = next;
        }
        head = prev;
    }
    
    // Find middle element using two pointers - O(n)
    public T findMiddle() {
        if (head == null) {
            throw new NoSuchElementException("List is empty");
        }
        
        Node<T> slow = head;
        Node<T> fast = head;
        
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        
        return slow.data;
    }
    
    // Detect cycle using Floyd's algorithm - O(n)
    public boolean hasCycle() {
        if (head == null || head.next == null) {
            return false;
        }
        
        Node<T> slow = head;
        Node<T> fast = head.next;
        
        while (slow != fast) {
            if (fast == null || fast.next == null) {
                return false;
            }
            slow = slow.next;
            fast = fast.next.next;
        }
        
        return true;
    }
    
    // Find nth node from end - O(n)
    public T findNthFromEnd(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }
        
        Node<T> first = head;
        Node<T> second = head;
        
        // Move first pointer n steps ahead
        for (int i = 0; i < n; i++) {
            if (first == null) {
                throw new IllegalArgumentException("n is larger than list size");
            }
            first = first.next;
        }
        
        // Move both pointers until first reaches end
        while (first != null) {
            first = first.next;
            second = second.next;
        }
        
        return second.data;
    }
    
    // Remove duplicates from sorted list - O(n)
    public void removeDuplicatesFromSorted() {
        Node<T> current = head;
        
        while (current != null && current.next != null) {
            if (Objects.equals(current.data, current.next.data)) {
                current.next = current.next.next;
                size--;
            } else {
                current = current.next;
            }
        }
    }
    
    // Merge two sorted lists
    public static <T extends Comparable<T>> SinglyLinkedList<T> mergeSorted(
            SinglyLinkedList<T> list1, SinglyLinkedList<T> list2) {
        
        SinglyLinkedList<T> result = new SinglyLinkedList<>();
        Node<T> current1 = list1.head;
        Node<T> current2 = list2.head;
        
        while (current1 != null && current2 != null) {
            if (current1.data.compareTo(current2.data) <= 0) {
                result.addLast(current1.data);
                current1 = current1.next;
            } else {
                result.addLast(current2.data);
                current2 = current2.next;
            }
        }
        
        // Add remaining elements
        while (current1 != null) {
            result.addLast(current1.data);
            current1 = current1.next;
        }
        
        while (current2 != null) {
            result.addLast(current2.data);
            current2 = current2.next;
        }
        
        return result;
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public void clear() {
        head = null;
        size = 0;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = head;
        
        while (current != null) {
            sb.append(current.data);
            if (current.next != null) {
                sb.append(" -> ");
            }
            current = current.next;
        }
        
        sb.append("]");
        return sb.toString();
    }
}
```

### Q4: Doubly Linked List Implementation

```java
public class DoublyLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;
    
    private static class Node<T> {
        T data;
        Node<T> next;
        Node<T> prev;
        
        Node(T data) {
            this.data = data;
        }
        
        Node(T data, Node<T> prev, Node<T> next) {
            this.data = data;
            this.prev = prev;
            this.next = next;
        }
    }
    
    public DoublyLinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }
    
    // Add at beginning - O(1)
    public void addFirst(T data) {
        Node<T> newNode = new Node<>(data);
        
        if (head == null) {
            head = tail = newNode;
        } else {
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }
        size++;
    }
    
    // Add at end - O(1)
    public void addLast(T data) {
        Node<T> newNode = new Node<>(data);
        
        if (tail == null) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
        size++;
    }
    
    // Add at specific index - O(n)
    public void add(int index, T data) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        if (index == 0) {
            addFirst(data);
        } else if (index == size) {
            addLast(data);
        } else {
            Node<T> current = getNode(index);
            Node<T> newNode = new Node<>(data, current.prev, current);
            
            current.prev.next = newNode;
            current.prev = newNode;
            size++;
        }
    }
    
    // Remove first - O(1)
    public T removeFirst() {
        if (head == null) {
            throw new NoSuchElementException("List is empty");
        }
        
        T data = head.data;
        
        if (head == tail) {
            head = tail = null;
        } else {
            head = head.next;
            head.prev = null;
        }
        
        size--;
        return data;
    }
    
    // Remove last - O(1)
    public T removeLast() {
        if (tail == null) {
            throw new NoSuchElementException("List is empty");
        }
        
        T data = tail.data;
        
        if (head == tail) {
            head = tail = null;
        } else {
            tail = tail.prev;
            tail.next = null;
        }
        
        size--;
        return data;
    }
    
    // Get node at index (optimized to search from closest end)
    private Node<T> getNode(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        Node<T> current;
        
        // Choose direction based on which end is closer
        if (index < size / 2) {
            // Start from head
            current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
        } else {
            // Start from tail
            current = tail;
            for (int i = size - 1; i > index; i--) {
                current = current.prev;
            }
        }
        
        return current;
    }
    
    public T get(int index) {
        return getNode(index).data;
    }
    
    public T set(int index, T data) {
        Node<T> node = getNode(index);
        T oldData = node.data;
        node.data = data;
        return oldData;
    }
    
    // Remove at index - O(n)
    public T remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        if (index == 0) {
            return removeFirst();
        } else if (index == size - 1) {
            return removeLast();
        } else {
            Node<T> nodeToRemove = getNode(index);
            T data = nodeToRemove.data;
            
            nodeToRemove.prev.next = nodeToRemove.next;
            nodeToRemove.next.prev = nodeToRemove.prev;
            
            size--;
            return data;
        }
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    // Iterator for forward traversal
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = head;
            
            @Override
            public boolean hasNext() {
                return current != null;
            }
            
            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }
    
    // Iterator for reverse traversal
    public Iterator<T> reverseIterator() {
        return new Iterator<T>() {
            private Node<T> current = tail;
            
            @Override
            public boolean hasNext() {
                return current != null;
            }
            
            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T data = current.data;
                current = current.prev;
                return data;
            }
        };
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = head;
        
        while (current != null) {
            sb.append(current.data);
            if (current.next != null) {
                sb.append(" <-> ");
            }
            current = current.next;
        }
        
        sb.append("]");
        return sb.toString();
    }
}
```

---

## Stacks and Queues

### Q5: Stack Implementation using Arrays

```java
public class ArrayStack<T> {
    private Object[] array;
    private int top;
    private int capacity;
    private static final int DEFAULT_CAPACITY = 10;
    
    public ArrayStack() {
        this.capacity = DEFAULT_CAPACITY;
        this.array = new Object[capacity];
        this.top = -1;
    }
    
    public ArrayStack(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.array = new Object[capacity];
        this.top = -1;
    }
    
    // Push element onto stack - O(1)
    public void push(T element) {
        if (isFull()) {
            resize();
        }
        array[++top] = element;
    }
    
    // Pop element from stack - O(1)
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        T element = (T) array[top];
        array[top--] = null; // Help GC
        return element;
    }
    
    // Peek at top element without removing - O(1)
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return (T) array[top];
    }
    
    public boolean isEmpty() {
        return top == -1;
    }
    
    public boolean isFull() {
        return top == capacity - 1;
    }
    
    public int size() {
        return top + 1;
    }
    
    private void resize() {
        capacity *= 2;
        Object[] newArray = new Object[capacity];
        System.arraycopy(array, 0, newArray, 0, top + 1);
        array = newArray;
    }
    
    public void clear() {
        while (!isEmpty()) {
            pop();
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i <= top; i++) {
            sb.append(array[i]);
            if (i < top) sb.append(", ");
        }
        sb.append("] <- top");
        return sb.toString();
    }
}

// Stack Applications
public class StackApplications {
    
    // Check balanced parentheses - O(n)
    public static boolean isBalanced(String expression) {
        ArrayStack<Character> stack = new ArrayStack<>();
        
        for (char ch : expression.toCharArray()) {
            // Push opening brackets
            if (ch == '(' || ch == '[' || ch == '{') {
                stack.push(ch);
            }
            // Check closing brackets
            else if (ch == ')' || ch == ']' || ch == '}') {
                if (stack.isEmpty()) {
                    return false;
                }
                
                char top = stack.pop();
                if (!isMatchingPair(top, ch)) {
                    return false;
                }
            }
        }
        
        return stack.isEmpty();
    }
    
    private static boolean isMatchingPair(char open, char close) {
        return (open == '(' && close == ')') ||
               (open == '[' && close == ']') ||
               (open == '{' && close == '}');
    }
    
    // Evaluate postfix expression - O(n)
    public static int evaluatePostfix(String expression) {
        ArrayStack<Integer> stack = new ArrayStack<>();
        
        for (String token : expression.split(" ")) {
            if (isOperator(token)) {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression");
                }
                
                int operand2 = stack.pop();
                int operand1 = stack.pop();
                int result = performOperation(operand1, operand2, token);
                stack.push(result);
            } else {
                stack.push(Integer.parseInt(token));
            }
        }
        
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression");
        }
        
        return stack.pop();
    }
    
    private static boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || 
               token.equals("*") || token.equals("/");
    }
    
    private static int performOperation(int a, int b, String operator) {
        switch (operator) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": 
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
            default: throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }
    
    // Convert infix to postfix - O(n)
    public static String infixToPostfix(String infix) {
        ArrayStack<Character> stack = new ArrayStack<>();
        StringBuilder postfix = new StringBuilder();
        
        for (char ch : infix.toCharArray()) {
            if (Character.isLetterOrDigit(ch)) {
                postfix.append(ch).append(' ');
            } else if (ch == '(') {
                stack.push(ch);
            } else if (ch == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    postfix.append(stack.pop()).append(' ');
                }
                if (!stack.isEmpty()) {
                    stack.pop(); // Remove '('
                }
            } else if (isOperator(String.valueOf(ch))) {
                while (!stack.isEmpty() && 
                       getPrecedence(ch) <= getPrecedence(stack.peek())) {
                    postfix.append(stack.pop()).append(' ');
                }
                stack.push(ch);
            }
        }
        
        while (!stack.isEmpty()) {
            postfix.append(stack.pop()).append(' ');
        }
        
        return postfix.toString().trim();
    }
    
    private static int getPrecedence(char operator) {
        switch (operator) {
            case '+':
            case '-': return 1;
            case '*':
            case '/': return 2;
            default: return 0;
        }
    }
    
    public static void main(String[] args) {
        // Test balanced parentheses
        System.out.println("Is '{[()]}' balanced? " + isBalanced("{[()]}"));
        System.out.println("Is '{[()]' balanced? " + isBalanced("{[()]"));
        
        // Test postfix evaluation
        System.out.println("Postfix '2 3 + 4 *' = " + evaluatePostfix("2 3 + 4 *"));
        
        // Test infix to postfix
        System.out.println("Infix 'a+b*c' to postfix: " + infixToPostfix("a+b*c"));
    }
}
```

### Q6: Queue Implementation using Two Stacks

```java
import java.util.Stack;

public class QueueUsingStacks<T> {
    private Stack<T> stack1; // For enqueue operations
    private Stack<T> stack2; // For dequeue operations
    
    public QueueUsingStacks() {
        stack1 = new Stack<>();
        stack2 = new Stack<>();
    }
    
    // Enqueue operation - O(1)
    public void enqueue(T element) {
        stack1.push(element);
    }
    
    // Dequeue operation - O(1) amortized
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        
        // Move elements from stack1 to stack2 if stack2 is empty
        if (stack2.isEmpty()) {
            while (!stack1.isEmpty()) {
                stack2.push(stack1.pop());
            }
        }
        
        return stack2.pop();
    }
    
    // Peek at front element - O(1) amortized
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        
        if (stack2.isEmpty()) {
            while (!stack1.isEmpty()) {
                stack2.push(stack1.pop());
            }
        }
        
        return stack2.peek();
    }
    
    public boolean isEmpty() {
        return stack1.isEmpty() && stack2.isEmpty();
    }
    
    public int size() {
        return stack1.size() + stack2.size();
    }
    
    @Override
    public String toString() {
        // Create a temporary representation
        List<T> elements = new ArrayList<>();
        
        // Add elements from stack2 (these will be dequeued first)
        List<T> stack2Elements = new ArrayList<>(stack2);
        Collections.reverse(stack2Elements);
        elements.addAll(stack2Elements);
        
        // Add elements from stack1 (these will be dequeued later)
        elements.addAll(stack1);
        
        return "Queue: " + elements + " (front -> rear)";
    }
}

// Circular Queue Implementation using Arrays
public class CircularQueue<T> {
    private Object[] array;
    private int front;
    private int rear;
    private int size;
    private int capacity;
    
    public CircularQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.array = new Object[capacity];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }
    
    // Enqueue operation - O(1)
    public void enqueue(T element) {
        if (isFull()) {
            throw new IllegalStateException("Queue is full");
        }
        
        rear = (rear + 1) % capacity;
        array[rear] = element;
        size++;
    }
    
    // Dequeue operation - O(1)
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        
        T element = (T) array[front];
        array[front] = null; // Help GC
        front = (front + 1) % capacity;
        size--;
        
        return element;
    }
    
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return (T) array[front];
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public boolean isFull() {
        return size == capacity;
    }
    
    public int size() {
        return size;
    }
    
    @Override
    public String toString() {
        if (isEmpty()) {
            return "Queue: []";
        }
        
        StringBuilder sb = new StringBuilder("Queue: [");
        for (int i = 0; i < size; i++) {
            int index = (front + i) % capacity;
            sb.append(array[index]);
            if (i < size - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
```

This covers the first part of the Data Structures implementation. Would you like me to continue with Trees, Hash Tables, Graphs, and Heaps, and then move on to the other files (Algorithms, Cloud Services, Spring Boot, etc.)?
