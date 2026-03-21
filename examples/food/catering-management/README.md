# Catering Management in Java with Conductor

Orchestrates a catering order from client inquiry through menu planning, event execution, and invoicing. ## The Problem

You need to manage a catering order from initial inquiry to final invoice. A client reaches out with event details (date, guest count), a customized quote is prepared, the menu is planned based on dietary requirements and budget, the catering event is executed, and the final invoice is sent. Providing a quote without understanding guest count leads to cost overruns; executing without a planned menu results in food shortages or waste.

Without orchestration, you'd manage catering through spreadsheets and email. manually tracking inquiry status, building quotes from templates, coordinating menu planning with kitchen staff, and chasing invoices after the event.

## The Solution

**You just write the client inquiry, menu planning, event execution, and invoicing logic. Conductor handles quote retries, event coordination sequencing, and catering engagement audit trails.**

Each catering concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (inquiry, quote, plan menu, execute, invoice), retrying if an external service fails, tracking every catering order's lifecycle, and resuming from the last step if the process crashes. ### What You Write: Workers

Inquiry intake, menu planning, quoting, execution, and invoicing workers each manage one phase of a catering engagement.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteWorker** | `cat_execute` | Executes the catering event with menu, staff, and logistics coordination |
| **InquiryWorker** | `cat_inquiry` | Receives a catering inquiry with client name and guest count, and captures event requirements (type, dietary needs, service style) |
| **InvoiceWorker** | `cat_invoice` | Generates a final invoice with client name, total cost, and invoice ID, then marks it as sent |
| **PlanMenuWorker** | `cat_plan_menu` | Plans a menu for the given guest count and budget, selecting appetizers, mains, desserts, and beverages |
| **QuoteWorker** | `cat_quote` | Calculates a price quote at $45 per guest based on guest count and returns the total and per-guest rate |

### The Workflow

```
cat_inquiry
 │
 ▼
cat_quote
 │
 ▼
cat_plan_menu
 │
 ▼
cat_execute
 │
 ▼
cat_invoice

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
