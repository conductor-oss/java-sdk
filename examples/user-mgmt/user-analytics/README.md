# User Analytics

Orchestrates user analytics through a multi-stage Conductor workflow.

**Input:** `dateRange`, `segments` | **Timeout:** 60s

## Pipeline

```
uan_collect_events
    │
uan_aggregate
    │
uan_compute_metrics
    │
uan_report
```

## Workers

**AggregateWorker** (`uan_aggregate`)

Outputs `aggregated`, `uniqueUsers`.

**AnalyticsReportWorker** (`uan_report`)

Outputs `dashboardUrl`, `generatedAt`.

**CollectEventsWorker** (`uan_collect_events`)

Reads `dateRange`. Outputs `events`, `totalEvents`.

**ComputeMetricsWorker** (`uan_compute_metrics`)

Outputs `metrics`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
