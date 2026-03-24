# In App Purchase

Orchestrates in app purchase through a multi-stage Conductor workflow.

**Input:** `playerId`, `itemId`, `price` | **Timeout:** 60s

## Pipeline

```
iap_select_item
    │
iap_verify
    │
iap_charge
    │
iap_deliver
    │
iap_receipt
```

## Workers

**ChargeWorker** (`iap_charge`)

Reads `playerId`, `price`. Outputs `transactionId`, `charged`, `amount`.

**DeliverWorker** (`iap_deliver`)

Reads `itemId`, `playerId`. Outputs `delivered`, `inventoryUpdated`.

**ReceiptWorker** (`iap_receipt`)

Reads `playerId`, `transactionId`. Outputs `receipt`.

**SelectItemWorker** (`iap_select_item`)

Reads `itemId`, `playerId`. Outputs `item`.

**VerifyWorker** (`iap_verify`)

Reads `playerId`. Outputs `eligible`, `ageVerified`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
