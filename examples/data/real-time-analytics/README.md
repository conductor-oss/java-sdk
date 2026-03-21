# Real-Time Analytics in Java Using Conductor : Event Ingestion, Windowed Stream Processing, Aggregate Updates, and Alert Checking

## The Problem

Events are arriving in batches from your application: page views, API calls, purchases, error occurrences, and you need analytics that reflect what's happening right now, not what happened yesterday. Each batch of events needs to be processed within a time window (last 5 minutes, last hour) to compute windowed metrics like requests per second, error rate, and 95th percentile latency. Those window metrics need to update running aggregates so dashboards show cumulative trends. And when an aggregate crosses a threshold, error rate exceeds 5%, latency spikes above 500ms, purchase volume drops below normal, alert rules need to fire immediately, not after a nightly batch job.

Without orchestration, you'd write a single event processor that ingests, windows, aggregates, and alerts in one loop. If the aggregate store is temporarily unavailable, the entire pipeline stops. Events back up, window metrics are lost, and alerts never fire. There's no visibility into how many events were ingested vs: processed, whether the window computation is the bottleneck, or which alert rules were evaluated. Adding a new alert rule or changing the window size means modifying the same tightly coupled loop that handles ingestion.

## The Solution

**You just write the event ingestion, windowed processing, aggregate updating, and alert checking workers. Conductor handles the ingest-window-aggregate-alert pipeline, retries when aggregate stores are temporarily unavailable, and event count tracking at every stage.**

Each stage of the analytics pipeline is a simple, independent worker. The event ingester validates and normalizes the incoming event batch, counting the events for tracking. The stream processor groups events by the configured window size, computes windowed metrics (event rate, error rate, distribution statistics), and flags anomalous windows. The aggregate updater merges the latest window metrics into the running aggregates, maintaining cumulative counters and moving averages. The alert checker evaluates the configured alert rules against current aggregates and triggers alerts when thresholds are breached. Conductor executes them in strict sequence, passes events through the pipeline, retries if the aggregate store is temporarily unavailable, and tracks event counts, processed counts, and alert counts at every stage. ### What You Write: Workers

Four workers power the real-time analytics pipeline: ingesting and normalizing event batches, processing events within configurable time windows, updating running aggregates, and evaluating alert rules to trigger notifications when thresholds are breached.

| Worker | Task | What It Does |
|---|---|---|
| **CheckAlertsWorker** | `ry_check_alerts` | Checks alert rules against current aggregates and triggers alerts. |
| **IngestEventsWorker** | `ry_ingest_events` | Ingests a batch of events for real-time analytics processing. |
| **ProcessStreamWorker** | `ry_process_stream` | Processes the event stream: computes window metrics, flags anomalies. |
| **UpdateAggregatesWorker** | `ry_update_aggregates` | Updates running aggregates with the latest window metrics. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
ry_ingest_events
 │
 ▼
ry_process_stream
 │
 ▼
ry_update_aggregates
 │
 ▼
ry_check_alerts

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
