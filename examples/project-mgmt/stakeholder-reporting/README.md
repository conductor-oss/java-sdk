# Stakeholder Reporting

Stakeholder reporting: collect updates, aggregate, format, distribute.

**Input:** `projectId`, `reportPeriod` | **Timeout:** 60s

## Pipeline

```
shr_collect_updates
    │
shr_aggregate
    │
shr_format
    │
shr_distribute
```

## Workers

**AggregateWorker** (`shr_aggregate`)

Outputs `summary`.

**CollectUpdatesWorker** (`shr_collect_updates`)

Reads `projectId`. Outputs `updates`.

**DistributeWorker** (`shr_distribute`)

Reads `projectId`. Outputs `distributed`.

**FormatWorker** (`shr_format`)

Outputs `report`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
