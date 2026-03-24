# Usage Analytics

Orchestrates usage analytics through a multi-stage Conductor workflow.

**Input:** `region`, `period` | **Timeout:** 60s

## Pipeline

```
uag_collect_cdrs
    │
uag_process
    │
uag_aggregate
    │
uag_report
    │
uag_alert
```

## Workers

**AggregateWorker** (`uag_aggregate`)

Outputs `aggregates`, `anomalies`.

**AlertWorker** (`uag_alert`)

Outputs `alertCount`, `notified`.

**CollectCdrsWorker** (`uag_collect_cdrs`)

Reads `region`. Outputs `cdrCount`, `sources`.

**ProcessWorker** (`uag_process`)

Outputs `processedRecords`, `duplicatesRemoved`.

**ReportWorker** (`uag_report`)

Reads `region`. Outputs `reportId`, `generated`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
