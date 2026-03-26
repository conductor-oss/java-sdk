# Insurance Claims

Insurance claims: submit, verify, adjudicate, pay, close

**Input:** `claimId`, `patientId`, `providerId`, `amount`, `procedureCode` | **Timeout:** 60s

## Pipeline

```
clm_submit
    │
clm_verify
    │
clm_adjudicate
    │
clm_pay
    │
clm_close
```

## Workers

**AdjudicateClaimWorker** (`clm_adjudicate`)

```java
double approved = Math.round(amount * coverage / 100.0 * 100.0) / 100.0;
```

**CloseClaimWorker** (`clm_close`)

**PayClaimWorker** (`clm_pay`)

**SubmitClaimWorker** (`clm_submit`)

**VerifyClaimWorker** (`clm_verify`)

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
