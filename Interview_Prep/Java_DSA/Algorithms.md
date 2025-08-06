# Algorithm Problems - Interview Preparation

## Table of Contents
1. [Sorting Algorithms](#sorting-algorithms)
2. [Searching Algorithms](#searching-algorithms)
3. [Dynamic Programming](#dynamic-programming)
4. [Graph Algorithms](#graph-algorithms)
5. [Tree Algorithms](#tree-algorithms)
6. [Two Pointers & Sliding Window](#two-pointers--sliding-window)
7. [Backtracking](#backtracking)

---

## Sorting Algorithms

### Q1: Implement Quick Sort with optimizations

**Time Complexity**: Average O(n log n), Worst O(n²)  
**Space Complexity**: O(log n) due to recursion

```java
public class QuickSort {
    
    // Main quicksort method
    public static void quickSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        quickSort(arr, 0, arr.length - 1);
    }
    
    private static void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            // Partition and get pivot index
            int pivotIndex = partition(arr, low, high);
            
            // Recursively sort elements before and after partition
            quickSort(arr, low, pivotIndex - 1);
            quickSort(arr, pivotIndex + 1, high);
        }
    }
    
    // Lomuto partition scheme
    private static int partition(int[] arr, int low, int high) {
        // Choose rightmost element as pivot
        int pivot = arr[high];
        int i = low - 1; // Index of smaller element
        
        for (int j = low; j < high; j++) {
            // If current element is smaller than or equal to pivot
            if (arr[j] <= pivot) {
                i++;
                swap(arr, i, j);
            }
        }
        
        // Place pivot in correct position
        swap(arr, i + 1, high);
        return i + 1;
    }
    
    // Optimized version with median-of-three pivot selection
    public static void quickSortOptimized(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        quickSortOptimized(arr, 0, arr.length - 1);
    }
    
    private static void quickSortOptimized(int[] arr, int low, int high) {
        // Use insertion sort for small arrays
        if (high - low + 1 < 10) {
            insertionSort(arr, low, high);
            return;
        }
        
        if (low < high) {
            // Use median-of-three for pivot selection
            int pivotIndex = medianOfThree(arr, low, high);
            swap(arr, pivotIndex, high); // Move pivot to end
            
            int partitionIndex = partition(arr, low, high);
            
            quickSortOptimized(arr, low, partitionIndex - 1);
            quickSortOptimized(arr, partitionIndex + 1, high);
        }
    }
    
    private static int medianOfThree(int[] arr, int low, int high) {
        int mid = low + (high - low) / 2;
        
        if (arr[mid] < arr[low]) {
            swap(arr, low, mid);
        }
        if (arr[high] < arr[low]) {
            swap(arr, low, high);
        }
        if (arr[high] < arr[mid]) {
            swap(arr, mid, high);
        }
        
        return mid;
    }
    
    private static void insertionSort(int[] arr, int low, int high) {
        for (int i = low + 1; i <= high; i++) {
            int key = arr[i];
            int j = i - 1;
            
            while (j >= low && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }
    
    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
    
    // 3-way quicksort for arrays with many duplicates
    public static void quickSort3Way(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        quickSort3Way(arr, 0, arr.length - 1);
    }
    
    private static void quickSort3Way(int[] arr, int low, int high) {
        if (low >= high) return;
        
        int lt = low;      // arr[low..lt-1] < pivot
        int gt = high;     // arr[gt+1..high] > pivot
        int i = low + 1;   // arr[lt..i-1] == pivot
        int pivot = arr[low];
        
        while (i <= gt) {
            if (arr[i] < pivot) {
                swap(arr, lt++, i++);
            } else if (arr[i] > pivot) {
                swap(arr, i, gt--);
            } else {
                i++;
            }
        }
        
        quickSort3Way(arr, low, lt - 1);
        quickSort3Way(arr, gt + 1, high);
    }
}
```

### Q2: Implement Merge Sort

**Time Complexity**: O(n log n) in all cases  
**Space Complexity**: O(n)

```java
public class MergeSort {
    
    public static void mergeSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        
        int[] temp = new int[arr.length];
        mergeSort(arr, temp, 0, arr.length - 1);
    }
    
    private static void mergeSort(int[] arr, int[] temp, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            
            // Recursively sort both halves
            mergeSort(arr, temp, left, mid);
            mergeSort(arr, temp, mid + 1, right);
            
            // Merge the sorted halves
            merge(arr, temp, left, mid, right);
        }
    }
    
    private static void merge(int[] arr, int[] temp, int left, int mid, int right) {
        // Copy elements to temporary array
        System.arraycopy(arr, left, temp, left, right - left + 1);
        
        int i = left;      // Initial index of left subarray
        int j = mid + 1;   // Initial index of right subarray
        int k = left;      // Initial index of merged array
        
        // Merge the temp arrays back into arr[left..right]
        while (i <= mid && j <= right) {
            if (temp[i] <= temp[j]) {
                arr[k++] = temp[i++];
            } else {
                arr[k++] = temp[j++];
            }
        }
        
        // Copy remaining elements of left subarray
        while (i <= mid) {
            arr[k++] = temp[i++];
        }
        
        // Copy remaining elements of right subarray
        while (j <= right) {
            arr[k++] = temp[j++];
        }
    }
    
    // Bottom-up merge sort (iterative)
    public static void mergeSortIterative(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        
        int n = arr.length;
        int[] temp = new int[n];
        
        // Merge subarrays of size 1, 2, 4, 8, ...
        for (int size = 1; size < n; size *= 2) {
            for (int left = 0; left < n - size; left += 2 * size) {
                int mid = left + size - 1;
                int right = Math.min(left + 2 * size - 1, n - 1);
                
                merge(arr, temp, left, mid, right);
            }
        }
    }
}
```

### Q3: Heap Sort Implementation

**Time Complexity**: O(n log n)  
**Space Complexity**: O(1)

```java
public class HeapSort {
    
    public static void heapSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        
        int n = arr.length;
        
        // Build max heap
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }
        
        // Extract elements from heap one by one
        for (int i = n - 1; i > 0; i--) {
            // Move current root to end
            swap(arr, 0, i);
            
            // Call heapify on the reduced heap
            heapify(arr, i, 0);
        }
    }
    
    private static void heapify(int[] arr, int n, int i) {
        int largest = i;       // Initialize largest as root
        int left = 2 * i + 1;  // Left child
        int right = 2 * i + 2; // Right child
        
        // If left child is larger than root
        if (left < n && arr[left] > arr[largest]) {
            largest = left;
        }
        
        // If right child is larger than largest so far
        if (right < n && arr[right] > arr[largest]) {
            largest = right;
        }
        
        // If largest is not root
        if (largest != i) {
            swap(arr, i, largest);
            
            // Recursively heapify the affected sub-tree
            heapify(arr, n, largest);
        }
    }
    
    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
```

---

## Searching Algorithms

### Q4: Binary Search and Variants

```java
public class BinarySearch {
    
    // Standard binary search
    public static int binarySearch(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (arr[mid] == target) {
                return mid;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return -1; // Not found
    }
    
    // Find first occurrence of target
    public static int findFirst(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        int result = -1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (arr[mid] == target) {
                result = mid;
                right = mid - 1; // Continue searching left
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return result;
    }
    
    // Find last occurrence of target
    public static int findLast(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        int result = -1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (arr[mid] == target) {
                result = mid;
                left = mid + 1; // Continue searching right
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return result;
    }
    
    // Search in rotated sorted array
    public static int searchRotated(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (arr[mid] == target) {
                return mid;
            }
            
            // Left half is sorted
            if (arr[left] <= arr[mid]) {
                if (target >= arr[left] && target < arr[mid]) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            }
            // Right half is sorted
            else {
                if (target > arr[mid] && target <= arr[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        
        return -1;
    }
    
    // Find peak element (element greater than its neighbors)
    public static int findPeak(int[] arr) {
        int left = 0, right = arr.length - 1;
        
        while (left < right) {
            int mid = left + (right - left) / 2;
            
            if (arr[mid] < arr[mid + 1]) {
                left = mid + 1; // Peak is on the right
            } else {
                right = mid; // Peak is on the left or at mid
            }
        }
        
        return left;
    }
    
    // Square root using binary search
    public static int sqrt(int x) {
        if (x < 2) return x;
        
        int left = 2, right = x / 2;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            long square = (long) mid * mid;
            
            if (square == x) {
                return mid;
            } else if (square < x) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return right;
    }
}
```

---

## Dynamic Programming

### Q5: Classic DP Problems

```java
public class DynamicProgramming {
    
    // 1. Fibonacci with memoization
    public static int fibonacci(int n) {
        int[] memo = new int[n + 1];
        Arrays.fill(memo, -1);
        return fibonacciMemo(n, memo);
    }
    
    private static int fibonacciMemo(int n, int[] memo) {
        if (n <= 1) return n;
        
        if (memo[n] != -1) return memo[n];
        
        memo[n] = fibonacciMemo(n - 1, memo) + fibonacciMemo(n - 2, memo);
        return memo[n];
    }
    
    // Fibonacci bottom-up (space optimized)
    public static int fibonacciOptimized(int n) {
        if (n <= 1) return n;
        
        int prev2 = 0, prev1 = 1;
        
        for (int i = 2; i <= n; i++) {
            int current = prev1 + prev2;
            prev2 = prev1;
            prev1 = current;
        }
        
        return prev1;
    }
    
    // 2. Climbing Stairs
    public static int climbStairs(int n) {
        if (n <= 2) return n;
        
        int[] dp = new int[n + 1];
        dp[1] = 1;
        dp[2] = 2;
        
        for (int i = 3; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }
        
        return dp[n];
    }
    
    // 3. Coin Change - Minimum coins
    public static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1); // Initialize with impossible value
        dp[0] = 0;
        
        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }
        
        return dp[amount] > amount ? -1 : dp[amount];
    }
    
    // 4. Longest Increasing Subsequence
    public static int lengthOfLIS(int[] nums) {
        if (nums.length == 0) return 0;
        
        int[] dp = new int[nums.length];
        Arrays.fill(dp, 1);
        int maxLength = 1;
        
        for (int i = 1; i < nums.length; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLength = Math.max(maxLength, dp[i]);
        }
        
        return maxLength;
    }
    
    // LIS with binary search - O(n log n)
    public static int lengthOfLISOptimized(int[] nums) {
        List<Integer> tails = new ArrayList<>();
        
        for (int num : nums) {
            int pos = Collections.binarySearch(tails, num);
            if (pos < 0) {
                pos = -(pos + 1);
            }
            
            if (pos == tails.size()) {
                tails.add(num);
            } else {
                tails.set(pos, num);
            }
        }
        
        return tails.size();
    }
    
    // 5. 0/1 Knapsack Problem
    public static int knapsack(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];
        
        for (int i = 1; i <= n; i++) {
            for (int w = 1; w <= capacity; w++) {
                if (weights[i - 1] <= w) {
                    // Max of including or excluding current item
                    dp[i][w] = Math.max(
                        values[i - 1] + dp[i - 1][w - weights[i - 1]], // Include
                        dp[i - 1][w] // Exclude
                    );
                } else {
                    dp[i][w] = dp[i - 1][w]; // Cannot include
                }
            }
        }
        
        return dp[n][capacity];
    }
    
    // Space optimized knapsack
    public static int knapsackOptimized(int[] weights, int[] values, int capacity) {
        int[] dp = new int[capacity + 1];
        
        for (int i = 0; i < weights.length; i++) {
            // Traverse from right to left to avoid using updated values
            for (int w = capacity; w >= weights[i]; w--) {
                dp[w] = Math.max(dp[w], dp[w - weights[i]] + values[i]);
            }
        }
        
        return dp[capacity];
    }
    
    // 6. Edit Distance (Levenshtein Distance)
    public static int editDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        // Base cases
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i; // Delete all characters from word1
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j; // Insert all characters to word1
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // No operation needed
                } else {
                    dp[i][j] = 1 + Math.min(
                        Math.min(dp[i - 1][j],     // Delete
                                dp[i][j - 1]),     // Insert
                        dp[i - 1][j - 1]           // Replace
                    );
                }
            }
        }
        
        return dp[m][n];
    }
    
    // 7. Maximum Subarray Sum (Kadane's Algorithm)
    public static int maxSubArray(int[] nums) {
        int maxSoFar = nums[0];
        int maxEndingHere = nums[0];
        
        for (int i = 1; i < nums.length; i++) {
            maxEndingHere = Math.max(nums[i], maxEndingHere + nums[i]);
            maxSoFar = Math.max(maxSoFar, maxEndingHere);
        }
        
        return maxSoFar;
    }
    
    // Return the actual subarray with maximum sum
    public static int[] maxSubArrayWithIndices(int[] nums) {
        int maxSum = nums[0];
        int currentSum = nums[0];
        int start = 0, end = 0, tempStart = 0;
        
        for (int i = 1; i < nums.length; i++) {
            if (currentSum < 0) {
                currentSum = nums[i];
                tempStart = i;
            } else {
                currentSum += nums[i];
            }
            
            if (currentSum > maxSum) {
                maxSum = currentSum;
                start = tempStart;
                end = i;
            }
        }
        
        return Arrays.copyOfRange(nums, start, end + 1);
    }
    
    // 8. House Robber
    public static int rob(int[] nums) {
        if (nums.length == 0) return 0;
        if (nums.length == 1) return nums[0];
        
        int prev2 = nums[0];
        int prev1 = Math.max(nums[0], nums[1]);
        
        for (int i = 2; i < nums.length; i++) {
            int current = Math.max(prev1, prev2 + nums[i]);
            prev2 = prev1;
            prev1 = current;
        }
        
        return prev1;
    }
    
    // House Robber II (circular array)
    public static int robCircular(int[] nums) {
        if (nums.length == 0) return 0;
        if (nums.length == 1) return nums[0];
        if (nums.length == 2) return Math.max(nums[0], nums[1]);
        
        // Case 1: Rob houses 0 to n-2 (exclude last house)
        int case1 = robLinear(nums, 0, nums.length - 2);
        
        // Case 2: Rob houses 1 to n-1 (exclude first house)
        int case2 = robLinear(nums, 1, nums.length - 1);
        
        return Math.max(case1, case2);
    }
    
    private static int robLinear(int[] nums, int start, int end) {
        int prev2 = 0, prev1 = 0;
        
        for (int i = start; i <= end; i++) {
            int current = Math.max(prev1, prev2 + nums[i]);
            prev2 = prev1;
            prev1 = current;
        }
        
        return prev1;
    }
}
```

---

## Graph Algorithms

### Q6: Graph Traversal and Shortest Path

```java
import java.util.*;

public class GraphAlgorithms {
    
    // Graph representation using adjacency list
    static class Graph {
        private int vertices;
        private List<List<Integer>> adjList;
        
        public Graph(int vertices) {
            this.vertices = vertices;
            this.adjList = new ArrayList<>();
            for (int i = 0; i < vertices; i++) {
                adjList.add(new ArrayList<>());
            }
        }
        
        public void addEdge(int source, int destination) {
            adjList.get(source).add(destination);
            adjList.get(destination).add(source); // For undirected graph
        }
        
        public void addDirectedEdge(int source, int destination) {
            adjList.get(source).add(destination);
        }
        
        public List<Integer> getNeighbors(int vertex) {
            return adjList.get(vertex);
        }
        
        public int getVertices() {
            return vertices;
        }
    }
    
    // Breadth-First Search
    public static List<Integer> bfs(Graph graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];
        Queue<Integer> queue = new LinkedList<>();
        
        visited[start] = true;
        queue.offer(start);
        
        while (!queue.isEmpty()) {
            int vertex = queue.poll();
            result.add(vertex);
            
            for (int neighbor : graph.getNeighbors(vertex)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }
        }
        
        return result;
    }
    
    // Depth-First Search
    public static List<Integer> dfs(Graph graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];
        dfsRecursive(graph, start, visited, result);
        return result;
    }
    
    private static void dfsRecursive(Graph graph, int vertex, boolean[] visited, List<Integer> result) {
        visited[vertex] = true;
        result.add(vertex);
        
        for (int neighbor : graph.getNeighbors(vertex)) {
            if (!visited[neighbor]) {
                dfsRecursive(graph, neighbor, visited, result);
            }
        }
    }
    
    // DFS Iterative
    public static List<Integer> dfsIterative(Graph graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];
        Stack<Integer> stack = new Stack<>();
        
        stack.push(start);
        
        while (!stack.isEmpty()) {
            int vertex = stack.pop();
            
            if (!visited[vertex]) {
                visited[vertex] = true;
                result.add(vertex);
                
                // Add neighbors in reverse order to maintain left-to-right traversal
                List<Integer> neighbors = graph.getNeighbors(vertex);
                for (int i = neighbors.size() - 1; i >= 0; i--) {
                    if (!visited[neighbors.get(i)]) {
                        stack.push(neighbors.get(i));
                    }
                }
            }
        }
        
        return result;
    }
    
    // Detect cycle in undirected graph
    public static boolean hasCycleUndirected(Graph graph) {
        boolean[] visited = new boolean[graph.getVertices()];
        
        for (int i = 0; i < graph.getVertices(); i++) {
            if (!visited[i]) {
                if (hasCycleUndirectedUtil(graph, i, visited, -1)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static boolean hasCycleUndirectedUtil(Graph graph, int vertex, boolean[] visited, int parent) {
        visited[vertex] = true;
        
        for (int neighbor : graph.getNeighbors(vertex)) {
            if (!visited[neighbor]) {
                if (hasCycleUndirectedUtil(graph, neighbor, visited, vertex)) {
                    return true;
                }
            } else if (neighbor != parent) {
                return true; // Back edge found
            }
        }
        return false;
    }
    
    // Detect cycle in directed graph
    public static boolean hasCycleDirected(Graph graph) {
        boolean[] visited = new boolean[graph.getVertices()];
        boolean[] recursionStack = new boolean[graph.getVertices()];
        
        for (int i = 0; i < graph.getVertices(); i++) {
            if (!visited[i]) {
                if (hasCycleDirectedUtil(graph, i, visited, recursionStack)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static boolean hasCycleDirectedUtil(Graph graph, int vertex, boolean[] visited, boolean[] recursionStack) {
        visited[vertex] = true;
        recursionStack[vertex] = true;
        
        for (int neighbor : graph.getNeighbors(vertex)) {
            if (!visited[neighbor] && hasCycleDirectedUtil(graph, neighbor, visited, recursionStack)) {
                return true;
            } else if (recursionStack[neighbor]) {
                return true; // Back edge in DFS tree
            }
        }
        
        recursionStack[vertex] = false;
        return false;
    }
    
    // Topological Sort (DFS-based)
    public static List<Integer> topologicalSort(Graph graph) {
        boolean[] visited = new boolean[graph.getVertices()];
        Stack<Integer> stack = new Stack<>();
        
        for (int i = 0; i < graph.getVertices(); i++) {
            if (!visited[i]) {
                topologicalSortUtil(graph, i, visited, stack);
            }
        }
        
        List<Integer> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        
        return result;
    }
    
    private static void topologicalSortUtil(Graph graph, int vertex, boolean[] visited, Stack<Integer> stack) {
        visited[vertex] = true;
        
        for (int neighbor : graph.getNeighbors(vertex)) {
            if (!visited[neighbor]) {
                topologicalSortUtil(graph, neighbor, visited, stack);
            }
        }
        
        stack.push(vertex);
    }
    
    // Kahn's Algorithm for Topological Sort
    public static List<Integer> topologicalSortKahn(Graph graph) {
        int[] inDegree = new int[graph.getVertices()];
        
        // Calculate in-degrees
        for (int i = 0; i < graph.getVertices(); i++) {
            for (int neighbor : graph.getNeighbors(i)) {
                inDegree[neighbor]++;
            }
        }
        
        Queue<Integer> queue = new LinkedList<>();
        List<Integer> result = new ArrayList<>();
        
        // Add vertices with 0 in-degree
        for (int i = 0; i < graph.getVertices(); i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        while (!queue.isEmpty()) {
            int vertex = queue.poll();
            result.add(vertex);
            
            // Reduce in-degree of neighbors
            for (int neighbor : graph.getNeighbors(vertex)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        // Check if there's a cycle
        if (result.size() != graph.getVertices()) {
            return new ArrayList<>(); // Cycle detected
        }
        
        return result;
    }
    
    // Dijkstra's Shortest Path Algorithm
    static class WeightedGraph {
        private int vertices;
        private List<List<Edge>> adjList;
        
        static class Edge {
            int destination;
            int weight;
            
            Edge(int destination, int weight) {
                this.destination = destination;
                this.weight = weight;
            }
        }
        
        public WeightedGraph(int vertices) {
            this.vertices = vertices;
            this.adjList = new ArrayList<>();
            for (int i = 0; i < vertices; i++) {
                adjList.add(new ArrayList<>());
            }
        }
        
        public void addEdge(int source, int destination, int weight) {
            adjList.get(source).add(new Edge(destination, weight));
            adjList.get(destination).add(new Edge(source, weight)); // For undirected
        }
        
        public List<Edge> getNeighbors(int vertex) {
            return adjList.get(vertex);
        }
        
        public int getVertices() {
            return vertices;
        }
    }
    
    public static int[] dijkstra(WeightedGraph graph, int source) {
        int vertices = graph.getVertices();
        int[] distances = new int[vertices];
        Arrays.fill(distances, Integer.MAX_VALUE);
        distances[source] = 0;
        
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[]{source, 0});
        
        boolean[] visited = new boolean[vertices];
        
        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int vertex = current[0];
            int distance = current[1];
            
            if (visited[vertex]) continue;
            visited[vertex] = true;
            
            for (WeightedGraph.Edge edge : graph.getNeighbors(vertex)) {
                int neighbor = edge.destination;
                int newDistance = distance + edge.weight;
                
                if (newDistance < distances[neighbor]) {
                    distances[neighbor] = newDistance;
                    pq.offer(new int[]{neighbor, newDistance});
                }
            }
        }
        
        return distances;
    }
    
    // Floyd-Warshall Algorithm (All pairs shortest path)
    public static int[][] floydWarshall(int[][] graph) {
        int vertices = graph.length;
        int[][] dist = new int[vertices][vertices];
        
        // Initialize distances
        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                dist[i][j] = graph[i][j];
            }
        }
        
        // Add all vertices one by one to the set of intermediate vertices
        for (int k = 0; k < vertices; k++) {
            for (int i = 0; i < vertices; i++) {
                for (int j = 0; j < vertices; j++) {
                    if (dist[i][k] != Integer.MAX_VALUE && 
                        dist[k][j] != Integer.MAX_VALUE && 
                        dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                    }
                }
            }
        }
        
        return dist;
    }
}
```

This covers a comprehensive set of algorithm problems commonly asked in interviews. Would you like me to continue with the remaining sections (Two Pointers & Sliding Window, Backtracking) and then move on to create the Cloud Services, Spring Boot, and Coding Challenges files?
