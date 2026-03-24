# Wearable Data

Orchestrates wearable data through a multi-stage Conductor workflow.

**Input:** `userId`, `deviceId` | **Timeout:** 60s

## Pipeline

```
wer_collect_vitals
    │
wer_process_data
    │
wer_detect_anomalies
    │
wer_notify
```

## Workers

**CollectVitalsWorker** (`wer_collect_vitals`)

Outputs `done`.

**DetectAnomaliesWorker** (`wer_detect_anomalies`)

Outputs `anomalyCount`.

**NotifyWorker** (`wer_notify`)

Outputs `done`.

**ProcessDataWorker** (`wer_process_data`)

Outputs `processed`, `avgHeartRate`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
