# Water Management

Orchestrates water management through a multi-stage Conductor workflow.

**Input:** `zoneId`, `sensorGroup` | **Timeout:** 60s

## Pipeline

```
wtr_monitor_levels
    │
wtr_analyze_quality
    │
wtr_detect_leaks
    │
wtr_alert
```

## Workers

**AlertWorker** (`wtr_alert`)

Outputs `done`.

**AnalyzeQualityWorker** (`wtr_analyze_quality`)

Outputs `done`.

**DetectLeaksWorker** (`wtr_detect_leaks`)

Outputs `confidence`.

**MonitorLevelsWorker** (`wtr_monitor_levels`)

Outputs `done`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
