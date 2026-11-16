---
title: Searching, Sorting and Heaps
pdf: Algorithms - Searching Sorting and Heaps.pdf
brand: Algorithms for Coding Interviews
docid: ALG-02
version: 1.0
effective: November 2026
appliesTo: Software engineers preparing for coding interviews
disclaimer: Sample learning material created for the KnowGauge demo. Complexities assume n is the input length.
---

# Searching, Sorting and Heaps

This document covers three families of algorithms that appear in many interviews: binary search, sorting
algorithms, and heaps for top-k problems. For each one it gives the idea, when to use it, its complexity, and
sample tasks.

## 1. Binary search

### 1.1 Idea

**Binary search** looks for a value in a **sorted** array by comparing it with the middle element and discarding the
half that cannot contain it. Every step halves the search range.

- Compute the middle as `low + (high - low) / 2` to avoid integer overflow.
- **Binary search on the answer**: when a yes/no condition is monotonic (false, false, …, true, true), binary search
  finds the first value for which it is true.

### 1.2 When to use it

- The data is **sorted**, or the answer lies in a range where a check is **monotonic**.
- Finding the **first or last** position of a value, or the insertion point.
- A **rotated sorted array**: one half is always sorted, which tells you where to continue.

### 1.3 Complexity

- Time **O(log n)**: searching one million elements takes about **20** comparisons.
- Space **O(1)** for the iterative version, **O(log n)** for the recursive version because of the call stack.

### 1.4 Sample tasks

- **Search insert position** in a sorted array.
- **Search in a rotated sorted array**.
- **Find the minimum in a rotated sorted array**.
- **Koko eating bananas**: binary search on the eating speed.

## 2. Sorting algorithms

### 2.1 Idea

- **Merge sort** splits the array in half, sorts both halves recursively and **merges** them.
- **Quicksort** picks a **pivot**, partitions the elements into smaller and larger ones, and sorts both parts.
- **Counting sort** counts how often each value occurs; it works only for small integer ranges.
- A sort is **stable** when equal elements keep their original order. Merge sort is stable; the usual quicksort is
  not.

### 2.2 When to use it

- **Sort first** to make a problem easier: merging intervals, finding duplicates, grouping anagrams.
- Use **merge sort** when stability matters or for linked lists; it is also the basis for counting **inversions**.
- Use **counting sort** or **bucket sort** when values come from a small known range.
- In practice, use the language's built-in sort (Java uses a dual-pivot quicksort for primitives and **TimSort**, a
  stable merge-based sort, for objects).

### 2.3 Complexity

- Merge sort: time **O(n log n)** in every case, extra space **O(n)**.
- Quicksort: average **O(n log n)**, worst case **O(n²)** with bad pivots; space **O(log n)** on average.
- Counting sort: **O(n + k)**, where k is the range of values.
- No comparison-based sort can be faster than **O(n log n)** in the worst case.

### 2.4 Sample tasks

- **Merge intervals**: sort by start, then merge overlapping intervals.
- **Sort colors** (the Dutch national flag problem) in one pass.
- **Group anagrams**: sort each word's letters to build a key.
- **Meeting rooms**: can a person attend all meetings? Sort by start time and check overlaps.

## 3. Heaps and top-k problems

### 3.1 Idea

A **heap** is a complete binary tree where every parent is smaller (**min-heap**) or larger (**max-heap**) than its
children. The smallest or largest element is always at the top. In Java, `PriorityQueue` is a min-heap by default.

### 3.2 When to use it

- **Top k** largest or most frequent elements: keep a **min-heap of size k** and remove the top when it grows
  larger than k.
- **Merging k sorted lists**: the heap always holds the current head of each list.
- A **running median**: one max-heap for the lower half and one min-heap for the upper half.

### 3.3 Complexity

- Insert and remove the top: **O(log n)**; reading the top: **O(1)**.
- Building a heap from n elements: **O(n)**.
- Top k with a heap of size k: **O(n log k)**, which beats sorting everything when k is small.

### 3.4 Sample tasks

- **Kth largest element in an array**.
- **Top k frequent elements**: count with a hash map, then keep a heap of size k.
- **Merge k sorted lists**.
- **Find median from a data stream** with two heaps.
