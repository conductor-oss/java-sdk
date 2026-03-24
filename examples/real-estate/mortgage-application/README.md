# Mortgage Application

Orchestrates mortgage application through a multi-stage Conductor workflow.

**Input:** `applicantId`, `loanAmount`, `propertyValue` | **Timeout:** 60s

## Pipeline

```
mtg_apply
    │
mtg_credit_check
    │
mtg_underwrite
    │
mtg_approve
    │
mtg_close
```

## Workers

**ApplyWorker** (`mtg_apply`)

Outputs `applicationId`.

**ApproveWorker** (`mtg_approve`)

Outputs `loanId`.

**CloseWorker** (`mtg_close`)

Outputs `closingStatus`.

**CreditCheckWorker** (`mtg_credit_check`)

Outputs `creditScore`.

**UnderwriteWorker** (`mtg_underwrite`)

Outputs `underwriting`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
