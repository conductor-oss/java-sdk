# Property Inspection

Orchestrates property inspection through a multi-stage Conductor workflow.

**Input:** `propertyId`, `inspectorId`, `inspectionType` | **Timeout:** 60s

## Pipeline

```
pin_schedule
    │
pin_inspect
    │
pin_document
    │
pin_report
```

## Workers

**DocumentWorker** (`pin_document`)

Outputs `documentation`, `findings`.

**InspectWorker** (`pin_inspect`)

Outputs `findings`, `documentation`.

**ReportWorker** (`pin_report`)

Outputs `reportId`, `findings`, `documentation`.

**ScheduleWorker** (`pin_schedule`)

Outputs `scheduledDate`, `findings`, `documentation`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
