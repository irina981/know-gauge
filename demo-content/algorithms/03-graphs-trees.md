---
title: Graphs and Trees
pdf: Algorithms - Graphs and Trees.pdf
brand: Algorithms for Coding Interviews
docid: ALG-03
version: 1.0
effective: November 2026
appliesTo: Software engineers preparing for coding interviews
disclaimer: Sample learning material created for the KnowGauge demo. V is the number of vertices and E the number of edges.
---

# Graphs and Trees

Trees and graphs are behind many interview problems: grids, dependencies, networks and hierarchies. This document
covers breadth-first and depth-first search, topological sort, and shortest paths with Dijkstra's algorithm. For
each one it gives the idea, when to use it, its complexity, and sample tasks.

## 1. Breadth-first and depth-first search

### 1.1 Idea

- **Breadth-first search (BFS)** visits nodes **level by level** using a **queue**: first all neighbours of the
  start, then their neighbours, and so on.
- **Depth-first search (DFS)** goes as **deep** as possible along one path before backtracking, using **recursion**
  or an explicit **stack**.
- In a graph, both keep a **visited** set so that no node is processed twice. A tree needs no visited set because it
  has no cycles.
- Tree traversals are forms of DFS: **pre-order** (node, left, right), **in-order** (left, node, right) and
  **post-order** (left, right, node). An in-order traversal of a binary search tree visits the values in sorted
  order. A **level-order** traversal is BFS.

### 1.2 When to use it

- **BFS** finds the **shortest path in an unweighted graph** or grid, because it reaches nodes in order of distance.
- **DFS** is natural for exploring everything reachable: **connected components**, flood fill, cycle detection and
  path enumeration.
- A grid is a graph whose nodes are cells and whose edges connect up, down, left and right neighbours.

### 1.3 Complexity

- Time **O(V + E)** for both, with an adjacency list; **O(rows × columns)** on a grid.
- Space **O(V)** for the visited set and the queue or stack. Recursive DFS on a very deep graph can overflow the
  call stack.

### 1.4 Sample tasks

- **Number of islands**: count connected groups of land cells with DFS or BFS.
- **Binary tree level order traversal** with a queue.
- **Shortest path in a binary matrix** with BFS.
- **Clone graph**: copy every node with DFS and a map from old to new nodes.

## 2. Topological sort

### 2.1 Idea

A **topological order** of a **directed acyclic graph (DAG)** lists the nodes so that every edge goes from an earlier
node to a later one, for example every course after its prerequisites.

- **Kahn's algorithm** repeatedly takes nodes with **in-degree 0** (no remaining prerequisites) from a queue and
  lowers the in-degree of their neighbours.
- A DFS-based version adds each node to the result after all its descendants, then reverses the list.

### 2.2 When to use it

- Tasks with **dependencies**: build order, course scheduling, spreadsheet cell evaluation.
- **Detecting a cycle** in a directed graph: if Kahn's algorithm cannot process every node, the graph contains a
  cycle and no valid order exists.

### 2.3 Complexity

- Time **O(V + E)**: every node and edge is processed once.
- Space **O(V)** for the in-degree counts and the queue.

### 2.4 Sample tasks

- **Course schedule**: can all courses be finished, given the prerequisite pairs?
- **Course schedule II**: return one valid order of the courses.
- **Alien dictionary**: derive the letter order from a sorted list of words.

## 3. Shortest paths with Dijkstra's algorithm

### 3.1 Idea

**Dijkstra's algorithm** finds the shortest distance from one start node to all others in a graph with
**non-negative edge weights**. It always expands the closest unvisited node next, using a **min-heap** (priority
queue) of tentative distances, and **relaxes** the edges of that node: if going through it is shorter, the
neighbour's distance is updated.

### 3.2 When to use it

- **Weighted** graphs where all weights are **zero or positive**: road networks, network delays, costs.
- With **negative weights** Dijkstra can give wrong results; use **Bellman-Ford** instead, which also detects
  negative cycles.
- With **all weights equal**, plain BFS is enough and simpler.

### 3.3 Complexity

- Time **O((V + E) log V)** with a binary heap.
- Space **O(V)** for the distances and the heap.
- Bellman-Ford takes **O(V × E)**.

### 3.4 Sample tasks

- **Network delay time**: how long until a signal from one node reaches all nodes?
- **Path with minimum effort** in a grid of heights.
- **Cheapest flights within k stops**: a variant where the number of edges is limited.
