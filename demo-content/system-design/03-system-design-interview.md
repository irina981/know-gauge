---
title: System Design Interview Walkthrough
pdf: System Design - Interview Walkthrough.pdf
brand: System Design
docid: SD-03
version: 1.0
effective: November 2026
appliesTo: Software engineers preparing for system design interviews
disclaimer: Sample learning material created for the KnowGauge demo. Numbers are rounded estimates for practice.
---

# System Design Interview Walkthrough

A system design interview is an open conversation about building a large system. This document covers a step-by-step
approach, back-of-the-envelope estimates, and two classic case studies: a URL shortener and a news feed.

## 1. A step-by-step approach

### 1.1 Clarify the requirements

- Ask about **functional requirements**: what must the system do? For example, "users can shorten a URL and open it".
- Ask about **non-functional requirements**: scale, latency, availability and consistency needs.
- Agree on what is **out of scope**, so the design stays focused.

### 1.2 Estimate, then design the API and data model

- Estimate the **traffic** and **storage** (see section 2), because they decide how much the system must scale.
- Define the main **API** endpoints, for example `POST /urls` and `GET /{code}`.
- Sketch the **data model**: the main entities, their keys and how they are queried.

### 1.3 High-level design and deep dives

- Draw the **high-level design**: clients, load balancer, services, caches, databases and queues.
- Then do **deep dives** on the hardest parts, such as scaling the database or handling peaks.
- Discuss **trade-offs** and bottlenecks openly; there is rarely one right answer.

## 2. Back-of-the-envelope estimates

### 2.1 From users to requests per second

- A day has about **86,400 seconds**; for quick estimates use **100,000** (10⁵).
- **10 million** daily active users making **10 requests** each give 10⁸ requests per day, or about **1,000
  requests per second** on average.
- Plan for **peak** traffic of two to ten times the average.

### 2.2 Storage

- Multiply the number of new records per day by the size of one record and by the retention period.
- For example, **1 million** new records per day of **500 bytes** each is **500 MB per day**, or about **180 GB per
  year**.
- Many systems are **read-heavy**: a read-to-write ratio of **100:1** is common for content sites.

### 2.3 Latency numbers to remember

- Reading from **memory**: about **100 nanoseconds**.
- A round trip **within a data centre**: about **0.5 milliseconds**.
- A random read from an **SSD**: about **0.1 milliseconds**; from a spinning **disk**: about **10 milliseconds**.
- A round trip **between continents**: about **150 milliseconds**.

## 3. Case study: a URL shortener

### 3.1 Requirements and API

- Users submit a long URL and get a short one, such as `kg.example/aZ3kP9q`; opening the short URL redirects to the
  long one.
- `POST /urls` creates a short code; `GET /{code}` answers with an HTTP **redirect**: **301** (permanent, browsers
  cache it) or **302** (temporary, every visit reaches the server, which allows counting clicks).

### 3.2 Generating short codes

- A code of **7 characters** in **base62** (a–z, A–Z, 0–9) gives 62⁷, about **3.5 trillion** combinations.
- One approach encodes a **unique counter** (for example from a database sequence or a range of IDs per server) in
  base62, which never collides.
- Another hashes the long URL and takes the first characters, but then **collisions** must be detected and resolved.

### 3.3 Scaling reads

- The system is **read-heavy**: far more redirects than new URLs.
- Put a **cache** in front of the database for popular codes, and serve redirects from many stateless servers behind
  a load balancer.
- A **key-value store** fits the data well: the code is the key, the long URL is the value.

## 4. Case study: a news feed

### 4.1 Requirements

- Users follow other users and see a **feed** of recent posts from the people they follow, newest first.
- The feed must load **fast**; a few seconds of delay before a new post appears is acceptable (eventual
  consistency).

### 4.2 Fan-out on write and fan-out on read

- **Fan-out on write** (push): when a user posts, the post ID is added to the precomputed feed of every follower.
  Reading a feed is then very fast, but writing is expensive for users with millions of followers.
- **Fan-out on read** (pull): the feed is built when it is opened, by fetching recent posts of everyone the user
  follows. Writing is cheap, reading is slow.
- A **hybrid** uses push for most users and pull for **celebrities** with very many followers.

### 4.3 Storage and caching

- Store posts in a database sharded by **post ID** or **user ID**, and keep each user's precomputed feed as a list of
  post IDs in a cache such as Redis.
- Load the feed in pages with **cursor-based pagination** (for example "posts older than this ID") rather than page
  numbers, so new posts do not shift the pages.
