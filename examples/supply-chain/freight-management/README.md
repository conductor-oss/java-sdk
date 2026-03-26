# Freight Management

Freight management: book, track, deliver, invoice, and reconcile.

**Input:** `origin`, `destination`, `weight`, `carrier` | **Timeout:** 60s

## Pipeline

```
frm_book
    │
frm_track
    │
frm_deliver
    │
frm_invoice
    │
frm_reconcile
```

## Workers

**BookWorker** (`frm_book`)

```java
double rate = weight * 2.5;
```

Reads `carrier`, `destination`, `origin`, `weight`. Outputs `bookingId`, `rate`.

**DeliverWorker** (`frm_deliver`)

Reads `bookingId`. Outputs `delivered`, `signedBy`.

**InvoiceWorker** (`frm_invoice`)

Reads `rate`. Outputs `invoiceId`, `amount`.

**ReconcileWorker** (`frm_reconcile`)

Reads `bookingId`, `invoiceId`. Outputs `reconciled`, `discrepancy`.

**TrackWorker** (`frm_track`)

Reads `bookingId`. Outputs `status`, `progress`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
