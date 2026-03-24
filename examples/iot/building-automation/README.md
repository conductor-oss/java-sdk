# Building Automation

Orchestrates building automation through a multi-stage Conductor workflow.

**Input:** `buildingId`, `floor` | **Timeout:** 60s

## Pipeline

```
bld_monitor_systems
    │
bld_optimize
    │
bld_schedule
    │
bld_adjust
```

## Workers

**AdjustWorker** (`bld_adjust`)

Outputs `applied`, `adjustmentCount`.

**MonitorSystemsWorker** (`bld_monitor_systems`)

Outputs `hvacTemp`.

**OptimizeWorker** (`bld_optimize`)

Outputs `energySaved`, `costSavings`.

**ScheduleWorker** (`bld_schedule`)

Outputs `scheduledCount`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
