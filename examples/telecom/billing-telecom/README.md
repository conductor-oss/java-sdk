# Billing Telecom

Orchestrates billing telecom through a multi-stage Conductor workflow.

**Input:** `customerId`, `billingPeriod` | **Timeout:** 60s

## Pipeline

```
btl_collect_usage
    │
btl_rate
    │
btl_invoice
    │
btl_send
    │
btl_collect_payment
```

## Workers

**CollectPaymentWorker** (`btl_collect_payment`)

Outputs `paymentStatus`, `paidAt`.

**CollectUsageWorker** (`btl_collect_usage`)

Reads `customerId`. Outputs `usageRecords`.

**InvoiceWorker** (`btl_invoice`)

Outputs `invoiceId`, `totalAmount`, `dueDate`.

**RateWorker** (`btl_rate`)

Outputs `ratedCharges`.

**SendWorker** (`btl_send`)

Reads `customerId`. Outputs `sent`, `method`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
