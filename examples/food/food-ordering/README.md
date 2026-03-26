# Food Ordering

Orchestrates food ordering through a multi-stage Conductor workflow.

**Input:** `customerId`, `restaurantId` | **Timeout:** 60s

## Pipeline

```
fod_browse
    │
fod_order
    │
fod_pay
    │
fod_prepare
    │
fod_deliver
```

## Workers

**BrowseWorker** (`fod_browse`)

Reads `restaurantId`. Outputs `selectedItems`.

**DeliverWorker** (`fod_deliver`)

Reads `orderId`. Outputs `delivery`.

**OrderWorker** (`fod_order`)

Reads `customerId`. Outputs `orderId`, `total`, `items`.

**PayWorker** (`fod_pay`)

Reads `orderId`, `total`. Outputs `paid`, `transactionId`.

**PrepareWorker** (`fod_prepare`)

Reads `orderId`. Outputs `prepTime`, `ready`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
