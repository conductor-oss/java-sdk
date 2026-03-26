# Real Time Analytics

A ride-sharing platform needs to compute real-time metrics from a stream of trip events: ingest the raw event, enrich it with driver and rider metadata, compute running aggregates (average fare, trip count per zone), and push updated metrics to a dashboard endpoint. Stale metrics older than 5 minutes are worse than no metrics.

## Pipeline

```
[ry_ingest_events]
     |
     v
[ry_process_stream]
     |
     v
[ry_update_aggregates]
     |
     v
[ry_check_alerts]
```

**Workflow inputs:** `eventBatch`, `windowSize`, `alertRules`

## Workers

**CheckAlertsWorker** (task: `ry_check_alerts`)

Checks alert rules against current aggregates and triggers alerts.

- Parses strings to `int`
- Writes `alerts`, `alertCount`

**IngestEventsWorker** (task: `ry_ingest_events`)

Ingests a batch of events for real-time analytics processing.

- Writes `events`, `eventCount`

**ProcessStreamWorker** (task: `ry_process_stream`)

Processes the event stream: computes window metrics, flags anomalies.

- Writes `processed`, `processedCount`, `windowMetrics`

**UpdateAggregatesWorker** (task: `ry_update_aggregates`)

Updates running aggregates with the latest window metrics.

- Writes `aggregates`, `updatedCount`

---

**20 tests** | Workflow: `real_time_analytics` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
