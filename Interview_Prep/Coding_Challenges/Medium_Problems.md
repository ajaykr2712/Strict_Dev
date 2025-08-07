# Medium Level Coding Challenges

## Array and String Problems

### Problem 1: Longest Substring Without Repeating Characters
**Difficulty**: Medium  
**Time**: 20-25 minutes  

**Problem Statement:**
Given a string `s`, find the length of the longest substring without repeating characters.

**Examples:**
```
Input: s = "abcabcbb"
Output: 3
Explanation: The answer is "abc", with the length of 3.

Input: s = "bbbbb"
Output: 1
Explanation: The answer is "b", with the length of 1.
```

**Solution:**
```java
public class LongestSubstringWithoutRepeating {
    
    // Approach 1: Sliding Window with HashSet - O(n) time, O(min(m,n)) space
    public int lengthOfLongestSubstring(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        
        Set<Character> window = new HashSet<>();
        int left = 0, maxLength = 0;
        
        for (int right = 0; right < s.length(); right++) {
            char rightChar = s.charAt(right);
            
            // Shrink window until no duplicate
            while (window.contains(rightChar)) {
                window.remove(s.charAt(left));
                left++;
            }
            
            window.add(rightChar);
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    // Approach 2: Sliding Window with HashMap (Optimized) - O(n) time, O(min(m,n)) space
    public int lengthOfLongestSubstringOptimized(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        
        Map<Character, Integer> charIndex = new HashMap<>();
        int left = 0, maxLength = 0;
        
        for (int right = 0; right < s.length(); right++) {
            char rightChar = s.charAt(right);
            
            // If character is already seen and is in current window
            if (charIndex.containsKey(rightChar) && charIndex.get(rightChar) >= left) {
                left = charIndex.get(rightChar) + 1;
            }
            
            charIndex.put(rightChar, right);
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    // Approach 3: Array for ASCII characters - O(n) time, O(1) space
    public int lengthOfLongestSubstringArray(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        
        int[] charIndex = new int[128]; // ASCII characters
        Arrays.fill(charIndex, -1);
        
        int left = 0, maxLength = 0;
        
        for (int right = 0; right < s.length(); right++) {
            char rightChar = s.charAt(right);
            
            if (charIndex[rightChar] >= left) {
                left = charIndex[rightChar] + 1;
            }
            
            charIndex[rightChar] = right;
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    // Test cases
    public static void main(String[] args) {
        LongestSubstringWithoutRepeating solution = new LongestSubstringWithoutRepeating();
        
        // Test cases
        System.out.println(solution.lengthOfLongestSubstring("abcabcbb")); // 3
        System.out.println(solution.lengthOfLongestSubstring("bbbbb")); // 1
        System.out.println(solution.lengthOfLongestSubstring("pwwkew")); // 3
        System.out.println(solution.lengthOfLongestSubstring("")); // 0
        System.out.println(solution.lengthOfLongestSubstring("au")); // 2
    }
}
```

**Key Points:**
- Use sliding window technique
- HashMap stores last seen index of characters
- Optimize by jumping directly to position after duplicate

---

### Problem 2: 3Sum
**Difficulty**: Medium  
**Time**: 25-30 minutes  

**Problem Statement:**
Given an integer array nums, return all the triplets `[nums[i], nums[j], nums[k]]` such that `i != j`, `i != k`, and `j != k`, and `nums[i] + nums[j] + nums[k] == 0`.

**Example:**
```
Input: nums = [-1,0,1,2,-1,-4]
Output: [[-1,-1,2],[-1,0,1]]
```

