---
title: Dynamic Programming, Backtracking and Greedy
pdf: Algorithms - Dynamic Programming Backtracking and Greedy.pdf
brand: Algorithms for Coding Interviews
docid: ALG-04
version: 1.0
effective: November 2026
appliesTo: Software engineers preparing for coding interviews
disclaimer: Sample learning material created for the KnowGauge demo. Complexities assume n is the input length.
---

# Dynamic Programming, Backtracking and Greedy

Optimisation and search problems are usually solved with dynamic programming, backtracking or a greedy strategy.
For each of the three this document gives the idea, when to use it, its complexity, and sample tasks.

## 1. Dynamic programming

### 1.1 Idea

**Dynamic programming (DP)** solves a problem by combining the answers to smaller **overlapping subproblems**, and
stores each answer so that it is computed only once.

- **Memoization (top-down)**: write the recursive solution and cache the results, for example in a map.
- **Tabulation (bottom-up)**: fill a table from the smallest subproblems upwards, without recursion.
- The key step is defining the **state** (what `dp[i]` means) and the **transition** (how `dp[i]` follows from
  smaller states).

### 1.2 When to use it

- The problem asks for the **number of ways**, the **minimum or maximum** value, or whether something is
  **possible**.
- A naive recursive solution recomputes the **same subproblems** many times.
- The problem has **optimal substructure**: the best answer is built from the best answers to subproblems.

### 1.3 Complexity

- Time is usually **number of states × work per state**: for example **O(n)** for climbing stairs and **O(n × m)**
  for the longest common subsequence of two strings of lengths n and m.
- Space equals the table size, but often only the previous row or the last two values are needed, which reduces a
  one-dimensional DP to **O(1)** space.
- Naive recursive Fibonacci takes **O(2ⁿ)** time; with memoization it takes **O(n)**.

### 1.4 Sample tasks

- **Climbing stairs**: the number of ways to reach step n taking 1 or 2 steps; `dp[i] = dp[i-1] + dp[i-2]`.
- **Coin change**: the fewest coins that make up an amount.
- **Longest common subsequence** of two strings.
- **House robber**: the maximum sum of non-adjacent houses.

## 2. Backtracking

### 2.1 Idea

**Backtracking** builds a solution step by step, and as soon as a partial solution cannot lead to a valid answer it
**undoes the last choice** and tries the next one. It is a depth-first search over the tree of choices.

- The usual shape: *choose* an option, *explore* recursively, *un-choose* it.
- **Pruning** skips branches that cannot succeed, which makes the search much faster in practice.

### 2.2 When to use it

- The problem asks for **all** solutions: all subsets, permutations or combinations.
- **Constraint** puzzles: placing queens, solving a Sudoku, searching for a word in a grid.
- The input is small, because the number of possibilities grows **exponentially**.

### 2.3 Complexity

- **Subsets** of n elements: **O(2ⁿ)** results; **permutations**: **O(n!)** results.
- Space **O(n)** for the recursion depth, plus the space of the results.

### 2.4 Sample tasks

- **Subsets**: generate the power set of distinct numbers.
- **Permutations** of distinct numbers.
- **N-Queens**: place n queens on an n×n board so that none attack each other.
- **Word search**: find whether a word exists in a grid of letters.

## 3. Greedy algorithms

### 3.1 Idea

A **greedy** algorithm makes the **locally best choice** at each step and never reconsiders it. It is simple and
fast, but it gives the correct answer only for problems where local choices lead to the global optimum.

### 3.2 When to use it

- **Interval scheduling**: to fit the most non-overlapping intervals, always pick the interval that **ends
  earliest**.
- The choice can be justified by an **exchange argument**: swapping any optimal solution's choice for the greedy one
  does not make it worse.
- Greedy **fails** for coin change with arbitrary coin values: with coins 1, 3 and 4 and amount 6, greedy takes
  4 + 1 + 1 (three coins), but 3 + 3 (two coins) is better. Use dynamic programming there.

### 3.3 Complexity

- Often **O(n log n)**, dominated by sorting; **O(n)** when the input is already ordered.
- Space usually **O(1)** extra.

### 3.4 Sample tasks

- **Non-overlapping intervals**: remove the fewest intervals so that the rest do not overlap.
- **Jump game**: can you reach the last index? Track the furthest reachable position.
- **Gas station**: find the start from which a car can drive around the circuit.
- **Assign cookies** to children to satisfy as many as possible.
