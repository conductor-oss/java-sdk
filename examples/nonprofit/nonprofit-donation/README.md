# Nonprofit Donation in Java with Conductor

## The Problem

A donor submits a donation through your nonprofit's website. The donation processing team needs to receive and validate the donation details, process the payment and obtain a transaction ID, generate a tax-deductible receipt, send a personalized thank-you message tied to the campaign, and record the completed donation in the donor database. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the donation acceptance, payment processing, receipt generation, and fund allocation logic. Conductor handles payment retries, receipt generation, and donation audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Gift intake, payment processing, receipt generation, and donor acknowledgment workers each handle one step of the donation acceptance flow.

| Worker | Task | What It Does |
|---|---|---|
| **ProcessPaymentWorker** | `don_process_payment` | Processes the donation payment for the specified amount, returning a transaction ID |
| **ReceiptWorker** | `don_receipt` | Generates and sends a tax-deductible receipt to the donor, returning a receipt ID |
| **ReceiveWorker** | `don_receive` | Validates the incoming donation from the donor, recording the amount and assigning a donation ID |
| **RecordWorker** | `don_record` | Records the completed donation in the donor database with donor name, amount, and transaction ID |
| **ThankYouWorker** | `don_thank_you` | Sends a personalized thank-you message to the donor tied to the specific campaign |

### The Workflow

```
don_receive
 │
 ▼
don_process_payment
 │
 ▼
don_receipt
 │
 ▼
don_thank_you
 │
 ▼
don_record

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
