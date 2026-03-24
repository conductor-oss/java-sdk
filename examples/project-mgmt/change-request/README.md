# Change Request Management in Java with Conductor : Submit, Impact Assessment, Approval, Implementation, and Verification

## The Problem

You need to manage change requests across your project. When someone submits a change. "swap the payment provider," "extend the timeline by two weeks," "add a new integration requirement", the request must go through a controlled process: log the change formally, assess its impact on scope, timeline, and budget, get approval from the change control board, implement the approved change, and verify the result. Each step depends on the previous one, and you need a complete audit trail for compliance.

Without orchestration, change management devolves into email threads and spreadsheets. Impact assessments get lost, approvals happen out of order, implementations proceed without sign-off, and nobody can reconstruct the timeline when an audit asks "who approved this scope change and when?" Building this as a monolithic script means any failure in the approval step silently skips verification.

## The Solution

**You just write the change submission, impact assessment, approval gating, implementation, and verification logic. Conductor handles impact analysis retries, approval routing, and change control audit trails.**

Each step in the change request lifecycle is a simple, independent worker. one submits and logs the request, one assesses impact, one handles the approval gate, one implements the change, one verifies the outcome. Conductor takes care of executing them in strict sequence, ensuring no step is skipped, retrying if an external system is temporarily down, and maintaining a complete execution history that serves as your audit trail.

### What You Write: Workers

Request intake, impact analysis, approval routing, and implementation tracking workers each manage one stage of the change control process.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `chr_submit` | Logs the change request with a unique ID, captures description, requester, and affected areas |
| **AssessImpactWorker** | `chr_assess_impact` | Evaluates the change's impact on scope, timeline, budget, and resource allocation |
| **ApproveWorker** | `chr_approve` | Processes the approval decision based on impact assessment (approve, reject, or request revision) |
| **ImplementWorker** | `chr_implement` | Executes the approved change. updates project plans, reassigns resources, adjusts milestones |
| **VerifyWorker** | `chr_verify` | Confirms the change was implemented correctly and project artifacts are consistent |

### The Workflow

```
chr_submit
 │
 ▼
chr_assess_impact
 │
 ▼
chr_approve
 │
 ▼
chr_implement
 │
 ▼
chr_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
