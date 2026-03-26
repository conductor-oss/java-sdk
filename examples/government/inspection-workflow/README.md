# Inspection Workflow

Orchestrates inspection workflow through a multi-stage Conductor workflow.

**Input:** `propertyId`, `inspectionType` | **Timeout:** 60s

## Pipeline

```
inw_schedule
    │
inw_inspect
    │
inw_document
    │
route_result [SWITCH]
  ├─ pass: inw_record_pass
  └─ fail: inw_record_fail
```

## Workers

**DocumentWorker** (`inw_document`)

Outputs `result`, `violations`.

**InspectWorker** (`inw_inspect`)

Reads `propertyId`. Outputs `findings`.

**RecordFailWorker** (`inw_record_fail`)

Reads `propertyId`. Outputs `reinspectionRequired`.

**RecordPassWorker** (`inw_record_pass`)

Reads `propertyId`. Outputs `certificate`.

**ScheduleWorker** (`inw_schedule`)

Reads `propertyId`. Outputs `scheduledDate`, `inspector`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