**Solution:**
```java
public class ThreeSum {
    
    // Approach: Sort + Two Pointers - O(n²) time, O(1) extra space
    public List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        
        if (nums == null || nums.length < 3) {
            return result;
        }
        
        // Sort the array
        Arrays.sort(nums);
        
        for (int i = 0; i < nums.length - 2; i++) {
            // Skip duplicates for first element
            if (i > 0 && nums[i] == nums[i - 1]) {
                continue;
            }
            
            int left = i + 1;
            int right = nums.length - 1;
            int target = -nums[i]; // We want nums[left] + nums[right] = -nums[i]
            
            while (left < right) {
                int sum = nums[left] + nums[right];
                
                if (sum == target) {
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    
                    // Skip duplicates for second element
                    while (left < right && nums[left] == nums[left + 1]) {
                        left++;
                    }
                    
                    // Skip duplicates for third element
                    while (left < right && nums[right] == nums[right - 1]) {
                        right--;
                    }
                    
                    left++;
                    right--;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        
        return result;
    }
    
    // Alternative approach with HashSet for deduplication
    public List<List<Integer>> threeSumWithSet(int[] nums) {
        Set<List<Integer>> resultSet = new HashSet<>();
        
        if (nums == null || nums.length < 3) {
            return new ArrayList<>();
        }
        
        Arrays.sort(nums);
        
        for (int i = 0; i < nums.length - 2; i++) {
            int left = i + 1;
            int right = nums.length - 1;
            
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                
                if (sum == 0) {
                    resultSet.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    left++;
                    right--;
                } else if (sum < 0) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        
        return new ArrayList<>(resultSet);
    }
    
    // Test cases
    public static void main(String[] args) {
        ThreeSum solution = new ThreeSum();
        
        // Test case 1
        int[] nums1 = {-1, 0, 1, 2, -1, -4};
        System.out.println("3Sum: " + solution.threeSum(nums1));
        // Output: [[-1, -1, 2], [-1, 0, 1]]
        
        // Test case 2
        int[] nums2 = {0, 1, 1};
        System.out.println("3Sum: " + solution.threeSum(nums2));
        // Output: []
        
        // Test case 3
        int[] nums3 = {0, 0, 0};
        System.out.println("3Sum: " + solution.threeSum(nums3));
        // Output: [[0, 0, 0]]
    }
}
```

---

### Problem 3: Container With Most Water
**Difficulty**: Medium  
**Time**: 15-20 minutes  

**Problem Statement:**
You are given an integer array `height` of length `n`. There are `n` vertical lines drawn such that the two endpoints of the `i`th line are `(i, 0)` and `(i, height[i])`. Find two lines that together with the x-axis form a container that can hold the most water.

**Solution:**
```java
public class ContainerWithMostWater {
    
    // Approach 1: Two Pointers - O(n) time, O(1) space (Optimal)
    public int maxArea(int[] height) {
        if (height == null || height.length < 2) {
            return 0;
        }
        
        int left = 0;
        int right = height.length - 1;
        int maxWater = 0;
        
        while (left < right) {
            // Calculate current water area
            int width = right - left;
            int currentHeight = Math.min(height[left], height[right]);
            int currentWater = width * currentHeight;
            
            maxWater = Math.max(maxWater, currentWater);
            
            // Move the pointer with smaller height
            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }
        
        return maxWater;
    }
    
    // Approach 2: Brute Force - O(n²) time, O(1) space
    public int maxAreaBruteForce(int[] height) {
        int maxWater = 0;
        
        for (int i = 0; i < height.length; i++) {
            for (int j = i + 1; j < height.length; j++) {
                int width = j - i;
                int currentHeight = Math.min(height[i], height[j]);
                int currentWater = width * currentHeight;
                
                maxWater = Math.max(maxWater, currentWater);
            }
        }
        
        return maxWater;
    }
    
    // Detailed explanation with step-by-step trace
    public int maxAreaWithTrace(int[] height) {
        int left = 0;
        int right = height.length - 1;
        int maxWater = 0;
        
        System.out.println("Tracing Container With Most Water:");
        
        while (left < right) {
            int width = right - left;
            int currentHeight = Math.min(height[left], height[right]);
            int currentWater = width * currentHeight;
            
            System.out.printf("Left: %d (height: %d), Right: %d (height: %d), " +
                            "Width: %d, Height: %d, Water: %d%n",
                            left, height[left], right, height[right],
                            width, currentHeight, currentWater);
            
            maxWater = Math.max(maxWater, currentWater);
            
            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }
        
        return maxWater;
    }
    
    // Test cases
    public static void main(String[] args) {
        ContainerWithMostWater solution = new ContainerWithMostWater();
        
        // Test case 1
        int[] height1 = {1, 8, 6, 2, 5, 4, 8, 3, 7};
        System.out.println("Max water: " + solution.maxArea(height1)); // 49
        
        // Test case 2
        int[] height2 = {1, 1};
        System.out.println("Max water: " + solution.maxArea(height2)); // 1
        
        // Test case 3 with trace
        int[] height3 = {1, 8, 6, 2, 5, 4, 8, 3, 7};
        System.out.println("Max water with trace: " + solution.maxAreaWithTrace(height3));
    }
}
```

