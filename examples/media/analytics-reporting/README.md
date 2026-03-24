# Analytics Reporting

Orchestrates analytics reporting through a multi-stage Conductor workflow.

**Input:** `reportId`, `dateRange`, `dataSources` | **Timeout:** 60s

## Pipeline

```
anr_collect_events
    │
anr_aggregate_data
    │
anr_compute_metrics
    │
anr_generate_report
```

## Workers

**AggregateDataWorker** (`anr_aggregate_data`)

Reads `aggregatedData`. Outputs `aggregatedData`, `totalSessions`, `uniqueUsers`, `pageViews`, `avgSessionDuration`.

**CollectEventsWorker** (`anr_collect_events`)

Reads `eventCount`. Outputs `eventCount`, `rawDataPath`, `sourcesProcessed`, `collectionTimeMs`.

**ComputeMetricsWorker** (`anr_compute_metrics`)

Reads `metrics`. Outputs `metrics`, `dau`, `wau`, `mau`, `bounceRate`.

**GenerateReportWorker** (`anr_generate_report`)

Reads `reportUrl`. Outputs `reportUrl`, `format`, `generatedAt`, `scheduledDelivery`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
