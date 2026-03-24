# Restaurant Management

Orchestrates restaurant management through a multi-stage Conductor workflow.

**Input:** `guestName`, `partySize` | **Timeout:** 60s

## Pipeline

```
rst_reservations
    │
rst_seating
    │
rst_order
    │
rst_kitchen
    │
rst_checkout
```

## Workers

**CheckoutWorker** (`rst_checkout`)

Reads `orderId`, `tableId`. Outputs `bill`.

**KitchenWorker** (`rst_kitchen`)

Reads `orderId`. Outputs `prepared`, `cookTime`.

**OrderWorker** (`rst_order`)

Reads `tableId`. Outputs `orderId`, `items`, `total`.

**ReservationsWorker** (`rst_reservations`)

Reads `guestName`, `partySize`. Outputs `reservation`.

**SeatingWorker** (`rst_seating`)

Outputs `tableId`, `section`, `seated`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
