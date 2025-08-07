# Easy Level Coding Challenges

## Array and String Problems

### Problem 1: Two Sum
**Difficulty**: Easy  
**Time**: 10-15 minutes  

**Problem Statement:**
Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to target. You may assume that each input would have exactly one solution, and you may not use the same element twice.

**Example:**
```
Input: nums = [2,7,11,15], target = 9
Output: [0,1]
Explanation: Because nums[0] + nums[1] == 9, we return [0, 1].
```

**Solution:**
```java
public class TwoSum {
    
    // Approach 1: Brute Force - O(n²) time, O(1) space
    public int[] twoSumBruteForce(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int[]{i, j};
                }
            }
        }
        throw new IllegalArgumentException("No two sum solution");
    }
    
    // Approach 2: HashMap - O(n) time, O(n) space (Optimal)
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[]{map.get(complement), i};
            }
            map.put(nums[i], i);
        }
        
        throw new IllegalArgumentException("No two sum solution");
    }
    
    // Test cases
    public static void main(String[] args) {
        TwoSum solution = new TwoSum();
        
        // Test case 1
        int[] nums1 = {2, 7, 11, 15};
        int target1 = 9;
        int[] result1 = solution.twoSum(nums1, target1);
        System.out.println("Result: [" + result1[0] + ", " + result1[1] + "]"); // [0, 1]
        
        // Test case 2
        int[] nums2 = {3, 2, 4};
        int target2 = 6;
        int[] result2 = solution.twoSum(nums2, target2);
        System.out.println("Result: [" + result2[0] + ", " + result2[1] + "]"); // [1, 2]
        
        // Test case 3
        int[] nums3 = {3, 3};
        int target3 = 6;
        int[] result3 = solution.twoSum(nums3, target3);
        System.out.println("Result: [" + result3[0] + ", " + result3[1] + "]"); // [0, 1]
    }
}
```

**Key Points:**
- HashMap approach is optimal with O(n) time complexity
- Store complement and index for quick lookup
- Handle edge cases like duplicate values

---

### Problem 2: Valid Palindrome
**Difficulty**: Easy  
**Time**: 10-15 minutes  

**Problem Statement:**
A phrase is a palindrome if, after converting all uppercase letters into lowercase letters and removing all non-alphanumeric characters, it reads the same forward and backward.

**Example:**
```
Input: s = "A man, a plan, a canal: Panama"
Output: true
Explanation: "amanaplanacanalpanama" is a palindrome.
```

**Solution:**
```java
public class ValidPalindrome {
    
    // Approach 1: Two pointers - O(n) time, O(1) space
    public boolean isPalindrome(String s) {
        if (s == null || s.length() <= 1) {
            return true;
        }
        
        int left = 0;
        int right = s.length() - 1;
        
        while (left < right) {
            // Skip non-alphanumeric characters from left
            while (left < right && !Character.isLetterOrDigit(s.charAt(left))) {
                left++;
            }
            
            // Skip non-alphanumeric characters from right
            while (left < right && !Character.isLetterOrDigit(s.charAt(right))) {
                right--;
            }
            
            // Compare characters (case-insensitive)
            if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
                return false;
            }
            
            left++;
            right--;
        }
        
        return true;
    }
    
    // Approach 2: Clean string first - O(n) time, O(n) space
    public boolean isPalindromeCleanFirst(String s) {
        // Clean the string
        StringBuilder cleaned = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                cleaned.append(Character.toLowerCase(c));
            }
        }
        
        // Check if palindrome
        String cleanedStr = cleaned.toString();
        String reversed = cleaned.reverse().toString();
        return cleanedStr.equals(reversed);
    }
    
    // Test cases
    public static void main(String[] args) {
        ValidPalindrome solution = new ValidPalindrome();
        
        // Test case 1
        System.out.println(solution.isPalindrome("A man, a plan, a canal: Panama")); // true
        
        // Test case 2
        System.out.println(solution.isPalindrome("race a car")); // false
        
        // Test case 3
        System.out.println(solution.isPalindrome(" ")); // true
        
        // Test case 4
        System.out.println(solution.isPalindrome("Madam")); // true
    }
}
```

---

### Problem 3: Valid Anagram
**Difficulty**: Easy  
**Time**: 10-15 minutes  

**Problem Statement:**
Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise.

**Example:**
```
Input: s = "anagram", t = "nagaram"
Output: true
```

