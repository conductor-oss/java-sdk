# Event Aggregation in Java Using Conductor

Event Aggregation Pipeline: collect events from a time window, aggregate metrics, generate a summary report, and publish the batch. ## The Problem

You need to aggregate events from a time window into summary metrics. The pipeline must collect all events within a specified window, compute aggregate statistics (counts, sums, averages, percentiles), generate a human-readable summary report, and publish the aggregated batch downstream. Without aggregation, downstream systems are overwhelmed by high-volume raw events; without windowing, you lose temporal context.

Without orchestration, you'd build a stateful aggregation service with in-memory buffers, manual window management, and ad-hoc metric calculations. handling buffer overflows when event volume spikes, recovering lost state after crashes, and debugging why a window's metrics do not add up.

## The Solution

**You just write the event-collection, metrics-aggregation, summary-generation, and batch-publish workers. Conductor handles window lifecycle management, retry on publish failure, and a durable record of every aggregation window.**

Each aggregation concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect, aggregate, summarize, publish), retrying if the downstream publish fails, tracking every aggregation window with full input/output details, and resuming from the last step if the process crashes. ### What You Write: Workers

Four workers drive the aggregation pipeline: CollectEventsWorker gathers events from a time window, AggregateMetricsWorker computes totals and averages, GenerateSummaryWorker produces a human-readable report, and PublishBatchWorker sends the aggregated result downstream.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateMetricsWorker** | `eg_aggregate_metrics` | Aggregates metrics from collected events. Produces fixed totals: totalRevenue=478.47, avgOrderValue=100.70, purchaseC... |
| **CollectEventsWorker** | `eg_collect_events` | Collects events from a time window. Returns a fixed set of 6 transaction events (purchases and refunds) along with th... |
| **GenerateSummaryWorker** | `eg_generate_summary` | Generates a human-readable summary report from the aggregated metrics, including highlights and a destination for the... |
| **PublishBatchWorker** | `eg_publish_batch` | Publishes the aggregated summary batch to the configured destination. Returns a fixed batch ID and publish timestamp. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
eg_collect_events
 │
 ▼
eg_aggregate_metrics
 │
 ▼
eg_generate_summary
 │
 ▼
eg_publish_batch

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
