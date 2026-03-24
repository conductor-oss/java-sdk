# Tenant Screening

Orchestrates tenant screening through a multi-stage Conductor workflow.

**Input:** `applicantName`, `propertyId`, `monthlyRent` | **Timeout:** 60s

## Pipeline

```
tsc_apply
    │
tsc_background
    │
tsc_credit
    │
tsc_decision
```

## Workers

**ApplyWorker** (`tsc_apply`)

Outputs `applicationId`, `score`.

**BackgroundCheckWorker** (`tsc_background`)

Outputs `result`, `score`.

**CreditCheckWorker** (`tsc_credit`)

Outputs `result`, `score`.

**DecisionWorker** (`tsc_decision`)

Outputs `decision`, `score`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
