---
title: Advanced System Design Topics
pdf: System Design - Advanced Topics.pdf
brand: System Design
docid: SD-02
version: 1.0
effective: November 2026
appliesTo: Software engineers learning to design scalable systems
disclaimer: Sample learning material created for the KnowGauge demo. It gives general information on well-known concepts.
---

# Advanced System Design Topics

This document goes beyond the basics: replication and sharding, consistency and the CAP theorem, asynchronous
messaging, and patterns that keep distributed systems reliable.

## 1. Replication and sharding

### 1.1 Replication

- **Replication** keeps copies of the same data on several nodes, for availability and to spread **read** load.
- In **leader-follower** (primary-replica) replication, all writes go to the leader, which sends them to the
  followers; reads can go to followers.
- **Synchronous** replication waits for followers before confirming a write (safer, slower); **asynchronous**
  replication does not wait, so followers can lag behind (**replication lag**) and a failover can lose recent writes.

### 1.2 Sharding

- **Sharding** (horizontal partitioning) splits the data across nodes, each holding a part, so that **writes and
  storage** scale too.
- A **shard key** decides where each row goes, for example the user ID. A bad key creates **hot spots**: one shard
  gets most of the traffic.
- **Range-based** sharding keeps ranges of keys together (good for range queries, risk of hot spots); **hash-based**
  sharding spreads keys evenly.
- Queries that need data from many shards (**scatter-gather**) and cross-shard transactions are expensive.

### 1.3 Consistent hashing

- With plain `hash(key) mod N`, adding or removing a node moves almost all keys.
- **Consistent hashing** places nodes and keys on a **hash ring**; each key belongs to the next node clockwise.
  Adding or removing a node moves only about **1/N** of the keys.
- **Virtual nodes** (several positions per physical node) spread the load more evenly. Consistent hashing is used in
  systems such as Cassandra and DynamoDB.

## 2. Consistency and the CAP theorem

### 2.1 The CAP theorem

- The **CAP theorem** says that when a **network partition** happens, a distributed system must choose between
  **consistency** (every read sees the latest write) and **availability** (every request gets an answer).
- Partitions cannot be avoided in practice, so the real choice is **CP** (refuse some requests to stay consistent)
  or **AP** (keep answering, possibly with stale data).
- **PACELC** extends CAP: even without a partition there is a trade-off between **latency** and **consistency**.

### 2.2 Consistency models

- **Strong consistency**: after a write completes, every read returns it.
- **Eventual consistency**: replicas converge to the same value if no new writes arrive; reads may be stale for a
  while. Acceptable for likes, view counts or a news feed; not for a bank balance.
- **Read-your-writes** consistency: a user always sees their own changes, even if others see them later.

### 2.3 Quorums

- With **N** replicas, a write must be confirmed by **W** of them and a read must ask **R** of them.
- If **R + W > N**, every read overlaps with the latest write on at least one replica. A common choice is N = 3,
  W = 2, R = 2.

## 3. Asynchronous messaging

### 3.1 Message queues and pub/sub

- A **message queue** (such as RabbitMQ or Amazon SQS) lets a producer hand work to consumers without waiting for it
  to finish; it smooths out traffic peaks and decouples services.
- In **publish/subscribe**, a message published to a **topic** is delivered to every subscriber. **Apache Kafka**
  stores messages in a durable, partitioned **log** that consumers read at their own pace.

### 3.2 Delivery guarantees

- **At most once**: a message may be lost but is never delivered twice.
- **At least once**: a message is never lost but may be delivered **more than once**; this is the usual default.
- **Exactly once** is hard in practice; the common solution is at-least-once delivery with **idempotent**
  consumers, where processing the same message twice has the same effect as once.

### 3.3 The outbox pattern

- Writing to the database and publishing a message are two separate actions; if one fails, they disagree.
- With the **transactional outbox**, the service writes the event to an **outbox table** in the same database
  transaction as the data change; a separate process publishes the outbox rows.

## 4. Reliability patterns

### 4.1 Rate limiting

- **Rate limiting** caps how many requests a client may send, protecting the system from overload and abuse.
- The **token bucket** algorithm adds tokens at a fixed rate up to a maximum; each request takes a token, so short
  **bursts** are allowed. A **fixed window** counter is simpler but allows bursts at window edges; a **sliding
  window** avoids that.
- Rejected requests get HTTP status **429 Too Many Requests**.

### 4.2 Retries and timeouts

- Every remote call needs a **timeout**; without one, a slow dependency blocks threads indefinitely.
- Retry only **transient** errors, with **exponential backoff** (wait 1, 2, 4, 8 … seconds) and random **jitter** so
  that clients do not retry at the same moment.
- Only retry operations that are **idempotent**, or use an **idempotency key**.

### 4.3 Circuit breakers

- A **circuit breaker** stops calling a dependency that keeps failing. In the **open** state calls fail immediately;
  after a wait it goes **half-open** and lets a few test calls through; if they succeed it **closes** again.
- This prevents **cascading failures**, where one slow service exhausts the threads of all services that call it.
