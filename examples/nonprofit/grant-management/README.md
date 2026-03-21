# Grant Management in Java with Conductor

## The Problem

Your nonprofit is applying for a foundation grant to fund a community program. The grants team needs to submit the application with the organization name and requested amount, have the grant committee review and score the application, approve the grant based on the review score, disburse the funds to the organization, and file a grant usage report documenting expenditures and outcomes. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the grant application, budget tracking, milestone reporting, and compliance documentation logic. Conductor handles submission retries, compliance tracking, and grant lifecycle audit trails.**

Each worker handles one nonprofit operation. Conductor manages the donation pipeline, campaign sequencing, receipt generation, and reporting.

### What You Write: Workers

Application preparation, submission, compliance tracking, and reporting workers each manage one stage of the grant lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `gmt_apply` | Submits the grant application from the organization for the specified program, returning an application ID and submission date |
| **ApproveWorker** | `gmt_approve` | Approves the grant based on the review score, confirming the approved funding amount |
| **FundWorker** | `gmt_fund` | Disburses the approved amount to the organization, assigning a grant ID and recording the disbursement date |
| **ReportWorker** | `gmt_report` | Files the grant usage report linking the grant ID, organization, and funding status |
| **ReviewWorker** | `gmt_review` | Reviews the application for the requested amount, assigning a score and recommendation from the grant committee |

### The Workflow

```
gmt_apply
 │
 ▼
gmt_review
 │
 ▼
gmt_approve
 │
 ▼
gmt_fund
 │
 ▼
gmt_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
