# Legal Billing in Java with Conductor

## The Problem

The billing period has closed. You need to collect time entries from attorneys across matters (e.g., 4.5 hours of contract review by J. Smith, 2.0 hours of research by A. Jones), review them for billing guideline compliance, generate an invoice with the correct total ($3,250.00), send it to the client, and track payment until collected. Manual billing processes lead to write-offs from missed time entries, rejected invoices from guideline violations, and delayed collections.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the time entry collection, rate application, invoice generation, and payment processing logic. Conductor handles time entry retries, invoice generation, and billing audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Time entry collection, rate application, invoice generation, and payment tracking workers each manage one phase of legal fee accounting.

| Worker | Task | What It Does |
|---|---|---|
| **TrackTimeWorker** | `lgb_track_time` | Collects time entries for the matter from all attorneys (e.g., J. Smith: 4.5 hrs contract review, A. Jones: 2.0 hrs research), totaling 6.5 hours |
| **ReviewWorker** | `lgb_review` | Reviews time entries against billing guidelines, approving 6.5 hours with zero adjustments and flagging any non-compliant entries |
| **GenerateWorker** | `lgb_generate` | Generates an invoice (INV-{timestamp}) for the client with a total amount of $3,250.00 USD based on approved time entries |
| **SendWorker** | `lgb_send` | Delivers the invoice to the client via their preferred channel and records the sent timestamp |
| **CollectWorker** | `lgb_collect` | Tracks payment status for the invoice, recording the payment date and marking the invoice as "paid" upon receipt |

### The Workflow

```
lgb_track_time
 │
 ▼
lgb_review
 │
 ▼
lgb_generate
 │
 ▼
lgb_send
 │
 ▼
lgb_collect

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
