# Beneficiary Tracking

Orchestrates beneficiary tracking through a multi-stage Conductor workflow.

**Input:** `beneficiaryName`, `programId`, `location` | **Timeout:** 60s

## Pipeline

```
btr_register
    │
btr_assess_needs
    │
btr_provide_services
    │
btr_monitor
    │
btr_report
```

## Workers

**AssessNeedsWorker** (`btr_assess_needs`)

Reads `beneficiaryId`. Outputs `needs`.

**MonitorWorker** (`btr_monitor`)

Reads `beneficiaryId`. Outputs `outcomes`.

**ProvideServicesWorker** (`btr_provide_services`)

Reads `beneficiaryId`, `programId`. Outputs `services`, `sessionsCompleted`.

**RegisterWorker** (`btr_register`)

Reads `beneficiaryName`, `location`. Outputs `beneficiaryId`, `registered`.

**ReportWorker** (`btr_report`)

Reads `beneficiaryId`, `outcomes`. Outputs `tracking`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
