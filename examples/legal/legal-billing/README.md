# Legal Billing

Orchestrates legal billing through a multi-stage Conductor workflow.

**Input:** `clientId`, `matterId`, `billingPeriod` | **Timeout:** 60s

## Pipeline

```
lgb_track_time
    │
lgb_review
    │
lgb_generate
    │
lgb_send
    │
lgb_collect
```

## Workers

**CollectWorker** (`lgb_collect`)

Reads `invoiceId`. Outputs `paymentStatus`, `paymentDate`.

**GenerateWorker** (`lgb_generate`)

Reads `clientId`. Outputs `invoiceId`, `totalAmount`, `currency`.

**ReviewWorker** (`lgb_review`)

Reads `timeEntries`. Outputs `approvedEntries`, `approvedHours`, `adjustments`.

**SendWorker** (`lgb_send`)

Reads `clientId`, `invoiceId`. Outputs `sent`, `sentAt`.

**TrackTimeWorker** (`lgb_track_time`)

Reads `matterId`. Outputs `timeEntries`, `totalHours`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
