# Stripe Integration in Java Using Conductor

## Processing Payments Through Stripe

Collecting a payment through Stripe involves a strict sequence: create a customer record (or look up an existing one), create a payment intent that captures the amount and currency, confirm the charge against the payment intent, and send a receipt to the customer. Each step depends on the previous one. you cannot create a payment intent without a customer ID, and you cannot charge without a payment intent ID. If the charge succeeds but the receipt fails, you need visibility into exactly what happened.

Without orchestration, you would chain Stripe API calls manually, pass customer IDs and payment intent IDs between steps, and handle idempotency and partial failures (like a charge that succeeds but a receipt that fails to send). Conductor sequences the pipeline and routes customer IDs, payment intent IDs, and charge IDs between workers automatically.

## The Solution

**You just write the payment workers. Customer creation, payment intent setup, charge confirmation, and receipt delivery. Conductor handles customer-to-receipt sequencing, Stripe API retries, and idempotent payment intent routing for partial failure recovery.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers process payments: CreateCustomerWorker registers the payer, PaymentIntentWorker sets up the charge amount, ChargeWorker confirms the payment, and SendReceiptWorker delivers the receipt email.

| Worker | Task | What It Does |
|---|---|---|
| **CreateCustomerWorker** | `stp_create_customer` | Creates a Stripe customer. |
| **PaymentIntentWorker** | `stp_payment_intent` | Creates a payment intent. |
| **ChargeWorker** | `stp_charge` | Confirms and captures a payment intent. |
| **SendReceiptWorker** | `stp_send_receipt` | Sends a receipt email. |

The workers auto-detect Stripe credentials at startup. When `STRIPE_API_KEY` is set, CreateCustomerWorker, PaymentIntentWorker, and ChargeWorker use the real Stripe REST API (via `java.net.http`) to create customers, payment intents, and confirm charges. Without the key, they fall back to demo mode with realistic output shapes so the workflow runs end-to-end without a Stripe account.

### The Workflow

```
stp_create_customer
 │
 ▼
stp_payment_intent
 │
 ▼
stp_charge
 │
 ▼
stp_send_receipt

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
