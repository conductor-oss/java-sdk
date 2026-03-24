# Event Dedup in Java Using Conductor

Event deduplication workflow. computes a hash of the event payload, checks if the event has been seen before, and either processes or skips the event via a SWITCH task.

## The Problem

You need to deduplicate events so that the same event is never processed twice. In distributed systems, at-least-once delivery guarantees mean consumers may receive duplicate messages. The workflow must compute a content hash of the event payload, check whether that hash has been seen before, and either process the event (if new) or skip it (if duplicate). Processing duplicates can lead to double charges, duplicate notifications, or corrupted state.

Without orchestration, you'd maintain a deduplication cache (in-memory set, Redis, database table), manually check before processing each event, handle race conditions in concurrent consumers, and deal with cache eviction that causes previously-seen events to be reprocessed.

## The Solution

**You just write the hash-computation, seen-check, event-processing, and skip workers. Conductor handles SWITCH-based duplicate routing, guaranteed dedup decisions, and a full record of every event's dedup outcome.**

Each deduplication concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of computing the hash, checking the dedup store, routing via a SWITCH task to process or skip, and tracking every event's dedup decision.

### What You Write: Workers

Four workers enforce exactly-once event processing: ComputeHashWorker generates a content fingerprint, CheckSeenWorker queries the dedup store, ProcessEventWorker handles new events, and SkipEventWorker discards duplicates.

| Worker | Task | What It Does |
|---|---|---|
| **CheckSeenWorker** | `dd_check_seen` | Checks whether a given hash has been seen before (demo lookup). Always returns "duplicate" to demonstrate a previou... |
| **ComputeHashWorker** | `dd_compute_hash` | Computes a deterministic hash of the event payload for deduplication. Uses a fixed hash value for demonstration purpo... |
| **ProcessEventWorker** | `dd_process_event` | Processes a new (non-duplicate) event. |
| **SkipEventWorker** | `dd_skip_event` | Skips a duplicate (or unknown-status) event. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
dd_compute_hash
 │
 ▼
dd_check_seen
 │
 ▼
SWITCH (dedup_switch_ref)
 ├── new: dd_process_event
 ├── duplicate: dd_skip_event
 └── default: dd_skip_event

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