---

## Tree Problems

### Problem 4: Binary Tree Level Order Traversal
**Difficulty**: Medium  
**Time**: 15-20 minutes  

**Problem Statement:**
Given the root of a binary tree, return the level order traversal of its nodes' values. (i.e., from left to right, level by level).

**Solution:**
```java
public class BinaryTreeLevelOrder {
    
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        
        TreeNode() {}
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }
    
    // Approach 1: BFS with Queue - O(n) time, O(w) space where w is max width
    public List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        
        if (root == null) {
            return result;
        }
        
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> currentLevel = new ArrayList<>();
            
            // Process all nodes at current level
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                currentLevel.add(node.val);
                
                // Add children for next level
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
            
            result.add(currentLevel);
        }
        
        return result;
    }
    
    // Approach 2: DFS Recursive - O(n) time, O(h) space where h is height
    public List<List<Integer>> levelOrderDFS(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        dfsHelper(root, 0, result);
        return result;
    }
    
    private void dfsHelper(TreeNode node, int level, List<List<Integer>> result) {
        if (node == null) {
            return;
        }
        
        // Create new level list if needed
        if (level >= result.size()) {
            result.add(new ArrayList<>());
        }
        
        // Add current node to its level
        result.get(level).add(node.val);
        
        // Recurse for children
        dfsHelper(node.left, level + 1, result);
        dfsHelper(node.right, level + 1, result);
    }
    
    // Bonus: Level order traversal from bottom to top
    public List<List<Integer>> levelOrderBottom(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        
        if (root == null) {
            return result;
        }
        
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> currentLevel = new ArrayList<>();
            
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                currentLevel.add(node.val);
                
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
            
            result.add(0, currentLevel); // Add to beginning for bottom-up
        }
        
        return result;
    }
    
    // Test cases
    public static void main(String[] args) {
        BinaryTreeLevelOrder solution = new BinaryTreeLevelOrder();
        
        // Create test tree: [3,9,20,null,null,15,7]
        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(9);
        root.right = new TreeNode(20);
        root.right.left = new TreeNode(15);
        root.right.right = new TreeNode(7);
        
        System.out.println("Level order (BFS): " + solution.levelOrder(root));
        // Output: [[3], [9, 20], [15, 7]]
        
        System.out.println("Level order (DFS): " + solution.levelOrderDFS(root));
        // Output: [[3], [9, 20], [15, 7]]
        
        System.out.println("Level order bottom: " + solution.levelOrderBottom(root));
        // Output: [[15, 7], [9, 20], [3]]
    }
}
```

---

### Problem 5: Validate Binary Search Tree
**Difficulty**: Medium  
**Time**: 20-25 minutes  

**Problem Statement:**
Given the root of a binary tree, determine if it is a valid binary search tree (BST).

