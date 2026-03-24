# Litigation Hold

Orchestrates litigation hold through a multi-stage Conductor workflow.

**Input:** `caseId`, `custodians` | **Timeout:** 60s

## Pipeline

```
lth_identify
    │
lth_notify
    │
lth_collect
    │
lth_preserve
    │
lth_track
```

## Workers

**CollectWorker** (`lth_collect`)

Reads `caseId`. Outputs `collectedData`, `totalItems`.

**IdentifyWorker** (`lth_identify`)

Reads `caseId`, `custodians`. Outputs `holdId`, `identifiedCustodians`, `dataSources`.

**NotifyWorker** (`lth_notify`)

Reads `caseId`. Outputs `notifiedCount`, `notificationsSent`.

**PreserveWorker** (`lth_preserve`)

Outputs `preservationId`, `preserved`.

**TrackWorker** (`lth_track`)

Reads `caseId`. Outputs `trackingId`, `status`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
