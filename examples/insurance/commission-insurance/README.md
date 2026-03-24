# Commission Insurance

Orchestrates commission insurance through a multi-stage Conductor workflow.

**Input:** `agentId`, `policyId`, `premiumAmount` | **Timeout:** 60s

## Pipeline

```
cin_calculate
    │
cin_validate
    │
cin_deduct_advances
    │
cin_pay
    │
cin_report
```

## Workers

**CalculateWorker** (`cin_calculate`)

Outputs `commission`, `rate`.

**DeductAdvancesWorker** (`cin_deduct_advances`)

Outputs `netCommission`, `advanceDeducted`.

**PayWorker** (`cin_pay`)

Reads `agentId`. Outputs `paid`, `paymentId`.

**ReportWorker** (`cin_report`)

Reads `agentId`. Outputs `reported`.

**ValidateWorker** (`cin_validate`)

Reads `agentId`. Outputs `valid`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
