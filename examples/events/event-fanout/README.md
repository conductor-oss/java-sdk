# Event Fanout in Java Using Conductor

Event fan-out workflow that receives an event, fans out to analytics, storage, and notification processing in parallel via FORK_JOIN, then aggregates the results.

## The Problem

You need to distribute a single event to multiple downstream consumers simultaneously. When an event arrives, it must be processed by analytics (for tracking), storage (for persistence), and notification (for alerting). all in parallel so no single slow consumer delays the others. After all consumers finish, results must be aggregated into a unified response.

Without orchestration, you'd spawn threads for each consumer, manage a CountDownLatch or CompletableFuture for synchronization, handle partial failures when one consumer crashes while others succeed, and manually aggregate results from different threads.

## The Solution

**You just write the event-receive, analytics, storage, notification, and aggregation workers. Conductor handles parallel fan-out via FORK_JOIN, per-consumer retry isolation, and automatic aggregation after all consumers complete.**

Each downstream consumer is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of fanning out to all three consumers in parallel via FORK_JOIN, waiting for all to complete, aggregating results, retrying any failed consumer independently, and tracking the entire fanout operation.

### What You Write: Workers

Five workers implement parallel event distribution: ReceiveEventWorker ingests the event, then AnalyticsWorker, StorageWorker, and NotificationWorker process it simultaneously via FORK_JOIN, and AggregateWorker combines all outcomes.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `fo_aggregate` | Aggregates results from the three parallel fan-out branches. |
| **AnalyticsWorker** | `fo_analytics` | Tracks event analytics and updates metrics. |
| **NotificationWorker** | `fo_notification` | Sends a notification about the event. |
| **ReceiveEventWorker** | `fo_receive_event` | Receives an incoming event and passes it through for fan-out processing. |
| **StorageWorker** | `fo_storage` | Stores the event payload to a data lake. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
fo_receive_event
 │
 ▼
FORK_JOIN
 ├── fo_analytics
 ├── fo_storage
 └── fo_notification
 │
 ▼
JOIN (wait for all branches)
fo_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
