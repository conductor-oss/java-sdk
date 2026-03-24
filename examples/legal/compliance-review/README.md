# Compliance Review

Orchestrates compliance review through a multi-stage Conductor workflow.

**Input:** `regulationType`, `entityId` | **Timeout:** 60s

## Pipeline

```
cmr_identify
    │
cmr_assess
    │
cmr_gap_analysis
    │
cmr_remediate
```

## Workers

**AssessWorker** (`cmr_assess`)

Outputs `assessment`, `requirements`, `gaps`.

**GapAnalysisWorker** (`cmr_gap_analysis`)

Outputs `gapCount`, `requirements`, `assessment`, `gaps`.

**IdentifyWorker** (`cmr_identify`)

Outputs `requirements`, `assessment`, `gaps`.

**RemediateWorker** (`cmr_remediate`)

Outputs `planId`, `requirements`, `assessment`, `gaps`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
