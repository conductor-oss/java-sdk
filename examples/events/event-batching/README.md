# Event Batching in Java Using Conductor

Event Batching. collects events, creates batches, then processes each batch in a DO_WHILE loop. ## The Problem

You need to batch high-volume events into manageable chunks before processing. When events arrive continuously, processing them one by one is inefficient. database inserts, API calls, and network round trips are much cheaper in batches. The workflow must collect incoming events, split them into fixed-size batches, and process each batch in a loop until all events are handled.

Without orchestration, you'd build a buffering service with manual batch-size management, a processing loop with error handling per batch, and recovery logic for partially processed batches. hoping the buffer does not overflow and that a failed batch does not block all subsequent batches.

## The Solution

**You just write the event-collection, batch-creation, and batch-processing workers. Conductor handles DO_WHILE batch iteration, per-batch retry on failure, and durable progress tracking across all batches.**

Each batching concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of collecting events, creating batches, processing each batch in a DO_WHILE loop, retrying failed batches without blocking subsequent ones, and tracking every batch's processing status. ### What You Write: Workers

Three workers handle batch processing: CollectEventsWorker gathers incoming events, CreateBatchesWorker splits them into fixed-size chunks, and ProcessBatchWorker processes each chunk in a DO_WHILE loop.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEventsWorker** | `eb_collect_events` | Collects incoming events and returns them along with a total count. |
| **CreateBatchesWorker** | `eb_create_batches` | Creates batches of events from the collected events list. |
| **ProcessBatchWorker** | `eb_process_batch` | Processes a single batch of events by index. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
eb_collect_events
 │
 ▼
eb_create_batches
 │
 ▼
DO_WHILE
 └── eb_process_batch

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
