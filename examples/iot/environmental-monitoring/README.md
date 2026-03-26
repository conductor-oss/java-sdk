# Environmental Monitoring

Orchestrates environmental monitoring through a multi-stage Conductor workflow.

**Input:** `stationId`, `region`, `monitoringType` | **Timeout:** 60s

## Pipeline

```
env_collect_data
    │
env_check_thresholds
    │
env_trigger_alert
    │
env_generate_report
```

## Workers

**CheckThresholdsWorker** (`env_check_thresholds`)

Reads `breachCount`. Outputs `breachCount`, `aqi`.

**CollectDataWorker** (`env_collect_data`)

Reads `readings`. Outputs `readings`, `pm25`, `pm10`, `co2`, `no2`.

**GenerateReportWorker** (`env_generate_report`)

Reads `reportId`. Outputs `reportId`, `reportUrl`, `generatedAt`, `complianceStatus`.

**TriggerAlertWorker** (`env_trigger_alert`)

Reads `alertsSent`. Outputs `alertsSent`, `notifications`, `escalated`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
