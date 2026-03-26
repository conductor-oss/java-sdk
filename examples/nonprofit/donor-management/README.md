# Donor Management

Orchestrates donor management through a multi-stage Conductor workflow.

**Input:** `donorName`, `donorEmail`, `firstGift` | **Timeout:** 60s

## Pipeline

```
dnr_acquire
    │
dnr_steward
    │
dnr_acknowledge
    │
dnr_retain
    │
dnr_upgrade
```

## Workers

**AcknowledgeWorker** (`dnr_acknowledge`)

Reads `donorName`. Outputs `acknowledged`, `method`.

**AcquireWorker** (`dnr_acquire`)

Reads `donorEmail`, `donorName`. Outputs `donorId`, `acquired`.

**RetainWorker** (`dnr_retain`)

Reads `donorId`. Outputs `level`, `retained`, `nextAction`.

**StewardWorker** (`dnr_steward`)

Reads `donorId`, `firstGift`. Outputs `engagement`.

**UpgradeWorker** (`dnr_upgrade`)

Reads `currentLevel`, `donorId`. Outputs `donor`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
