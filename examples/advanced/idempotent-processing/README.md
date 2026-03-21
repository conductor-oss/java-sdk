# Idempotent Message Processing in Java Using Conductor: Check, Process-or-Skip, Record

Kafka delivers a payment event. Your service processes it, charges the customer $49.99, and then: network blip, the acknowledgment never reaches the broker. Kafka redelivers. Your service processes it again. The customer is now out $99.98 and your support queue has a new ticket. At-least-once delivery guarantees duplicates will arrive; the only question is whether your system charges twice or catches the replay. This example builds an idempotent processing pipeline with Conductor: check a dedup store before doing any work, route new messages to processing and duplicates to a skip path via a `SWITCH` task, and record every message ID so future replays are caught. The example submits the same message twice to prove both paths. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Duplicates Are Inevitable, Double-Processing Is Not

Message queues guarantee at-least-once delivery, which means your consumer will see the same message more than once. After a network timeout, a consumer restart, or a broker failover. If your processing logic charges a credit card, sends an email, or updates an inventory count, processing the same message twice produces incorrect results.

Idempotent processing solves this by checking a deduplication store before doing any work. If the message ID has been seen before, the workflow returns the previous result without re-executing the business logic. If it's new, the workflow processes it normally. Either way, the final state is recorded so future duplicates are caught. The `SWITCH` task makes this branch explicit. Unprocessed messages go to `idp_process`, already-processed messages go to `idp_skip`.

## The Solution

**You write the dedup check and processing logic. Conductor handles the process-or-skip routing, retries, and execution history.**

`CheckProcessedWorker` looks up the message ID in the deduplication store and returns the processing state. `unprocessed` if the message is new, `processed` if it's been seen before (along with the previous result hash). A `SWITCH` task routes accordingly: new messages go to `ProcessWorker` for execution, duplicates go to `SkipWorker` which returns the cached result. `RecordWorker` persists the message ID in the dedup store so future duplicates are caught. Conductor makes the dedup-then-route pattern declarative, and every execution records whether the message was processed or skipped.

### What You Write: Workers

Four workers implement the dedup-or-process pattern. Duplicate detection, business logic execution for new messages, skip-with-cached-result for duplicates, and dedup store recording.

| Worker | Task | What It Does |
|---|---|---|
| **CheckProcessedWorker** | `idp_check_processed` | Looks up the messageId in the in-memory `DedupStore`. Returns `processingState=unprocessed` for new messages, `processingState=processed` (with previous result hash) for duplicates. |
| **ProcessWorker** | `idp_process` | Executes business logic for new messages. Produces a deterministic SHA-256 hash of the messageId as the result. Same input always produces the same output. |
| **RecordWorker** | `idp_record` | Writes the messageId and resultHash to the `DedupStore` so future runs with the same messageId are detected as duplicates. |
| **SkipWorker** | `idp_skip` | Returns the skip reason and the previous result hash for messages that have already been processed. No business logic is re-executed. |

The `DedupStore` is a `ConcurrentHashMap` shared across workers within the same JVM. To go to production, swap it for Redis, DynamoDB, or a database, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
idp_check_processed
 │
 ▼
SWITCH (idp_switch_ref)
 ├── unprocessed: idp_process
 ├── processed: idp_skip
 └── default: idp_process
 │
 ▼
idp_record

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
