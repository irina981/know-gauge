---
title: Array and String Patterns
pdf: Algorithms - Array and String Patterns.pdf
brand: Algorithms for Coding Interviews
docid: ALG-01
version: 1.0
effective: November 2026
appliesTo: Software engineers preparing for coding interviews
disclaimer: Sample learning material created for the KnowGauge demo. Complexities assume n is the input length.
---

# Array and String Patterns

Many interview problems on arrays and strings are solved with one of three patterns: the sliding window, two
pointers, and prefix sums with hashing. For each pattern this document gives the idea, when to use it, its
complexity, and sample tasks.

## 1. Sliding window

### 1.1 Idea

A **sliding window** is a contiguous range `[left, right]` that moves over the array or string. The right end grows
the window; the left end shrinks it when a condition is broken. Each element enters and leaves the window at most
once, so the work is linear instead of checking every subarray.

- In a **fixed-size** window, both ends move together by one step.
- In a **variable-size** window, the right end moves every step and the left end moves only until the window is
  valid again.

### 1.2 When to use it

- The problem asks about a **contiguous** subarray or substring.
- Words such as *longest*, *shortest*, *maximum sum of k consecutive* or *at most k distinct* appear.
- The window state (a sum, a count, a frequency map) can be updated in **constant time** when one element enters or
  leaves.

### 1.3 Complexity

- Time **O(n)**: both pointers only move forward.
- Space **O(1)** for a running sum, or **O(k)** for a frequency map of the characters in the window.
- The brute-force alternative, checking all subarrays, is **O(n²)**.

### 1.4 Sample tasks

- **Maximum sum of a subarray of size k**: fixed window, add the new element, subtract the one that leaves.
- **Longest substring without repeating characters**: variable window with a set; shrink from the left while the
  new character is already in the window.
- **Minimum window substring**: the smallest substring of `s` that contains all characters of `t`, using two
  frequency maps.
- **Longest substring with at most k distinct characters**.

## 2. Two pointers

### 2.1 Idea

Two indexes move through the data, usually **from both ends towards each other** or **both forward at different
speeds**. Each step discards part of the search space, so one pass is enough.

- **Opposite ends**: on a **sorted** array, if the sum of the two values is too small move the left pointer right;
  if it is too large move the right pointer left.
- **Fast and slow**: a fast pointer moves two steps for every step of the slow pointer.

### 2.2 When to use it

- The input is **sorted**, or can be sorted, and you look for pairs or triplets.
- You must **remove duplicates** or rearrange an array **in place**.
- On a **linked list**, to find the middle node or detect a cycle (**Floyd's cycle detection**).

### 2.3 Complexity

- Time **O(n)** for one pass; **O(n log n)** if the array must be sorted first.
- Space **O(1)**, because only two indexes are stored.
- **3Sum** with two pointers inside a loop is **O(n²)**.

### 2.4 Sample tasks

- **Two Sum II** on a sorted array: return the two indexes whose values add up to a target.
- **Container with most water**: move the pointer at the shorter line inwards.
- **Valid palindrome**: compare characters from both ends.
- **Linked list cycle**: if the fast and slow pointers meet, there is a cycle.

## 3. Prefix sums and hashing

### 3.1 Idea

A **prefix sum** array stores `prefix[i] = a[0] + … + a[i-1]`, so the sum of any range `[i, j)` is
`prefix[j] - prefix[i]` in constant time. Combined with a **hash map** of prefix sums seen so far, it finds
subarrays with a given sum in one pass.

### 3.2 When to use it

- Many **range sum** queries on an array that does not change.
- Counting subarrays whose sum equals **k**, including arrays with **negative numbers**, where a sliding window does
  not work.
- **Two Sum** on an unsorted array: store each value's index in a hash map and look up `target - value`.

### 3.3 Complexity

- Building the prefix array: time **O(n)**, space **O(n)**.
- Each range sum query: **O(1)**.
- Subarray sum equals k with a hash map: time **O(n)**, space **O(n)**.

### 3.4 Sample tasks

- **Range sum query**: answer many sum queries on a fixed array.
- **Subarray sum equals k**: count how often `prefix - k` has been seen.
- **Two Sum** on an unsorted array in one pass with a hash map.
- **Product of array except self**: prefix and suffix products, without division.
