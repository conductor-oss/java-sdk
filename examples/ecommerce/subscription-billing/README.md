# Subscription Billing in Java Using Conductor : Check Period, Generate Invoice, Charge, Activate

## Subscription Billing Must Be Reliable and Retry-Safe

A customer on the $29/month Pro plan has their billing date today. The system must verify the billing period (not already billed, subscription is active, no pending cancellation), generate an invoice (pro-rated amounts for mid-cycle changes, usage-based add-ons, applicable taxes), charge the payment method (with retry logic for declined cards), and activate the next period (extending access, resetting usage counters).

Failed charges are the biggest challenge. a card declines due to insufficient funds, but the customer adds money the next day. The billing system needs grace period handling: retry the charge daily for 3-7 days before suspending the subscription. If the charge succeeds on retry, the subscription should activate seamlessly as if nothing happened. And every billing event needs an audit trail for revenue recognition and dispute resolution.

## The Solution

**You just write the period verification, invoice generation, payment charging, and subscription activation logic. Conductor handles payment retries, billing cycle sequencing, and subscription audit trails.**

`CheckPeriodWorker` verifies the billing period. confirming the subscription is active, checking for pending cancellations or plan changes, and determining the billing amount including any proration. `GenerateInvoiceWorker` creates an itemized invoice with the plan cost, any usage-based charges, applicable taxes, and credits. `ChargeWorker` processes the payment against the customer's payment method, handling declined cards with configurable retry logic. `ActivateWorker` extends the subscription to the next period, resets usage counters, and confirms the billing cycle. Conductor sequences these steps, retries failed charges with backoff, and records every billing event.

### What You Write: Workers

Billing workers for metering, invoice generation, payment collection, and renewal each handle one billing cycle phase autonomously.

| Worker | Task | What It Does |
|---|---|---|
| **CheckPeriodWorker** | `sub_check_period` | Determines the current billing period start/end dates based on the subscription's billing cycle |
| **GenerateInvoiceWorker** | `sub_generate_invoice` | Creates an itemized invoice for the subscriber's plan and billing period |
| **ChargeWorker** | `sub_charge` | Charges the customer's payment method for the invoiced amount |
| **ActivateWorker** | `sub_activate` | Activates the subscription for the next billing period after successful payment |

### The Workflow

```
sub_check_period
 │
 ▼
sub_generate_invoice
 │
 ▼
sub_charge
 │
 ▼
sub_activate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
