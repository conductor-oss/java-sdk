# Medical Records Review

Medical Records Review -- validate HIPAA compliance, wait for physician review, then store result.

**Input:** `recordId` | **Timeout:** 300s

**Output:** `compliant`, `checks`, `stored`, `auditTrailId`

## Pipeline

```
mr_validate_hipaa
    │
physician_review [WAIT]
    │
mr_store_result
```

## Workers

**MrStoreResultWorker** (`mr_store_result`): Worker for mr_store_result task -- stores the physician review result.

Outputs `stored`, `auditTrailId`.

**MrValidateHipaaWorker** (`mr_validate_hipaa`): Worker for mr_validate_hipaa task -- validates HIPAA compliance checks.

Outputs `compliant`, `checks`.

## Workflow Output

- `compliant`: `${validate_hipaa_ref.output.compliant}`
- `checks`: `${validate_hipaa_ref.output.checks}`
- `stored`: `${store_result_ref.output.stored}`
- `auditTrailId`: `${store_result_ref.output.auditTrailId}`

## Data Flow

**mr_validate_hipaa**: `recordId` = `${workflow.input.recordId}`
**mr_store_result**: `recordId` = `${workflow.input.recordId}`, `compliant` = `${validate_hipaa_ref.output.compliant}`, `physicianDecision` = `${physician_review_ref.output.decision}`

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
