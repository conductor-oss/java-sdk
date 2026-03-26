# Stream Processing

A telemetry pipeline receives a continuous stream of sensor events. Each event window needs ingestion, deduplication (sensors sometimes double-fire), aggregation into window-level statistics, and emission of the aggregated result to a downstream consumer. If the aggregation step is slow, backpressure must prevent the ingestion buffer from overflowing.

## Pipeline

```
[st_ingest_stream]
     |
     v
[st_window_events]
     |
     v
[st_aggregate_windows]
     |
     v
[st_detect_anomalies]
     |
     v
[st_emit_results]
```

**Workflow inputs:** `events`, `windowSizeMs`

## Workers

**AggregateWindowsWorker** (task: `st_aggregate_windows`)

- Rounds with `math.round()`
- Reads `windows`. Writes `aggregates`

**DetectAnomaliesWorker** (task: `st_detect_anomalies`)

- Rounds with `math.round()`, uses `math.abs()`
- Reads `aggregates`. Writes `anomalies`, `anomalyCount`, `globalAvg`

**EmitResultsWorker** (task: `st_emit_results`)

- Reads `aggregates`, `anomalies`. Writes `summary`

**IngestStreamWorker** (task: `st_ingest_stream`)

- Reads `events`. Writes `events`, `eventCount`

**WindowEventsWorker** (task: `st_window_events`)

- Reads `events`, `windowSizeMs`. Writes `windows`, `windowCount`

---

**40 tests** | Workflow: `stream_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
