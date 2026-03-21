# Escalation Timer in Java Using Conductor : Request Submission, WAIT with Timeout for Auto-Approval, and Decision Processing

## The Problem

Pending approvals that sit indefinitely block business processes. If an approver is on vacation, overwhelmed, or simply forgets, the request should not languish forever. The workflow must submit the request, wait for a human decision, and if no decision arrives within the configured deadline (autoApproveAfterMs), automatically approve the request so work can proceed. The processing step must know whether the decision came from a human or was auto-approved, since auto-approved items may need additional audit scrutiny. Without timeout-based escalation, stale requests accumulate and SLAs are violated.

## The Solution

**You just write the request-submission and decision-processing workers. Conductor handles the timed wait and the auto-approval when the deadline expires.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

SubmitWorker prepares the request with a deadline, and ProcessWorker handles both human-approved and auto-approved decisions, the timeout-based auto-approval logic is managed by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `et_submit` | Submits the request for approval. validates the request ID, records the auto-approve deadline, and marks it as ready for the WAIT task |
| *WAIT task* | `approval_wait` | Pauses for the approver's decision; an external timer checks the deadline and auto-completes the task with `{ "decision": "approved", "method": "auto" }` if no human acts in time | Built-in Conductor WAIT. no worker needed |
| **ProcessWorker** | `et_process` | Processes the approval decision. records whether it came from a human or auto-approval, flags auto-approved items for audit review, and triggers downstream actions |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
et_submit
 │
 ▼
approval_wait [WAIT]
 │
 ▼
et_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
