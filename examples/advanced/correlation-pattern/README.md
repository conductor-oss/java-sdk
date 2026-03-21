# Message Correlation in Java Using Conductor : Group Related Messages by ID and Process Together

## Linking Messages That Belong Together

A single customer order generates events from the payment service, the inventory service, and the shipping service. Each event arrives independently with its own schema, but they all share an order ID. To build a complete order view. payment confirmed, items reserved, label printed, you need to match these messages by their correlation field, group them, and process each group as a whole.

Without orchestration, you'd build a stateful correlator that buffers incoming messages, maintains a lookup table keyed by the correlation field, handles late-arriving messages, and decides when a group is complete enough to process. That's a lot of in-memory state management, timeout logic, and concurrency control for what's fundamentally a match-and-group operation.

## The Solution

**You write the matching and aggregation logic. Conductor handles sequencing, retries, and correlation tracking.**

`CrpReceiveMessagesWorker` ingests the batch of messages and counts them. `CrpMatchByIdWorker` groups the messages by the specified correlation field (e.g., `orderId`), producing correlated groups where each group contains all messages sharing the same ID. `CrpAggregateWorker` combines each group into a single aggregated result. merging payment status with inventory status with shipping status. `CrpProcessWorker` acts on the aggregated groups, producing a final outcome per correlated set. Conductor tracks how many messages were received, how many groups were formed, and how many were successfully processed.

### What You Write: Workers

Four workers handle the match-and-group flow: message ingestion, correlation-ID matching, per-group aggregation, and group processing, each isolated from the others' data schemas.### The Workflow

```
crp_receive_messages
 │
 ▼
crp_match_by_id
 │
 ▼
crp_aggregate
 │
 ▼
crp_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
