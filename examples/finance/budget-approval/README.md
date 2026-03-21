# Budget Approval in Java with Conductor

Budget approval with SWITCH for approve/revise/reject decisions. ## The Problem

You need to route budget requests through an approval workflow. A department submits a budget request with amount and justification, a reviewer evaluates it, and the outcome is one of three paths: approve (release funds), revise (send back with feedback), or reject (deny with explanation). The routing decision depends on the reviewer's assessment of the amount, justification quality, and department priority.

Without orchestration, you'd build an approval service with if/else routing for approve/revise/reject, manually tracking request state through email chains, handling escalation when a reviewer is unavailable, and logging every decision to satisfy audit requirements for budget governance.

## The Solution

**You just write the budget workers. Request submission, review, and three-way routing for approve, revise, or reject decisions. Conductor handles three-way SWITCH routing for approve, revise, and reject decisions, automatic retries, and a complete audit trail for budget governance.**

Each approval concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of submitting the request, routing via a SWITCH task to the correct outcome (approve, revise, reject), retrying if the review system is unavailable, and tracking every budget request's lifecycle with full audit trail. ### What You Write: Workers

Six workers manage the approval lifecycle: SubmitBudgetWorker captures the request, ReviewBudgetWorker evaluates it, and SWITCH routes to ApproveBudgetWorker, ReviseBudgetWorker, or RejectBudgetWorker based on the review, with AllocateFundsWorker releasing approved funds.

| Worker | Task | What It Does |
|---|---|---|
| **AllocateFundsWorker** | `bgt_allocate_funds` | Allocates funds |
| **ApproveBudgetWorker** | `bgt_approve_budget` | Approves the budget |
| **RejectBudgetWorker** | `bgt_reject_budget` | Rejects the budget |
| **ReviewBudgetWorker** | `bgt_review_budget` | Reviews the budget |
| **ReviseBudgetWorker** | `bgt_revise_budget` | Revise Budget. Computes and returns revised, revised amount, revision id |
| **SubmitBudgetWorker** | `bgt_submit_budget` | Handles submit budget |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
bgt_submit_budget
 │
 ▼
bgt_review_budget
 │
 ▼
SWITCH (bgt_switch_ref)
 ├── approve: bgt_approve_budget
 ├── revise: bgt_revise_budget
 ├── reject: bgt_reject_budget
 │
 ▼
bgt_allocate_funds

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
