# Compliance Nonprofit

Orchestrates compliance nonprofit through a multi-stage Conductor workflow.

**Input:** `organizationName`, `fiscalYear`, `ein` | **Timeout:** 60s

## Pipeline

```
cnp_audit
    │
cnp_verify_filings
    │
cnp_check_requirements
    │
cnp_report
    │
cnp_submit
```

## Workers

**AuditWorker** (`cnp_audit`)

Reads `fiscalYear`, `organizationName`. Outputs `results`.

**CheckRequirementsWorker** (`cnp_check_requirements`)

Outputs `compliance`.

**ReportWorker** (`cnp_report`)

Reads `organizationName`. Outputs `report`.

**SubmitWorker** (`cnp_submit`)

Reads `ein`. Outputs `submission`.

**VerifyFilingsWorker** (`cnp_verify_filings`)

Reads `ein`. Outputs `status`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
