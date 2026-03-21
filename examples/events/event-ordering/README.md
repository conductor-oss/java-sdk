# Event Ordering in Java Using Conductor

Event Ordering. buffers incoming events, sorts them by sequence number, and processes each in order using a DO_WHILE loop. ## The Problem

You need to process events in strict sequence order, even when they arrive out of order. In distributed systems, events with sequence numbers 1, 3, 2, 5, 4 may arrive in any order due to network jitter or parallel producers. The workflow must buffer incoming events, sort them by sequence number, and process each one in order using a loop. Processing out-of-order events corrupts state in systems that depend on causal ordering (e.g., bank transactions, state machines).

Without orchestration, you'd build a reordering buffer with a priority queue, manage watermarks to know when all events for a window have arrived, handle gaps in sequence numbers, and process each event in a loop. manually ensuring the buffer does not grow unbounded and that late arrivals are handled correctly.

## The Solution

**You just write the event-buffering, sorting, and sequential-processing workers. Conductor handles DO_WHILE sequential processing, durable buffer state, and per-event retry within the ordered loop.**

Each ordering concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of buffering events, sorting them by sequence number, processing each in order via a DO_WHILE loop, retrying any failed processing step, and tracking the entire ordering operation. ### What You Write: Workers

Three workers enforce causal ordering: BufferEventsWorker collects incoming events, SortEventsWorker arranges them by sequence number, and ProcessNextWorker handles each one sequentially in a DO_WHILE loop.

| Worker | Task | What It Does |
|---|---|---|
| **BufferEventsWorker** | `oo_buffer_events` | Buffers incoming events. accepts a list of events and outputs them as a buffered collection along with the total count. |
| **ProcessNextWorker** | `oo_process_next` | Processes the next event from the sorted list based on the current DO_WHILE iteration index. Gets the event at positi... |
| **SortEventsWorker** | `oo_sort_events` | Sorts buffered events by the "seq" field in ascending order. For determinism, always returns the fixed sorted order: ... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
oo_buffer_events
 │
 ▼
oo_sort_events
 │
 ▼
DO_WHILE
 └── oo_process_next

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
