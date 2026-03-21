# Contract Lifecycle Management in Java with Conductor: Drafting, Legal Review, Approval, Execution, and Renewal

Your $250K logistics contract auto-renewed last month at a 20% rate increase. The opt-out window closed 60 days before expiry, but nobody tracked it. the renewal date was buried in a PDF on a shared drive, and the calendar reminder was set for the week of expiry, not the week of the opt-out deadline. That's $50K/year you'll pay for the next 12 months because a date wasn't surfaced. Meanwhile, three other contracts are sitting in legal review right now: one has been there for six weeks because legal didn't know it was waiting, and the other two were approved but never executed because the DocuSign request went to a generic inbox. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full contract lifecycle, drafting, legal review, approval, execution, and renewal scheduling, so deadlines are enforced and contracts don't silently lapse or auto-renew.

## The Problem

You need to manage supplier contracts from creation to renewal. The procurement team drafts a service agreement with terms and pricing, legal reviews for risk and compliance with company policies, finance approves based on budget authority, the contract is executed with digital signatures, and renewal needs to be triggered before the 12-month term expires. If the legal review identifies issues, the contract must go back to drafting; but today that happens over email with no version control.

Without orchestration, contracts sit in shared drives with no visibility into their status. Legal review takes weeks because nobody knows a contract is waiting for them. Approved contracts are executed but renewal dates go untracked, causing service interruptions when contracts lapse. When the CFO asks how many contracts above $100K are pending approval, nobody can answer without manually checking email threads.

## The Solution

**You just write the contract lifecycle workers. Drafting, legal review, approval routing, execution, and renewal scheduling. Conductor handles sequencing, retry logic, version tracking, and complete audit trails for contract analytics.**

Each stage of the contract lifecycle is a simple, independent worker, a plain Java class that does one thing. Conductor sequences them so drafts are completed before legal review begins, review feedback gates approval, approval gates execution, and renewal is automatically scheduled based on the contract term. If the approval worker fails to reach the signatory, Conductor retries without re-triggering legal review. Every draft version, review comment, approval decision, and execution timestamp is recorded for audit and contract analytics.

### What You Write: Workers

Five workers cover the contract lifecycle: DraftWorker creates agreements, ReviewWorker handles legal review, ApproveWorker obtains signatory approval, ExecuteWorker captures digital signatures, and RenewWorker schedules renewals.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `clf_approve` | Obtains budget and authority approval based on contract value and review notes. |
| **DraftWorker** | `clf_draft` | Creates a contract draft with vendor, type, value, and term details. |
| **ExecuteWorker** | `clf_execute` | Executes the approved contract with digital signatures. |
| **RenewWorker** | `clf_renew` | Schedules contract renewal based on the term length and expiry date. |
| **ReviewWorker** | `clf_review` | Routes the draft through legal review for risk and compliance assessment. |

### The Workflow

```
clf_draft
 │
 ▼
clf_review
 │
 ▼
clf_approve
 │
 ▼
clf_execute
 │
 ▼
clf_renew

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
