# Commission Calculation

Orchestrates commission calculation through a multi-stage Conductor workflow.

**Input:** `agentId`, `salePrice`, `transactionId` | **Timeout:** 60s

## Pipeline

```
cmc_base
    │
cmc_tiers
    │
cmc_deductions
    │
cmc_finalize
```

## Workers

**BaseCommissionWorker** (`cmc_base`)

Outputs `baseCommission`.

**DeductionsWorker** (`cmc_deductions`)

Outputs `netCommission`.

**FinalizeWorker** (`cmc_finalize`)

Outputs `paymentId`.

**TiersWorker** (`cmc_tiers`)

Outputs `tieredAmount`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
