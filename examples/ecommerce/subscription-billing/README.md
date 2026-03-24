# Subscription Billing

Subscription billing: check period, generate invoice, charge, activate next period

**Input:** `subscriptionId`, `customerId`, `plan`, `billingCycle` | **Timeout:** 60s

## Pipeline

```
sub_check_period
    │
sub_generate_invoice
    │
sub_charge
    │
sub_activate
```

## Workers

**ActivateWorker** (`sub_activate`)

Reads `nextPeriodStart`, `subscriptionId`. Outputs `active`, `nextPeriodStart`, `renewedAt`.

**ChargeWorker** (`sub_charge`)

Reads `amount`, `customerId`, `invoiceId`. Outputs `chargeId`, `charged`, `chargedAt`.

**CheckPeriodWorker** (`sub_check_period`)

Reads `billingCycle`, `subscriptionId`. Outputs `periodStart`, `periodEnd`, `dueDate`, `billingCycle`.

**GenerateInvoiceWorker** (`sub_generate_invoice`)

Reads `periodEnd`, `periodStart`, `plan`. Outputs `invoiceId`, `amount`, `lineItems`.

## Tests

**9 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