**Solution:**
```java
public class ValidAnagram {
    
    // Approach 1: Sorting - O(n log n) time, O(1) space
    public boolean isAnagramSorting(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }
        
        char[] sArray = s.toCharArray();
        char[] tArray = t.toCharArray();
        
        Arrays.sort(sArray);
        Arrays.sort(tArray);
        
        return Arrays.equals(sArray, tArray);
    }
    
    // Approach 2: Character count - O(n) time, O(1) space (optimal)
    public boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }
        
        int[] charCount = new int[26]; // For lowercase English letters
        
        for (int i = 0; i < s.length(); i++) {
            charCount[s.charAt(i) - 'a']++;
            charCount[t.charAt(i) - 'a']--;
        }
        
        for (int count : charCount) {
            if (count != 0) {
                return false;
            }
        }
        
        return true;
    }
    
    // Approach 3: HashMap for Unicode support - O(n) time, O(k) space
    public boolean isAnagramHashMap(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }
        
        Map<Character, Integer> charCount = new HashMap<>();
        
        // Count characters in s
        for (char c : s.toCharArray()) {
            charCount.put(c, charCount.getOrDefault(c, 0) + 1);
        }
        
        // Decrease count for characters in t
        for (char c : t.toCharArray()) {
            if (!charCount.containsKey(c)) {
                return false;
            }
            charCount.put(c, charCount.get(c) - 1);
            if (charCount.get(c) == 0) {
                charCount.remove(c);
            }
        }
        
        return charCount.isEmpty();
    }
    
    // Test cases
    public static void main(String[] args) {
        ValidAnagram solution = new ValidAnagram();
        
        // Test case 1
        System.out.println(solution.isAnagram("anagram", "nagaram")); // true
        
        // Test case 2
        System.out.println(solution.isAnagram("rat", "car")); // false
        
        // Test case 3
        System.out.println(solution.isAnagram("listen", "silent")); // true
    }
}
```

---

### Problem 4: Contains Duplicate
**Difficulty**: Easy  
**Time**: 5-10 minutes  

**Problem Statement:**
Given an integer array `nums`, return `true` if any value appears at least twice in the array, and return `false` if every element is distinct.

**Solution:**
```java
public class ContainsDuplicate {
    
    // Approach 1: HashSet - O(n) time, O(n) space (optimal)
    public boolean containsDuplicate(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        
        for (int num : nums) {
            if (seen.contains(num)) {
                return true;
            }
            seen.add(num);
        }
        
        return false;
    }
    
    // Approach 2: Sorting - O(n log n) time, O(1) space
    public boolean containsDuplicateSorting(int[] nums) {
        Arrays.sort(nums);
        
        for (int i = 1; i < nums.length; i++) {
            if (nums[i] == nums[i - 1]) {
                return true;
            }
        }
        
        return false;
    }
    
    // Approach 3: Stream API (Java 8+) - O(n) time, O(n) space
    public boolean containsDuplicateStream(int[] nums) {
        return Arrays.stream(nums)
                    .boxed()
                    .collect(Collectors.toSet())
                    .size() != nums.length;
    }
    
    // Test cases
    public static void main(String[] args) {
        ContainsDuplicate solution = new ContainsDuplicate();
        
        // Test case 1
        int[] nums1 = {1, 2, 3, 1};
        System.out.println(solution.containsDuplicate(nums1)); // true
        
        // Test case 2
        int[] nums2 = {1, 2, 3, 4};
        System.out.println(solution.containsDuplicate(nums2)); // false
        
        // Test case 3
        int[] nums3 = {1, 1, 1, 3, 3, 4, 3, 2, 4, 2};
        System.out.println(solution.containsDuplicate(nums3)); // true
    }
}
```

---

## Linked List Problems

### Problem 5: Reverse Linked List
**Difficulty**: Easy  
**Time**: 15-20 minutes  

**Problem Statement:**
Given the head of a singly linked list, reverse the list, and return the reversed list.

