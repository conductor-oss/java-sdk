# Stripe Integration

Orchestrates stripe integration through a multi-stage Conductor workflow.

**Input:** `email`, `amount`, `currency`, `description` | **Timeout:** 60s

## Pipeline

```
stp_create_customer
    │
stp_payment_intent
    │
stp_charge
    │
stp_send_receipt
```

## Workers

**ChargeWorker** (`stp_charge`): Charges a payment (confirms and captures a payment intent).

```java
this.liveMode = apiKey != null && !apiKey.isBlank();
```

Reads `paymentIntentId`. Outputs `chargeId`, `status`, `capturedAt`.
Returns `FAILED` on validation errors.

**CreateCustomerWorker** (`stp_create_customer`): Creates a Stripe customer.

```java
this.liveMode = apiKey != null && !apiKey.isBlank();
```

Reads `email`. Outputs `customerId`.
Returns `FAILED` on validation errors.

**PaymentIntentWorker** (`stp_payment_intent`): Creates a payment intent.

```java
this.liveMode = apiKey != null && !apiKey.isBlank();
```

Reads `amount`, `currency`, `customerId`, `description`. Outputs `paymentIntentId`, `status`.
Returns `FAILED` on validation errors.

**SendReceiptWorker** (`stp_send_receipt`): Sends a receipt email.

Reads `chargeId`, `email`. Outputs `sent`, `sentAt`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