**Solution:**
```java
public class ValidateBST {
    
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        
        TreeNode() {}
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }
    
    // Approach 1: Recursive with bounds - O(n) time, O(h) space
    public boolean isValidBST(TreeNode root) {
        return validate(root, null, null);
    }
    
    private boolean validate(TreeNode node, Integer min, Integer max) {
        // Empty tree is valid BST
        if (node == null) {
            return true;
        }
        
        // Check if current node violates BST property
        if ((min != null && node.val <= min) || (max != null && node.val >= max)) {
            return false;
        }
        
        // Recursively validate left and right subtrees with updated bounds
        return validate(node.left, min, node.val) && validate(node.right, node.val, max);
    }
    
    // Approach 2: Inorder traversal - O(n) time, O(h) space
    public boolean isValidBSTInorder(TreeNode root) {
        List<Integer> inorderList = new ArrayList<>();
        inorderTraversal(root, inorderList);
        
        // Check if inorder traversal is strictly increasing
        for (int i = 1; i < inorderList.size(); i++) {
            if (inorderList.get(i) <= inorderList.get(i - 1)) {
                return false;
            }
        }
        
        return true;
    }
    
    private void inorderTraversal(TreeNode node, List<Integer> result) {
        if (node == null) {
            return;
        }
        
        inorderTraversal(node.left, result);
        result.add(node.val);
        inorderTraversal(node.right, result);
    }
    
    // Approach 3: Optimized inorder with previous value tracking
    private Integer previousValue = null;
    
    public boolean isValidBSTOptimized(TreeNode root) {
        previousValue = null; // Reset for each call
        return inorderCheck(root);
    }
    
    private boolean inorderCheck(TreeNode node) {
        if (node == null) {
            return true;
        }
        
        // Check left subtree
        if (!inorderCheck(node.left)) {
            return false;
        }
        
        // Check current node
        if (previousValue != null && node.val <= previousValue) {
            return false;
        }
        previousValue = node.val;
        
        // Check right subtree
        return inorderCheck(node.right);
    }
    
    // Approach 4: Iterative inorder - O(n) time, O(h) space
    public boolean isValidBSTIterative(TreeNode root) {
        Stack<TreeNode> stack = new Stack<>();
        Integer previousValue = null;
        TreeNode current = root;
        
        while (!stack.isEmpty() || current != null) {
            // Go to leftmost node
            while (current != null) {
                stack.push(current);
                current = current.left;
            }
            
            // Process current node
            current = stack.pop();
            
            // Check BST property
            if (previousValue != null && current.val <= previousValue) {
                return false;
            }
            previousValue = current.val;
            
            // Move to right subtree
            current = current.right;
        }
        
        return true;
    }
    
    // Test cases
    public static void main(String[] args) {
        ValidateBST solution = new ValidateBST();
        
        // Test case 1: Valid BST [2,1,3]
        TreeNode root1 = new TreeNode(2);
        root1.left = new TreeNode(1);
        root1.right = new TreeNode(3);
        System.out.println("Is valid BST: " + solution.isValidBST(root1)); // true
        
        // Test case 2: Invalid BST [5,1,4,null,null,3,6]
        TreeNode root2 = new TreeNode(5);
        root2.left = new TreeNode(1);
        root2.right = new TreeNode(4);
        root2.right.left = new TreeNode(3);
        root2.right.right = new TreeNode(6);
        System.out.println("Is valid BST: " + solution.isValidBST(root2)); // false
        
        // Test case 3: Edge case with Integer.MIN_VALUE
        TreeNode root3 = new TreeNode(Integer.MIN_VALUE);
        root3.right = new TreeNode(Integer.MIN_VALUE + 1);
        System.out.println("Is valid BST: " + solution.isValidBST(root3)); // true
    }
}
```

---

## Dynamic Programming Problems

### Problem 6: Coin Change
**Difficulty**: Medium  
**Time**: 20-25 minutes  

**Problem Statement:**
You are given an integer array `coins` representing coins of different denominations and an integer `amount` representing a total amount of money. Return the fewest number of coins that you need to make up that amount. If that amount of money cannot be made up by any combination of the coins, return -1.

