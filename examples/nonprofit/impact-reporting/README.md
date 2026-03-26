# Impact Reporting

Orchestrates impact reporting through a multi-stage Conductor workflow.

**Input:** `programName`, `reportYear` | **Timeout:** 60s

## Pipeline

```
ipr_collect_data
    │
ipr_aggregate
    │
ipr_analyze
    │
ipr_format
    │
ipr_publish
```

## Workers

**AggregateWorker** (`ipr_aggregate`)

Outputs `aggregated`.

**AnalyzeWorker** (`ipr_analyze`)

Outputs `analysis`.

**CollectDataWorker** (`ipr_collect_data`)

Reads `programName`, `year`. Outputs `data`.

**FormatWorker** (`ipr_format`)

Reads `programName`. Outputs `report`.

**PublishWorker** (`ipr_publish`)

Outputs `impact`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
