# Billing Telecom in Java Using Conductor

## Why Telecom Billing Needs Orchestration

Running a billing cycle requires a strict pipeline where each step depends on the previous one. You collect all usage records (CDRs, IPDRs) for the customer's billing period. You rate each record by applying the correct tariff based on the customer's plan, time of day, destination, and any bundled allowances. You generate an invoice that itemizes the rated charges and calculates the total. You send the invoice to the customer via their preferred channel. Finally, you collect payment by charging the customer's payment method on file.

If rating fails partway through, you need to know which records were already rated so you don't double-charge. If the invoice is generated but delivery fails, you have a valid invoice the customer never sees and payment never comes. Without orchestration, you'd build a monolithic billing script that mixes CDR collection, tariff lookups, invoice generation, and payment processing. making it impossible to swap rating engines, test invoice formatting independently, or audit which usage records drove which charges.

## The Solution

**You just write the usage collection, tariff rating, invoice generation, delivery, and payment processing logic. Conductor handles usage collection retries, invoice generation, and billing audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Usage collection, charge calculation, invoice generation, and payment processing workers each handle one stage of the telecom billing cycle.

| Worker | Task | What It Does |
|---|---|---|
| **CollectPaymentWorker** | `btl_collect_payment` | Collects payment for the invoice amount by charging the customer's payment method on file. |
| **CollectUsageWorker** | `btl_collect_usage` | Collects all usage records (voice CDRs, data IPDRs, SMS logs) for a customer's billing period. |
| **InvoiceWorker** | `btl_invoice` | Generates an itemized invoice from the rated charges and calculates the total amount due. |
| **RateWorker** | `btl_rate` | Rates each usage record against the customer's plan tariffs, applying time-of-day and destination rules. |
| **SendWorker** | `btl_send` | Delivers the invoice to the customer via their preferred channel (email, postal, in-app). |

### The Workflow

```
btl_collect_usage
 │
 ▼
btl_rate
 │
 ▼
btl_invoice
 │
 ▼
btl_send
 │
 ▼
btl_collect_payment

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
