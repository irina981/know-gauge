---
title: System Design Basics
pdf: System Design - Basics.pdf
brand: System Design
docid: SD-01
version: 1.0
effective: November 2026
appliesTo: Software engineers learning to design scalable systems
disclaimer: Sample learning material created for the KnowGauge demo. It gives general information on well-known concepts.
---

# System Design Basics

This document covers the building blocks of most web systems: scalability, load balancing, caching, and choosing
a database.

## 1. Scalability

### 1.1 Vertical and horizontal scaling

- **Vertical scaling** (scaling up) gives one server more CPU, memory or disk. It is simple but has a hard limit,
  and the server remains a **single point of failure**.
- **Horizontal scaling** (scaling out) adds more servers and spreads the load between them. It has no hard limit and
  survives the loss of a server, but it needs a load balancer and more coordination.

### 1.2 Stateless services

- A **stateless** service keeps no user data in memory between requests; any server can handle any request.
- Session data is moved to a shared store such as **Redis** or a database, or into a signed token sent by the client.
- Stateless services are easy to scale horizontally: new instances can be added or removed at any time.

### 1.3 Measuring load and performance

- **Throughput** is the number of requests handled per second (**QPS** or RPS).
- **Latency** is the time to answer one request. Report **percentiles** such as **p95** and **p99**, not only the
  average: p99 is the latency that 99% of requests stay under.
- **Availability** is often given in "nines": **99.9%** allows about **8.8 hours** of downtime per year, **99.99%**
  about **53 minutes**.

## 2. Load balancing

### 2.1 What a load balancer does

- A **load balancer** receives client requests and distributes them across a pool of servers.
- It runs **health checks** and stops sending traffic to servers that fail them.
- It can **terminate TLS**, so the servers behind it receive plain HTTP.

### 2.2 Algorithms

- **Round robin**: each server in turn; simple, but ignores how busy servers are.
- **Least connections**: the server with the fewest open connections; better when requests take different times.
- **IP hash**: the client's address decides the server, which keeps a client on the same server (**sticky
  sessions**).

### 2.3 Layer 4 and layer 7

- A **layer 4** load balancer works on TCP/UDP connections and does not look into the request; it is very fast.
- A **layer 7** load balancer understands HTTP and can route by **path, host or header**, for example `/api` to one
  service and `/images` to another.

## 3. Caching

### 3.1 Why and where to cache

- A **cache** stores the results of expensive operations in fast memory, so that repeated requests are answered
  without the database. Reading from memory takes microseconds; a database query often takes milliseconds.
- Caches exist at many levels: the **browser**, a **CDN** (content delivery network) for static files close to
  users, an in-memory cache such as **Redis** or **Memcached**, and the database's own buffer.

### 3.2 Caching strategies

- **Cache-aside** (lazy loading): the application reads the cache first; on a **miss** it reads the database and
  stores the result in the cache. This is the most common strategy.
- **Write-through**: every write goes to the cache and the database at the same time, so the cache is never stale,
  at the cost of slower writes.
- **Write-back** (write-behind): writes go to the cache and are saved to the database later; fast, but data can be
  lost if the cache fails.

### 3.3 Eviction and invalidation

- When the cache is full, an **eviction policy** removes entries: **LRU** (least recently used) is the most common,
  **LFU** (least frequently used) is another.
- A **TTL** (time to live) expires entries automatically after a set time.
- **Cache invalidation** – removing or updating entries when the data changes – is one of the hardest parts of
  caching.

## 4. Choosing a database

### 4.1 SQL databases

- **Relational (SQL)** databases such as **PostgreSQL** and **MySQL** store data in tables with a fixed schema and
  support **joins** and **transactions**.
- Transactions follow **ACID**: **Atomicity** (all or nothing), **Consistency** (rules are kept), **Isolation**
  (concurrent transactions do not interfere) and **Durability** (committed data survives a crash).

### 4.2 NoSQL databases

- **Key-value** stores (Redis, DynamoDB): very fast lookups by key.
- **Document** stores (MongoDB): flexible JSON-like documents.
- **Wide-column** stores (Cassandra): very high write volumes spread over many nodes.
- **Graph** databases (Neo4j): data that is mostly relationships.

### 4.3 Indexes

- An **index** is an extra data structure, usually a **B-tree**, that lets the database find rows without scanning
  the whole table.
- Indexes make **reads faster** but **writes slower** and use extra storage, so index the columns used in frequent
  queries.
