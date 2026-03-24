# Travel Analytics

Travel analytics: collect, aggregate, analyze, report.

**Input:** `period`, `department` | **Timeout:** 60s

## Pipeline

```
tan_collect
    │
tan_aggregate
    │
tan_analyze
    │
tan_report
```

## Workers

**AggregateWorker** (`tan_aggregate`)

Outputs `aggregated`.

**AnalyzeWorker** (`tan_analyze`)

Outputs `insights`.

**CollectWorker** (`tan_collect`)

Reads `department`, `period`. Outputs `rawData`.

**ReportWorker** (`tan_report`)

Outputs `reportId`, `published`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
