# Nonprofit Donation

Orchestrates nonprofit donation through a multi-stage Conductor workflow.

**Input:** `donorName`, `amount`, `campaign` | **Timeout:** 60s

## Pipeline

```
don_receive
    │
don_process_payment
    │
don_receipt
    │
don_thank_you
    │
don_record
```

## Workers

**ProcessPaymentWorker** (`don_process_payment`)

Reads `amount`, `donorName`. Outputs `transactionId`, `processed`.

**ReceiptWorker** (`don_receipt`)

Reads `donorName`. Outputs `receiptId`, `taxDeductible`.

**ReceiveWorker** (`don_receive`)

Reads `amount`, `donorName`. Outputs `received`, `donationId`.

**RecordWorker** (`don_record`)

Reads `amount`, `donorName`, `transactionId`. Outputs `donation`.

**ThankYouWorker** (`don_thank_you`)

Reads `campaign`, `donorName`. Outputs `thanked`, `channel`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
