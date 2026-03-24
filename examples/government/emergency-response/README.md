# Emergency Response

Orchestrates emergency response through a multi-stage Conductor workflow.

**Input:** `incidentType`, `location`, `reportedBy` | **Timeout:** 60s

## Pipeline

```
emr_detect
    │
emr_classify_severity
    │
emr_dispatch
    │
emr_coordinate
    │
emr_debrief
```

## Workers

**ClassifySeverityWorker** (`emr_classify_severity`)

Reads `incidentId`. Outputs `severity`, `responseLevel`.

**CoordinateWorker** (`emr_coordinate`)

Reads `incidentId`. Outputs `outcome`, `duration`, `casualties`.

**DebriefWorker** (`emr_debrief`)

Reads `incidentId`, `outcome`. Outputs `reportFiled`, `lessonsLearned`.

**DetectWorker** (`emr_detect`)

Reads `incidentType`, `location`. Outputs `incidentId`, `detectedAt`.

**DispatchWorker** (`emr_dispatch`)

Reads `location`, `severity`. Outputs `units`, `dispatchTime`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
