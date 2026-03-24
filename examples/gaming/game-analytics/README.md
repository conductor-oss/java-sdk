# Game Analytics

Orchestrates game analytics through a multi-stage Conductor workflow.

**Input:** `gameId`, `dateRange` | **Timeout:** 60s

## Pipeline

```
gan_collect_events
    │
gan_process
    │
gan_aggregate
    │
gan_compute_kpis
    │
gan_report
```

## Workers

**AggregateWorker** (`gan_aggregate`)

Outputs `aggregated`.

**CollectEventsWorker** (`gan_collect_events`)

Reads `dateRange`, `gameId`. Outputs `events`, `types`.

**ComputeKpisWorker** (`gan_compute_kpis`)

Outputs `kpis`.

**ProcessWorker** (`gan_process`)

Reads `rawEvents`. Outputs `processed`.

**ReportWorker** (`gan_report`)

Reads `gameId`, `kpis`. Outputs `report`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
