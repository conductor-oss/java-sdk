# Industrial Iot

Orchestrates industrial iot through a multi-stage Conductor workflow.

**Input:** `plantId`, `machineId`, `machineType` | **Timeout:** 60s

## Pipeline

```
iit_monitor_machines
    │
iit_predictive_analysis
    │
iit_schedule_repair
```

## Workers

**MonitorMachinesWorker** (`iit_monitor_machines`)

Reads `telemetry`. Outputs `telemetry`, `temperature`, `vibration`, `pressure`, `rpm`.

**PredictiveAnalysisWorker** (`iit_predictive_analysis`)

Reads `failureProbability`. Outputs `failureProbability`, `predictedComponent`, `estimatedRemainingLife`, `modelConfidence`.

**ScheduleRepairWorker** (`iit_schedule_repair`)

Reads `repairScheduled`. Outputs `repairScheduled`, `workOrderId`, `scheduledDate`, `partsRequired`, `estimatedDowntime`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
