# Regulatory Filing

Orchestrates regulatory filing through a multi-stage Conductor workflow.

**Input:** `filingType`, `entityName`, `jurisdiction` | **Timeout:** 60s

## Pipeline

```
rgf_prepare
    │
rgf_validate
    │
rgf_submit
    │
rgf_track
    │
rgf_confirm
```

## Workers

**ConfirmWorker** (`rgf_confirm`)

Reads `submissionId`. Outputs `confirmationNumber`, `confirmed`.

**PrepareWorker** (`rgf_prepare`)

Reads `entityName`, `filingType`. Outputs `filingId`, `filingPackage`.

**SubmitWorker** (`rgf_submit`)

Outputs `submissionId`, `submittedAt`.

**TrackWorker** (`rgf_track`)

Reads `submissionId`. Outputs `trackingStatus`, `estimatedProcessingDays`.

**ValidateWorker** (`rgf_validate`)

Outputs `validated`, `errors`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
