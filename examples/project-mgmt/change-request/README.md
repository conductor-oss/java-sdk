# Change Request

Change request: submit, assess impact, approve, implement, verify.

**Input:** `changeId`, `description`, `requestedBy` | **Timeout:** 60s

## Pipeline

```
chr_submit
    │
chr_assess_impact
    │
chr_approve
    │
chr_implement
    │
chr_verify
```

## Workers

**ApproveWorker** (`chr_approve`)

Reads `changeId`. Outputs `approval`.

**AssessImpactWorker** (`chr_assess_impact`)

Reads `changeId`. Outputs `impact`.

**ImplementWorker** (`chr_implement`)

Reads `changeId`. Outputs `result`.

**SubmitWorker** (`chr_submit`)

Reads `changeId`, `description`. Outputs `details`.

**VerifyWorker** (`chr_verify`)

Reads `changeId`. Outputs `verification`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