**Solution:**
```java
public class ReverseLinkedList {
    
    // Definition for singly-linked list
    static class ListNode {
        int val;
        ListNode next;
        
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }
    
    // Approach 1: Iterative - O(n) time, O(1) space (optimal)
    public ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode current = head;
        
        while (current != null) {
            ListNode nextTemp = current.next; // Store next node
            current.next = prev;              // Reverse the link
            prev = current;                   // Move prev forward
            current = nextTemp;               // Move current forward
        }
        
        return prev; // prev is the new head
    }
    
    // Approach 2: Recursive - O(n) time, O(n) space
    public ListNode reverseListRecursive(ListNode head) {
        // Base case
        if (head == null || head.next == null) {
            return head;
        }
        
        // Recursively reverse the rest of the list
        ListNode reversedHead = reverseListRecursive(head.next);
        
        // Reverse the current connection
        head.next.next = head;
        head.next = null;
        
        return reversedHead;
    }
    
    // Helper method to create linked list from array
    public ListNode createLinkedList(int[] values) {
        if (values.length == 0) return null;
        
        ListNode head = new ListNode(values[0]);
        ListNode current = head;
        
        for (int i = 1; i < values.length; i++) {
            current.next = new ListNode(values[i]);
            current = current.next;
        }
        
        return head;
    }
    
    // Helper method to print linked list
    public void printList(ListNode head) {
        ListNode current = head;
        while (current != null) {
            System.out.print(current.val);
            if (current.next != null) {
                System.out.print(" -> ");
            }
            current = current.next;
        }
        System.out.println();
    }
    
    // Test cases
    public static void main(String[] args) {
        ReverseLinkedList solution = new ReverseLinkedList();
        
        // Test case 1: [1,2,3,4,5]
        int[] values1 = {1, 2, 3, 4, 5};
        ListNode head1 = solution.createLinkedList(values1);
        System.out.print("Original: ");
        solution.printList(head1);
        
        ListNode reversed1 = solution.reverseList(head1);
        System.out.print("Reversed: ");
        solution.printList(reversed1);
        
        // Test case 2: [1,2]
        int[] values2 = {1, 2};
        ListNode head2 = solution.createLinkedList(values2);
        ListNode reversed2 = solution.reverseListRecursive(head2);
        System.out.print("Recursive Reversed: ");
        solution.printList(reversed2);
    }
}
```

---

### Problem 6: Merge Two Sorted Lists
**Difficulty**: Easy  
**Time**: 15-20 minutes  

**Problem Statement:**
You are given the heads of two sorted linked lists `list1` and `list2`. Merge the two lists in a sorted manner and return the head of the merged linked list.

**Solution:**
```java
public class MergeTwoSortedLists {
    
    static class ListNode {
        int val;
        ListNode next;
        
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }
    
    // Approach 1: Iterative - O(m + n) time, O(1) space (optimal)
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // Create a dummy node to simplify edge cases
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;
        
        // Merge while both lists have nodes
        while (list1 != null && list2 != null) {
            if (list1.val <= list2.val) {
                current.next = list1;
                list1 = list1.next;
            } else {
                current.next = list2;
                list2 = list2.next;
            }
            current = current.next;
        }
        
        // Attach remaining nodes
        if (list1 != null) {
            current.next = list1;
        } else {
            current.next = list2;
        }
        
        return dummy.next; // Skip dummy node
    }
    
    // Approach 2: Recursive - O(m + n) time, O(m + n) space
    public ListNode mergeTwoListsRecursive(ListNode list1, ListNode list2) {
        // Base cases
        if (list1 == null) return list2;
        if (list2 == null) return list1;
        
        // Recursive case
        if (list1.val <= list2.val) {
            list1.next = mergeTwoListsRecursive(list1.next, list2);
            return list1;
        } else {
            list2.next = mergeTwoListsRecursive(list1, list2.next);
            return list2;
        }
    }
    
    // Helper methods (same as previous problem)
    public ListNode createLinkedList(int[] values) {
        if (values.length == 0) return null;
        
        ListNode head = new ListNode(values[0]);
        ListNode current = head;
        
        for (int i = 1; i < values.length; i++) {
            current.next = new ListNode(values[i]);
            current = current.next;
        }
        
        return head;
    }
    
    public void printList(ListNode head) {
        ListNode current = head;
        while (current != null) {
            System.out.print(current.val);
            if (current.next != null) {
                System.out.print(" -> ");
            }
            current = current.next;
        }
        System.out.println();
    }
    
    // Test cases
    public static void main(String[] args) {
        MergeTwoSortedLists solution = new MergeTwoSortedLists();
        
        // Test case 1: list1 = [1,2,4], list2 = [1,3,4]
        ListNode list1 = solution.createLinkedList(new int[]{1, 2, 4});
        ListNode list2 = solution.createLinkedList(new int[]{1, 3, 4});
        
        System.out.print("List 1: ");
        solution.printList(list1);
        System.out.print("List 2: ");
        solution.printList(list2);
        
        ListNode merged = solution.mergeTwoLists(list1, list2);
        System.out.print("Merged: ");
        solution.printList(merged); // Expected: 1 -> 1 -> 2 -> 3 -> 4 -> 4
    }
}
```

