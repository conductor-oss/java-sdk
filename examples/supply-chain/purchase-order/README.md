# Purchase Order

Purchase order lifecycle: create, approve, send to vendor, track, and receive.

**Input:** `vendor`, `items`, `totalAmount` | **Timeout:** 60s

## Pipeline

```
po_create
    │
po_approve
    │
po_send
    │
po_track
    │
po_receive
```

## Workers

**ApproveWorker** (`po_approve`)

```java
boolean approved = totalAmount <= 100000;
```

Reads `poNumber`, `totalAmount`. Outputs `approved`, `approver`.

**CreateWorker** (`po_create`)

Reads `totalAmount`, `vendor`. Outputs `poNumber`.

**ReceiveWorker** (`po_receive`)

Reads `poNumber`, `trackingStatus`. Outputs `received`, `condition`, `matchesPO`.

**SendWorker** (`po_send`)

Reads `poNumber`, `vendor`. Outputs `sent`, `method`.

**TrackWorker** (`po_track`)

Reads `poNumber`. Outputs `status`, `eta`.

## Tests

**11 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
