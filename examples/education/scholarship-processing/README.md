# Scholarship Processing in Java with Conductor : Application, Evaluation, Ranking, Award, and Notification

## The Problem

You need to process scholarship applications from submission to award. A student applies for a specific scholarship, the financial aid office evaluates their eligibility based on GPA and demonstrated financial need, applicants are ranked against the competition, an award decision is made based on ranking and available funds, and the student is notified whether they received the scholarship and for how much. Awarding without proper evaluation risks compliance violations; ranking without consistent criteria leads to unfair outcomes.

Without orchestration, you'd build a single batch-processing script that pulls applications from a database, scores them inline, sorts by rank, updates award status, and sends notification emails. manually handling ties in ranking, retrying failed email deliveries, and logging every decision to satisfy audit requirements from donors and federal financial aid regulations.

## The Solution

**You just write the application intake, eligibility evaluation, competitive ranking, award decision, and student notification logic. Conductor handles financial analysis retries, award routing, and application audit trails.**

Each scholarship concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (apply, evaluate, rank, award, notify), retrying if the financial aid system is temporarily unavailable, maintaining a complete audit trail of every application's journey from submission to decision, and resuming from the last successful step if the process crashes.

### What You Write: Workers

Application review, financial analysis, award determination, and disbursement workers handle scholarship decisions as a transparent pipeline.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `scp_apply` | Receives the student's scholarship application with GPA and financial information |
| **EvaluateWorker** | `scp_evaluate` | Scores the application based on GPA, financial need, and scholarship-specific criteria |
| **RankWorker** | `scp_rank` | Ranks the applicant against other candidates based on evaluation score |
| **AwardWorker** | `scp_award` | Decides whether to award the scholarship based on rank and available funds |
| **NotifyWorker** | `scp_notify` | Notifies the student of the award decision and amount |

### The Workflow

```
scp_apply
 │
 ▼
scp_evaluate
 │
 ▼
scp_rank
 │
 ▼
scp_award
 │
 ▼
scp_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
