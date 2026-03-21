# Payment Processing in Java Using Conductor: Validate, Authorize, Capture, Receipt, Reconcile

A customer pays $259.97 for their order. The payment gateway charges the card successfully, but the confirmation response times out on the network hop back. Your system assumes the charge failed and retries. Now the customer is double-charged, their bank shows two pending holds, and they file a chargeback before your support team even sees the ticket. The second charge eventually settles, the chargeback reverses the first, and your reconciliation report is off by $259.97 for a month until someone manually traces the duplicate. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full payment lifecycle as independent workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Payments Have a Two-Phase Lifecycle: Authorize Then Capture

A $150 payment is not a single operation: it's a lifecycle. First, validate the payment method (card not expired, billing address matches, card not blacklisted). Then authorize (put a hold on the customer's funds without actually charging). Then capture (convert the hold into a real charge, typically done when the order ships). Then generate a receipt for the customer. Finally, reconcile the transaction with the merchant's bank account to ensure the funds actually arrive.

Authorize-then-capture is standard because you don't want to charge until you're sure you can fulfill. If fulfillment fails, you release the authorization instead of processing a refund. If capture fails after authorization, you need to retry capture. . Not re-authorize (which would create a second hold). The reconciliation step catches discrepancies between expected and actual settlement amounts.

## The Solution

**You just write the payment validation, authorization, capture, receipt, and reconciliation logic. Conductor handles authorization retries, settlement sequencing, and transaction audit trails for every payment.**

`ValidateWorker` checks the payment method. Card expiration, billing address verification (AVS), CVV match, and fraud screening. `AuthorizeWorker` places a hold on the customer's funds for the order amount without charging. `CaptureWorker` converts the authorization into a charge, transferring the funds. `ReceiptWorker` generates an itemized receipt with transaction ID, payment method details, and tax breakdown. `ReconcileWorker` matches the captured amount against the merchant settlement to detect discrepancies. Conductor sequences these five stages, ensures idempotent retries for capture, and records the complete payment lifecycle for financial audit.

### What You Write: Workers

Payment workers isolate authorization, capture, settlement, and notification into separate steps, so retry logic targets only the failed transaction phase.

| Worker | Task | What It Does |
|---|---|---|
| **ValidatePaymentWorker** | `pay_validate` | Validates payment method, amount, currency, and computes fraud score |
| **AuthorizePaymentWorker** | `pay_authorize` | Creates a Stripe PaymentIntent with manual capture (authorize only) |
| **CapturePaymentWorker** | `pay_capture` | Captures a previously authorized PaymentIntent |
| **ReceiptWorker** | `pay_receipt` | Generates a receipt with SHA-256 ID, retrieves charge details from Stripe |
| **ReconcileWorker** | `pay_reconcile` | Reconciles captured amount against Stripe, computes fees and net amount |

All workers run in mock mode by default when `STRIPE_API_KEY` is not set, producing realistic deterministic output. Set `STRIPE_API_KEY=sk_test_...` to use the real Stripe API. The workflow stays the same either way.

### The Workflow

```
pay_validate
 │
 ▼
pay_authorize
 │
 ▼
pay_capture
 │
 ▼
pay_receipt
 │
 ▼
pay_reconcile

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