**Solution:**
```java
public class CoinChange {
    
    // Approach 1: Dynamic Programming (Bottom-up) - O(amount × coins) time, O(amount) space
    public int coinChange(int[] coins, int amount) {
        if (amount == 0) {
            return 0;
        }
        
        if (coins == null || coins.length == 0) {
            return -1;
        }
        
        // dp[i] represents minimum coins needed for amount i
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1); // Initialize with impossible value
        dp[0] = 0; // Base case: 0 coins needed for amount 0
        
        // For each amount from 1 to target amount
        for (int i = 1; i <= amount; i++) {
            // Try each coin
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }
        
        return dp[amount] > amount ? -1 : dp[amount];
    }
    
    // Approach 2: Recursive with Memoization (Top-down) - O(amount × coins) time, O(amount) space
    public int coinChangeRecursive(int[] coins, int amount) {
        if (amount == 0) {
            return 0;
        }
        
        Integer[] memo = new Integer[amount + 1];
        int result = dfs(coins, amount, memo);
        return result == Integer.MAX_VALUE ? -1 : result;
    }
    
    private int dfs(int[] coins, int amount, Integer[] memo) {
        if (amount == 0) {
            return 0;
        }
        
        if (amount < 0) {
            return Integer.MAX_VALUE;
        }
        
        if (memo[amount] != null) {
            return memo[amount];
        }
        
        int minCoins = Integer.MAX_VALUE;
        for (int coin : coins) {
            int result = dfs(coins, amount - coin, memo);
            if (result != Integer.MAX_VALUE) {
                minCoins = Math.min(minCoins, result + 1);
            }
        }
        
        memo[amount] = minCoins;
        return minCoins;
    }
    
    // Approach 3: BFS - O(amount × coins) time, O(amount) space
    public int coinChangeBFS(int[] coins, int amount) {
        if (amount == 0) {
            return 0;
        }
        
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        
        queue.offer(0);
        visited.add(0);
        int level = 0;
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            level++;
            
            for (int i = 0; i < size; i++) {
                int current = queue.poll();
                
                for (int coin : coins) {
                    int next = current + coin;
                    
                    if (next == amount) {
                        return level;
                    }
                    
                    if (next < amount && !visited.contains(next)) {
                        visited.add(next);
                        queue.offer(next);
                    }
                }
            }
        }
        
        return -1;
    }
    
    // Helper method to trace the solution (which coins are used)
    public List<Integer> coinChangeWithSolution(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        int[] parent = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        Arrays.fill(parent, -1);
        dp[0] = 0;
        
        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i && dp[i - coin] + 1 < dp[i]) {
                    dp[i] = dp[i - coin] + 1;
                    parent[i] = coin;
                }
            }
        }
        
        if (dp[amount] > amount) {
            return new ArrayList<>(); // No solution
        }
        
        // Reconstruct solution
        List<Integer> result = new ArrayList<>();
        int current = amount;
        while (current > 0) {
            result.add(parent[current]);
            current -= parent[current];
        }
        
        return result;
    }
    
    // Test cases
    public static void main(String[] args) {
        CoinChange solution = new CoinChange();
        
        // Test case 1
        int[] coins1 = {1, 3, 4};
        int amount1 = 6;
        System.out.println("Min coins for amount " + amount1 + ": " + 
                         solution.coinChange(coins1, amount1)); // 2 (3 + 3)
        
        // Test case 2
        int[] coins2 = {2};
        int amount2 = 3;
        System.out.println("Min coins for amount " + amount2 + ": " + 
                         solution.coinChange(coins2, amount2)); // -1
        
        // Test case 3
        int[] coins3 = {1, 3, 4};
        int amount3 = 6;
        System.out.println("Coins used: " + solution.coinChangeWithSolution(coins3, amount3));
        // Output: [3, 3] or [4, 1, 1] depending on implementation
    }
}
```

---

## Backtracking Problems

### Problem 7: Generate Parentheses
**Difficulty**: Medium  
**Time**: 20-25 minutes  

**Problem Statement:**
Given `n` pairs of parentheses, write a function to generate all combinations of well-formed parentheses.

