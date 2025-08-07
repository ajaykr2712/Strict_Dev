# Hard Coding Challenges

## Table of Contents
1. [Dynamic Programming Advanced](#dynamic-programming-advanced)
2. [Graph Algorithms](#graph-algorithms)
3. [Advanced Tree Problems](#advanced-tree-problems)
4. [String Manipulation Advanced](#string-manipulation-advanced)
5. [System Design Coding](#system-design-coding)
6. [Concurrency Problems](#concurrency-problems)

---

## Dynamic Programming Advanced

### 1. Edit Distance (Levenshtein Distance)
**Problem:** Given two strings word1 and word2, return the minimum number of operations required to convert word1 to word2. You have the following three operations permitted on a word: Insert, Delete, Replace.

**Example:**
```
Input: word1 = "horse", word2 = "ros"
Output: 3
Explanation: 
horse -> rorse (replace 'h' with 'r')
rorse -> rose (remove 'r')
rose -> ros (remove 'e')
```

**Solution:**
```java
public class EditDistance {
    public int minDistance(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        
        // dp[i][j] represents edit distance between first i chars of word1
        // and first j chars of word2
        int[][] dp = new int[m + 1][n + 1];
        
        // Base cases
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i; // Need i deletions
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j; // Need j insertions
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // No operation needed
                } else {
                    dp[i][j] = 1 + Math.min(
                        Math.min(dp[i - 1][j], dp[i][j - 1]), // Delete or Insert
                        dp[i - 1][j - 1] // Replace
                    );
                }
            }
        }
        
        return dp[m][n];
    }
}
```

### 2. Longest Increasing Subsequence
**Problem:** Given an integer array nums, return the length of the longest strictly increasing subsequence.

**Solution:**
```java
public class LongestIncreasingSubsequence {
    // O(n log n) solution using binary search
    public int lengthOfLIS(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        
        List<Integer> tails = new ArrayList<>();
        
        for (int num : nums) {
            int pos = binarySearch(tails, num);
            if (pos == tails.size()) {
                tails.add(num);
            } else {
                tails.set(pos, num);
            }
        }
        
        return tails.size();
    }
    
    private int binarySearch(List<Integer> tails, int target) {
        int left = 0, right = tails.size();
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (tails.get(mid) < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left;
    }
}
```

---

## Graph Algorithms

### 3. Word Ladder
**Problem:** Given two words, beginWord and endWord, and a dictionary wordList, return the length of shortest transformation sequence from beginWord to endWord.

**Solution:**
```java
public class WordLadder {
    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        Set<String> wordSet = new HashSet<>(wordList);
        if (!wordSet.contains(endWord)) return 0;
        
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.offer(beginWord);
        visited.add(beginWord);
        int level = 1;
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                String current = queue.poll();
                if (current.equals(endWord)) return level;
                
                // Try all possible one character changes
                char[] chars = current.toCharArray();
                for (int j = 0; j < chars.length; j++) {
                    char originalChar = chars[j];
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == originalChar) continue;
                        chars[j] = c;
                        String newWord = new String(chars);
                        
                        if (wordSet.contains(newWord) && !visited.contains(newWord)) {
                            queue.offer(newWord);
                            visited.add(newWord);
                        }
                    }
                    chars[j] = originalChar; // Restore
                }
            }
            level++;
        }
        
        return 0;
    }
}
```

### 4. Course Schedule II (Topological Sort)
**Problem:** There are a total of numCourses courses you have to take, labeled from 0 to numCourses - 1. Some courses may have prerequisites. Return the ordering of courses you should take to finish all courses.

**Solution:**
```java
public class CourseScheduleII {
    public int[] findOrder(int numCourses, int[][] prerequisites) {
        // Build adjacency list and in-degree array
        List<List<Integer>> graph = new ArrayList<>();
        int[] inDegree = new int[numCourses];
        
        for (int i = 0; i < numCourses; i++) {
            graph.add(new ArrayList<>());
        }
        
        for (int[] prereq : prerequisites) {
            int course = prereq[0];
            int prerequisite = prereq[1];
            graph.get(prerequisite).add(course);
            inDegree[course]++;
        }
        
        // Kahn's algorithm for topological sorting
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        int[] result = new int[numCourses];
        int index = 0;
        
        while (!queue.isEmpty()) {
            int current = queue.poll();
            result[index++] = current;
            
            for (int neighbor : graph.get(current)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        return index == numCourses ? result : new int[0];
    }
}
```

---

## Advanced Tree Problems

### 5. Serialize and Deserialize Binary Tree
**Problem:** Design an algorithm to serialize and deserialize a binary tree.

**Solution:**
```java
public class SerializeDeserializeBinaryTree {
    private static final String DELIMITER = ",";
    private static final String NULL_NODE = "null";
    
    // Encodes a tree to a single string (preorder traversal)
    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }
    
    private void serializeHelper(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append(NULL_NODE).append(DELIMITER);
            return;
        }
        
        sb.append(node.val).append(DELIMITER);
        serializeHelper(node.left, sb);
        serializeHelper(node.right, sb);
    }
    
    // Decodes your encoded data to tree
    public TreeNode deserialize(String data) {
        Queue<String> nodes = new LinkedList<>();
        nodes.addAll(Arrays.asList(data.split(DELIMITER)));
        return deserializeHelper(nodes);
    }
    
    private TreeNode deserializeHelper(Queue<String> nodes) {
        String val = nodes.poll();
        if (NULL_NODE.equals(val)) {
            return null;
        }
        
        TreeNode node = new TreeNode(Integer.valueOf(val));
        node.left = deserializeHelper(nodes);
        node.right = deserializeHelper(nodes);
        return node;
    }
}
```

### 6. Binary Tree Maximum Path Sum
**Problem:** A path in a binary tree is a sequence of nodes where each pair of adjacent nodes in the sequence has an edge connecting them. The path sum is the sum of the node's values in the path. Find the maximum path sum.

**Solution:**
```java
public class BinaryTreeMaxPathSum {
    private int maxSum = Integer.MIN_VALUE;
    
    public int maxPathSum(TreeNode root) {
        maxPathSumHelper(root);
        return maxSum;
    }
    
    private int maxPathSumHelper(TreeNode node) {
        if (node == null) return 0;
        
        // Max sum on the left and right sub-trees of node
        int leftSum = Math.max(maxPathSumHelper(node.left), 0);
        int rightSum = Math.max(maxPathSumHelper(node.right), 0);
        
        // Max path sum with the current node as the highest node
        int currentMaxPath = node.val + leftSum + rightSum;
        
        // Update global maximum
        maxSum = Math.max(maxSum, currentMaxPath);
        
        // Return the max gain the node and one subtree could add to the path
        return node.val + Math.max(leftSum, rightSum);
    }
}
```

---

## String Manipulation Advanced

### 7. Regular Expression Matching
**Problem:** Given an input string s and a pattern p, implement regular expression matching with support for '.' and '*'.

**Solution:**
```java
public class RegularExpressionMatching {
    public boolean isMatch(String s, String p) {
        int m = s.length();
        int n = p.length();
        
        // dp[i][j] represents if s[0...i-1] matches p[0...j-1]
        boolean[][] dp = new boolean[m + 1][n + 1];
        
        // Base case: empty string matches empty pattern
        dp[0][0] = true;
        
        // Handle patterns like a*, a*b*, a*b*c*
        for (int j = 2; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 2];
            }
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char sChar = s.charAt(i - 1);
                char pChar = p.charAt(j - 1);
                
                if (pChar != '*') {
                    // Current characters match or pattern has '.'
                    if (sChar == pChar || pChar == '.') {
                        dp[i][j] = dp[i - 1][j - 1];
                    }
                } else {
                    // Current pattern character is '*'
                    char prevPChar = p.charAt(j - 2);
                    
                    // Case 1: * represents zero occurrences
                    dp[i][j] = dp[i][j - 2];
                    
                    // Case 2: * represents one or more occurrences
                    if (sChar == prevPChar || prevPChar == '.') {
                        dp[i][j] = dp[i][j] || dp[i - 1][j];
                    }
                }
            }
        }
        
        return dp[m][n];
    }
}
```

### 8. Minimum Window Substring
**Problem:** Given two strings s and t, return the minimum window substring of s such that every character in t (including duplicates) is included in the window.

**Solution:**
```java
public class MinimumWindowSubstring {
    public String minWindow(String s, String t) {
        if (s.length() < t.length()) return "";
        
        // Character frequency map for t
        Map<Character, Integer> targetMap = new HashMap<>();
        for (char c : t.toCharArray()) {
            targetMap.put(c, targetMap.getOrDefault(c, 0) + 1);
        }
        
        int left = 0, right = 0;
        int formed = 0; // Number of unique characters in current window with desired frequency
        int required = targetMap.size();
        
        Map<Character, Integer> windowCounts = new HashMap<>();
        
        // Result: [window length, left, right]
        int[] result = {-1, 0, 0};
        
        while (right < s.length()) {
            // Add character from right to window
            char rightChar = s.charAt(right);
            windowCounts.put(rightChar, windowCounts.getOrDefault(rightChar, 0) + 1);
            
            // Check if frequency of current character matches desired count in t
            if (targetMap.containsKey(rightChar) && 
                windowCounts.get(rightChar).intValue() == targetMap.get(rightChar).intValue()) {
                formed++;
            }
            
            // Try to contract window from left
            while (left <= right && formed == required) {
                char leftChar = s.charAt(left);
                
                // Update result if this window is smaller
                if (result[0] == -1 || right - left + 1 < result[0]) {
                    result[0] = right - left + 1;
                    result[1] = left;
                    result[2] = right;
                }
                
                // Remove leftmost character
                windowCounts.put(leftChar, windowCounts.get(leftChar) - 1);
                if (targetMap.containsKey(leftChar) && 
                    windowCounts.get(leftChar).intValue() < targetMap.get(leftChar).intValue()) {
                    formed--;
                }
                
                left++;
            }
            
            right++;
        }
        
        return result[0] == -1 ? "" : s.substring(result[1], result[2] + 1);
    }
}
```

---

## System Design Coding

### 9. LRU Cache Implementation
**Problem:** Design a data structure that follows the constraints of a Least Recently Used (LRU) cache.

**Solution:**
```java
public class LRUCache {
    class DLinkedNode {
        int key;
        int value;
        DLinkedNode prev;
        DLinkedNode next;
        
        DLinkedNode() {}
        DLinkedNode(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private Map<Integer, DLinkedNode> cache = new HashMap<>();
    private int capacity;
    private DLinkedNode head, tail;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        
        head = new DLinkedNode();
        tail = new DLinkedNode();
        
        head.next = tail;
        tail.prev = head;
    }
    
    private void addNode(DLinkedNode node) {
        // Always add new node right after head
        node.prev = head;
        node.next = head.next;
        
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(DLinkedNode node) {
        // Remove an existing node from the linked list
        DLinkedNode prevNode = node.prev;
        DLinkedNode nextNode = node.next;
        
        prevNode.next = nextNode;
        nextNode.prev = prevNode;
    }
    
    private void moveToHead(DLinkedNode node) {
        // Move certain node to head
        removeNode(node);
        addNode(node);
    }
    
    private DLinkedNode popTail() {
        // Pop the current tail
        DLinkedNode lastNode = tail.prev;
        removeNode(lastNode);
        return lastNode;
    }
    
    public int get(int key) {
        DLinkedNode node = cache.get(key);
        if (node == null) {
            return -1;
        }
        
        // Move accessed node to head
        moveToHead(node);
        return node.value;
    }
    
    public void put(int key, int value) {
        DLinkedNode node = cache.get(key);
        
        if (node == null) {
            DLinkedNode newNode = new DLinkedNode(key, value);
            
            if (cache.size() >= capacity) {
                // Remove tail node
                DLinkedNode tail = popTail();
                cache.remove(tail.key);
            }
            
            cache.put(key, newNode);
            addNode(newNode);
        } else {
            // Update existing node
            node.value = value;
            moveToHead(node);
        }
    }
}
```

### 10. Design Twitter Feed
**Problem:** Design a simplified version of Twitter where users can post tweets, follow/unfollow another user, and see the 10 most recent tweets in the user's news feed.

**Solution:**
```java
public class Twitter {
    private static int timeStamp = 0;
    
    class Tweet {
        int id;
        int time;
        Tweet next;
        
        Tweet(int id) {
            this.id = id;
            this.time = timeStamp++;
            this.next = null;
        }
    }
    
    // userId -> user's tweet head
    private Map<Integer, Tweet> tweets;
    // userId -> set of followee ids
    private Map<Integer, Set<Integer>> follows;
    
    public Twitter() {
        tweets = new HashMap<>();
        follows = new HashMap<>();
    }
    
    public void postTweet(int userId, int tweetId) {
        Tweet newTweet = new Tweet(tweetId);
        newTweet.next = tweets.get(userId);
        tweets.put(userId, newTweet);
    }
    
    public List<Integer> getNewsFeed(int userId) {
        List<Integer> result = new ArrayList<>();
        
        // Priority queue to maintain tweets by timestamp (most recent first)
        PriorityQueue<Tweet> pq = new PriorityQueue<>((a, b) -> b.time - a.time);
        
        // Add user's own tweets
        if (tweets.containsKey(userId)) {
            pq.offer(tweets.get(userId));
        }
        
        // Add followees' tweets
        Set<Integer> followees = follows.get(userId);
        if (followees != null) {
            for (int followeeId : followees) {
                if (tweets.containsKey(followeeId)) {
                    pq.offer(tweets.get(followeeId));
                }
            }
        }
        
        // Get top 10 most recent tweets
        while (!pq.isEmpty() && result.size() < 10) {
            Tweet current = pq.poll();
            result.add(current.id);
            
            // Add next tweet from same user if exists
            if (current.next != null) {
                pq.offer(current.next);
            }
        }
        
        return result;
    }
    
    public void follow(int followerId, int followeeId) {
        if (followerId != followeeId) {
            follows.computeIfAbsent(followerId, k -> new HashSet<>()).add(followeeId);
        }
    }
    
    public void unfollow(int followerId, int followeeId) {
        Set<Integer> followees = follows.get(followerId);
        if (followees != null) {
            followees.remove(followeeId);
        }
    }
}
```

---

## Concurrency Problems

### 11. Print in Order using Semaphores
**Problem:** Suppose we have a class with three methods: first(), second(), third(). Ensure they execute in order.

**Solution:**
```java
import java.util.concurrent.Semaphore;

public class Foo {
    private Semaphore semFirst = new Semaphore(1);
    private Semaphore semSecond = new Semaphore(0);
    private Semaphore semThird = new Semaphore(0);
    
    public void first(Runnable printFirst) throws InterruptedException {
        semFirst.acquire();
        printFirst.run();
        semSecond.release();
    }
    
    public void second(Runnable printSecond) throws InterruptedException {
        semSecond.acquire();
        printSecond.run();
        semThird.release();
    }
    
    public void third(Runnable printThird) throws InterruptedException {
        semThird.acquire();
        printThird.run();
        semFirst.release();
    }
}
```

### 12. Producer-Consumer using BlockingQueue
**Problem:** Implement a thread-safe producer-consumer pattern.

**Solution:**
```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ProducerConsumer {
    private BlockingQueue<Integer> queue;
    private final int capacity;
    
    public ProducerConsumer(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 10; i++) {
                    queue.put(i);
                    System.out.println("Produced: " + i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Integer item = queue.take();
                    System.out.println("Consumed: " + item);
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer(5);
        
        Thread producerThread = new Thread(pc.new Producer());
        Thread consumerThread = new Thread(pc.new Consumer());
        
        producerThread.start();
        consumerThread.start();
    }
}
```

---

## Interview Tips for Hard Problems

### Problem-Solving Approach:
1. **Understand the Problem**: Ask clarifying questions
2. **Think Out Loud**: Explain your thought process
3. **Start with Brute Force**: Then optimize
4. **Consider Edge Cases**: Empty inputs, single elements, etc.
5. **Analyze Time/Space Complexity**: Big O notation
6. **Test with Examples**: Walk through your solution

### Common Patterns in Hard Problems:
- **Dynamic Programming**: Optimal substructure + overlapping subproblems
- **Graph Algorithms**: BFS/DFS, topological sort, shortest path
- **Tree Traversal**: Preorder, inorder, postorder, level-order
- **Sliding Window**: For string/array problems
- **Two Pointers**: For sorted arrays or linked lists
- **Divide and Conquer**: Break problem into smaller subproblems

### Time Complexity Goals:
- **String/Array**: O(n) or O(n log n)
- **Trees**: O(n) for traversal, O(h) for search
- **Graphs**: O(V + E) for traversal
- **Dynamic Programming**: O(n²) or better
- **Sorting**: O(n log n)

Remember: The interviewer is more interested in your problem-solving approach than the perfect solution. Communicate clearly, start simple, and iterate towards optimization.
