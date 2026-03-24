# Roaming Management

Orchestrates roaming management through a multi-stage Conductor workflow.

**Input:** `subscriberId`, `homeNetwork`, `visitedNetwork` | **Timeout:** 60s

## Pipeline

```
rmg_detect_roaming
    │
rmg_validate_agreement
    │
rmg_rate
    │
rmg_bill
    │
rmg_settle
```

## Workers

**BillWorker** (`rmg_bill`)

Reads `subscriberId`. Outputs `billed`, `invoiceId`.

**DetectRoamingWorker** (`rmg_detect_roaming`)

Reads `subscriberId`, `visitedNetwork`. Outputs `usage`, `country`.

**RateWorker** (`rmg_rate`)

Outputs `charges`, `interCarrierAmount`.

**SettleWorker** (`rmg_settle`)

Reads `homeNetwork`, `visitedNetwork`. Outputs `settled`, `settlementId`.

**ValidateAgreementWorker** (`rmg_validate_agreement`)

Reads `homeNetwork`, `visitedNetwork`. Outputs `agreement`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