**Solution:**
```java
public class GenerateParentheses {
    
    // Approach 1: Backtracking - O(4^n / √n) time, O(4^n / √n) space
    public List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();
        backtrack(result, new StringBuilder(), 0, 0, n);
        return result;
    }
    
    private void backtrack(List<String> result, StringBuilder current, 
                          int open, int close, int max) {
        // Base case: we have used all pairs
        if (current.length() == max * 2) {
            result.add(current.toString());
            return;
        }
        
        // Add opening parenthesis if we haven't used all
        if (open < max) {
            current.append('(');
            backtrack(result, current, open + 1, close, max);
            current.deleteCharAt(current.length() - 1); // backtrack
        }
        
        // Add closing parenthesis if it would be valid
        if (close < open) {
            current.append(')');
            backtrack(result, current, open, close + 1, max);
            current.deleteCharAt(current.length() - 1); // backtrack
        }
    }
    
    // Approach 2: Iterative with Queue - O(4^n / √n) time, O(4^n / √n) space
    public List<String> generateParenthesisIterative(int n) {
        List<String> result = new ArrayList<>();
        Queue<ParenthesesState> queue = new LinkedList<>();
        queue.offer(new ParenthesesState("", 0, 0));
        
        while (!queue.isEmpty()) {
            ParenthesesState current = queue.poll();
            
            if (current.str.length() == 2 * n) {
                result.add(current.str);
                continue;
            }
            
            // Add opening parenthesis
            if (current.open < n) {
                queue.offer(new ParenthesesState(
                    current.str + "(", current.open + 1, current.close));
            }
            
            // Add closing parenthesis
            if (current.close < current.open) {
                queue.offer(new ParenthesesState(
                    current.str + ")", current.open, current.close + 1));
            }
        }
        
        return result;
    }
    
    static class ParenthesesState {
        String str;
        int open;
        int close;
        
        ParenthesesState(String str, int open, int close) {
            this.str = str;
            this.open = open;
            this.close = close;
        }
    }
    
    // Approach 3: Dynamic Programming - Build from smaller solutions
    public List<String> generateParenthesisDP(int n) {
        List<List<String>> dp = new ArrayList<>();
        
        // Base case
        dp.add(Arrays.asList(""));
        
        for (int i = 1; i <= n; i++) {
            List<String> current = new ArrayList<>();
            
            for (int j = 0; j < i; j++) {
                List<String> left = dp.get(j);
                List<String> right = dp.get(i - 1 - j);
                
                for (String l : left) {
                    for (String r : right) {
                        current.add("(" + l + ")" + r);
                    }
                }
            }
            
            dp.add(current);
        }
        
        return dp.get(n);
    }
    
    // Helper method to validate parentheses (for testing)
    public boolean isValid(String s) {
        int count = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') {
                count++;
            } else if (c == ')') {
                count--;
                if (count < 0) {
                    return false;
                }
            }
        }
        return count == 0;
    }
    
    // Test cases
    public static void main(String[] args) {
        GenerateParentheses solution = new GenerateParentheses();
        
        // Test case 1
        System.out.println("n=1: " + solution.generateParenthesis(1));
        // Output: ["()"]
        
        // Test case 2
        System.out.println("n=2: " + solution.generateParenthesis(2));
        // Output: ["(())", "()()"]
        
        // Test case 3
        System.out.println("n=3: " + solution.generateParenthesis(3));
        // Output: ["((()))", "(()())", "(())()", "()(())", "()()()"]
        
        // Validate all results
        List<String> result = solution.generateParenthesis(3);
        boolean allValid = result.stream().allMatch(solution::isValid);
        System.out.println("All results valid: " + allValid);
    }
}
```

---

## Tips for Medium Problems:

### 1. **Pattern Recognition**
- **Sliding Window**: Variable size windows, substring problems
- **Two Pointers**: Sorted arrays, palindromes, 3Sum type problems
- **Dynamic Programming**: Optimization problems, counting problems
- **Backtracking**: Generate all combinations, constraint satisfaction
- **Tree Traversal**: Level order (BFS), validation (DFS with bounds)

### 2. **Optimization Strategies**
- Start with brute force, then optimize
- Use memoization for recursive solutions
- Consider space-time tradeoffs
- Look for mathematical insights

### 3. **Common Gotchas**
- Integer overflow
- Empty/null inputs
- Duplicate handling in arrays
- Tree node validation
- Off-by-one errors in indices

### 4. **Interview Approach**
- Clarify requirements and constraints
- Discuss multiple approaches
- Implement the most efficient solution
- Test with edge cases
- Discuss further optimizations

These medium problems test your ability to apply multiple algorithms and data structures to solve more complex scenarios. Master these patterns and you'll be well-prepared for most technical interviews!
