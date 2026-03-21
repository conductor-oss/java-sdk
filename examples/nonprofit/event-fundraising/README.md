# Event Fundraising in Java with Conductor

## The Problem

Your nonprofit is hosting a gala dinner to raise funds. The events team needs to plan the event by booking a venue and setting capacity, promote it across email, social media, and partner channels to drive registrations, execute the event and track attendance and satisfaction, collect ticket revenue and additional donations, and reconcile the finances to produce a net-raised figure. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the event setup, registration processing, donation collection, and post-event reporting logic. Conductor handles registration retries, donation processing, and event audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Event planning, registration, donation collection, and impact reporting workers each own one aspect of fundraising event execution.

| Worker | Task | What It Does |
|---|---|---|
| **CollectWorker** | `efr_collect` | Calculates ticket revenue from attendees and ticket price, adds additional donations, and returns total raised |
| **ExecuteWorker** | `efr_execute` | Runs the event day, recording actual attendee count, total expenses, and attendee satisfaction score |
| **PlanWorker** | `efr_plan` | Plans the event by assigning a venue and capacity, returning an event ID |
| **PromoteWorker** | `efr_promote` | Promotes the event across email, social media, and partner channels, returning registration count |
| **ReconcileWorker** | `efr_reconcile` | Reconciles revenue against expenses to compute net funds raised and marks the event as reconciled |

### The Workflow

```
efr_plan
 │
 ▼
efr_promote
 │
 ▼
efr_execute
 │
 ▼
efr_collect
 │
 ▼
efr_reconcile

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
