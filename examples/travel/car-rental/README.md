# Car Rental

Car rental: search, select, book, pickup, return.

**Input:** `travelerId`, `location`, `pickupDate`, `returnDate` | **Timeout:** 60s

## Pipeline

```
crl_search
    │
crl_select
    │
crl_book
    │
crl_pickup
    │
crl_return
```

## Workers

**BookWorker** (`crl_book`)

Reads `travelerId`. Outputs `reservationId`, `confirmationCode`.

**PickupWorker** (`crl_pickup`)

Reads `reservationId`. Outputs `pickedUp`, `mileageStart`.

**ReturnWorker** (`crl_return`)

Reads `reservationId`. Outputs `returned`, `totalCost`, `mileageEnd`.

**SearchWorker** (`crl_search`)

Reads `location`. Outputs `available`.

**SelectWorker** (`crl_select`)

Outputs `selected`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
