# Event Split in Java Using Conductor

Splits a composite event into multiple sub-events for parallel processing using FORK_JOIN. ## The Problem

You need to decompose a composite event into multiple independent sub-events for parallel processing. A single incoming event may contain data for multiple downstream systems. analytics, billing, and notification. Splitting the composite event and processing each sub-event in parallel reduces latency compared to sequential processing, and isolates failures so a billing error does not block notification delivery.

Without orchestration, you'd manually parse the composite event, extract sub-payloads, spawn threads for parallel processing, synchronize completion with barriers, and aggregate results. handling the case where one sub-event processor fails while others succeed.

## The Solution

**You just write the event-receive, split, per-sub-event processing, and result-combination workers. Conductor handles parallel sub-event processing, per-processor retry isolation, and automatic result combination after all branches complete.**

Each sub-event processor is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of splitting the composite event, processing all sub-events in parallel via FORK_JOIN, aggregating results after all complete, retrying any failed sub-event processor independently, and tracking the entire split-and-process operation. ### What You Write: Workers

Six workers decompose and process composite events: ReceiveCompositeWorker ingests the event, SplitEventWorker breaks it into sub-events, ProcessSubAWorker, ProcessSubBWorker, and ProcessSubCWorker each handle one piece in parallel via FORK_JOIN, and CombineResultsWorker merges the outcomes.

| Worker | Task | What It Does |
|---|---|---|
| **CombineResultsWorker** | `sp_combine_results` | Combines results from all three parallel sub-event processors. |
| **ProcessSubAWorker** | `sp_process_sub_a` | Processes sub-event A (order details). |
| **ProcessSubBWorker** | `sp_process_sub_b` | Processes sub-event B (customer info). |
| **ProcessSubCWorker** | `sp_process_sub_c` | Processes sub-event C (shipping info). |
| **ReceiveCompositeWorker** | `sp_receive_composite` | Receives a composite event and passes it through. |
| **SplitEventWorker** | `sp_split_event` | Splits a composite event into three sub-events. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
sp_receive_composite
 │
 ▼
sp_split_event
 │
 ▼
FORK_JOIN
 ├── sp_process_sub_a
 ├── sp_process_sub_b
 └── sp_process_sub_c
 │
 ▼
JOIN (wait for all branches)
sp_combine_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
