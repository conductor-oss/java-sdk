# Stream Processing in Java Using Conductor : Event Ingestion, Time Windowing, Window Aggregation, Anomaly Detection, and Result Emission

## The Problem

Events are arriving continuously. API requests, sensor readings, user actions, financial transactions, and you need to analyze them in near-real-time using time windows. A configurable window size (say, 60,000ms for 1-minute windows) groups events by time, and each window needs aggregate statistics: how many events occurred, what was the average value, what was the distribution. Then anomaly detection needs to run across windows: a window with 10x the normal event count might indicate a traffic spike or DDoS, while a window with zero events might indicate a service outage. The results: both normal aggregates and anomaly alerts, need to be emitted to downstream consumers.

Without orchestration, you'd write a single stream processor that ingests, windows, aggregates, detects anomalies, and emits in one tight loop. If the anomaly detection algorithm needs tuning, you'd re-ingest and re-window everything. There's no visibility into how many events were ingested vs, how many windows were created vs, how many anomalies were detected. If the process crashes after windowing but before anomaly detection, the windowed results are lost and the entire batch must be reprocessed from scratch.

## The Solution

**You just write the event ingestion, time windowing, per-window aggregation, anomaly detection, and result emission workers. Conductor handles the sequential stream pipeline, per-stage retries, and tracking of event counts, window counts, and anomaly counts at every stage.**

Each stage of the stream pipeline is a simple, independent worker. The stream ingester validates and normalizes the incoming event batch, counting the total events. The windower groups events into time windows based on the configured window size in milliseconds, producing a list of windows each containing their events. The aggregator computes per-window statistics: event count, sum, average, min, max, across all windows. The anomaly detector scans the window aggregates for outliers, flagging windows where metrics deviate significantly from expected ranges (sudden spikes, unexpected drops, unusual distributions). The emitter combines aggregates and anomaly alerts into a final summary for downstream consumption. Conductor executes them in strict sequence, passes the evolving event data between stages, retries if any stage fails, and tracks event counts, window counts, and anomaly counts at every stage. ### What You Write: Workers

Five workers handle windowed stream analytics: ingesting timestamped events, grouping them into configurable time windows, computing per-window aggregates (count, average, distribution), detecting anomalies across windows, and emitting the combined results.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWindowsWorker** | `st_aggregate_windows` | Aggregate Windows. Computes and returns aggregates |
| **DetectAnomaliesWorker** | `st_detect_anomalies` | Detect Anomalies. Computes and returns anomalies, anomaly count, global avg |
| **EmitResultsWorker** | `st_emit_results` | Handles emit results |
| **IngestStreamWorker** | `st_ingest_stream` | Ingest Stream. Computes and returns events, event count |
| **WindowEventsWorker** | `st_window_events` | Window Events. Computes and returns windows, window count |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
st_ingest_stream
 │
 ▼
st_window_events
 │
 ▼
st_aggregate_windows
 │
 ▼
st_detect_anomalies
 │
 ▼
st_emit_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
