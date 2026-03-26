# Title Search

Orchestrates title search through a multi-stage Conductor workflow.

**Input:** `propertyId`, `address`, `county` | **Timeout:** 60s

## Pipeline

```
tts_search
    │
tts_verify_ownership
    │
tts_check_liens
    │
tts_certify
```

## Workers

**CertifyTitleWorker** (`tts_certify`)

Outputs `titleStatus`, `certificateId`, `records`.

**CheckLiensWorker** (`tts_check_liens`)

Outputs `clear`, `certificateId`, `records`.

**SearchRecordsWorker** (`tts_search`)

Outputs `records`, `certificateId`.

**VerifyOwnershipWorker** (`tts_verify_ownership`)

Outputs `verified`, `certificateId`, `records`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
