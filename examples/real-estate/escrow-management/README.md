# Escrow Management

Orchestrates escrow management through a multi-stage Conductor workflow.

**Input:** `buyerId`, `sellerId`, `amount` | **Timeout:** 60s

## Pipeline

```
esc_open
    │
esc_deposit
    │
esc_verify
    │
esc_release
    │
esc_close
```

## Workers

**CloseEscrowWorker** (`esc_close`)

Outputs `status`.

**DepositWorker** (`esc_deposit`)

Outputs `deposited`.

**OpenEscrowWorker** (`esc_open`)

Outputs `escrowId`.

**ReleaseWorker** (`esc_release`)

Outputs `released`.

**VerifyWorker** (`esc_verify`)

Outputs `conditionsMet`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