---

## Tree Problems

### Problem 7: Maximum Depth of Binary Tree
**Difficulty**: Easy  
**Time**: 10-15 minutes  

**Problem Statement:**
Given the root of a binary tree, return its maximum depth. A binary tree's maximum depth is the number of nodes along the longest path from the root node down to the farthest leaf node.

**Solution:**
```java
public class MaximumDepthBinaryTree {
    
    // Definition for a binary tree node
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
    
    // Approach 1: Recursive DFS - O(n) time, O(h) space where h is height
    public int maxDepth(TreeNode root) {
        if (root == null) {
            return 0;
        }
        
        int leftDepth = maxDepth(root.left);
        int rightDepth = maxDepth(root.right);
        
        return Math.max(leftDepth, rightDepth) + 1;
    }
    
    // Approach 2: Iterative BFS - O(n) time, O(w) space where w is max width
    public int maxDepthIterative(TreeNode root) {
        if (root == null) {
            return 0;
        }
        
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        int depth = 0;
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            depth++;
            
            // Process all nodes at current level
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
        }
        
        return depth;
    }
    
    // Approach 3: DFS with Stack - O(n) time, O(h) space
    public int maxDepthDFSStack(TreeNode root) {
        if (root == null) {
            return 0;
        }
        
        Stack<Pair<TreeNode, Integer>> stack = new Stack<>();
        stack.push(new Pair<>(root, 1));
        int maxDepth = 0;
        
        while (!stack.isEmpty()) {
            Pair<TreeNode, Integer> current = stack.pop();
            TreeNode node = current.getKey();
            int depth = current.getValue();
            
            maxDepth = Math.max(maxDepth, depth);
            
            if (node.left != null) {
                stack.push(new Pair<>(node.left, depth + 1));
            }
            if (node.right != null) {
                stack.push(new Pair<>(node.right, depth + 1));
            }
        }
        
        return maxDepth;
    }
    
    // Simple Pair class for stack approach
    static class Pair<K, V> {
        private K key;
        private V value;
        
        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
        
        public K getKey() { return key; }
        public V getValue() { return value; }
    }
    
    // Test cases
    public static void main(String[] args) {
        MaximumDepthBinaryTree solution = new MaximumDepthBinaryTree();
        
        // Test case 1: [3,9,20,null,null,15,7] - depth should be 3
        TreeNode root1 = new TreeNode(3);
        root1.left = new TreeNode(9);
        root1.right = new TreeNode(20);
        root1.right.left = new TreeNode(15);
        root1.right.right = new TreeNode(7);
        
        System.out.println("Max depth (recursive): " + solution.maxDepth(root1)); // 3
        System.out.println("Max depth (iterative): " + solution.maxDepthIterative(root1)); // 3
        
        // Test case 2: [1,null,2] - depth should be 2
        TreeNode root2 = new TreeNode(1);
        root2.right = new TreeNode(2);
        
        System.out.println("Max depth: " + solution.maxDepth(root2)); // 2
    }
}
```

---

## Tips for Easy Problems:

### 1. **Time Management**
- Spend 2-3 minutes understanding the problem
- Ask clarifying questions
- Start with brute force, then optimize
- Test with edge cases

### 2. **Common Patterns**
- **Two Pointers**: For sorted arrays, palindromes
- **Hash Map**: For lookups, counting
- **Sliding Window**: For substrings, subarrays
- **BFS/DFS**: For trees and graphs

### 3. **Edge Cases to Consider**
- Empty input (null, empty array/string)
- Single element
- All same elements
- Negative numbers
- Integer overflow

### 4. **Communication Tips**
- Explain your approach before coding
- Think out loud while implementing
- Mention time and space complexity
- Test with examples

### 5. **Code Quality**
- Use meaningful variable names
- Handle edge cases
- Write clean, readable code
- Add comments for complex logic

These easy problems form the foundation for more complex challenges. Master these patterns and you'll be well-prepared for medium and hard problems!
