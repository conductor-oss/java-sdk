# Salvage Recovery

Orchestrates salvage recovery through a multi-stage Conductor workflow.

**Input:** `claimId`, `vehicleId`, `damageType` | **Timeout:** 60s

## Pipeline

```
slv_assess_damage
    │
slv_salvage
    │
slv_auction
    │
slv_settle
    │
slv_close
```

## Workers

**AssessDamageWorker** (`slv_assess_damage`)

Reads `vehicleId`. Outputs `totalLoss`, `salvageValue`.

**AuctionWorker** (`slv_auction`)

Outputs `proceeds`, `buyer`, `bids`.

**CloseWorker** (`slv_close`)

Reads `claimId`. Outputs `closed`, `closedAt`.

**SalvageWorker** (`slv_salvage`)

Reads `vehicleId`. Outputs `reservePrice`, `lotNumber`.

**SettleWorker** (`slv_settle`)

Reads `claimId`. Outputs `recoveryAmount`, `netRecovery`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
