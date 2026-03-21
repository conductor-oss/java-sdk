# Multi-Level Escalation Chain in Java Using Conductor: Request Submission, WAIT for Analyst/Manager/VP Approval, and Finalization

## The Problem

You need an approval process where requests escalate through a chain of authority. Analyst, then manager, then VP. The request starts with the analyst. If the analyst has the authority and context to decide, they approve or reject directly. If the request exceeds their authority or expertise, they escalate to their manager. The manager can likewise approve, reject, or escalate to the VP. The system must track at which level the decision was made and who made it. Without escalation tracking, there is no visibility into whether requests are being resolved at the appropriate level or whether analysts are escalating too frequently.

Without orchestration, you'd build a custom escalation system, the analyst receives a notification, clicks "escalate" in your UI, your backend reassigns the request, sends a new notification to the manager, and repeats. If the system crashes during escalation, the request is unassigned. There is no single view showing how many requests are pending at each level, the average time at each level, or the escalation rate.

## The Solution

**You just write the request submission and escalation finalization workers. Conductor handles the durable hold through the analyst-manager-VP chain.**

The WAIT task is the key pattern here. After submitting the request, the workflow pauses at a single WAIT task. The external escalation logic (analyst -> manager -> VP) occurs outside the workflow, each person in the chain either completes the WAIT task with their decision or hands it to the next level. When someone finally decides, they complete the WAIT task with the decision and the level (respondedAt) at which it was made. The finalize worker then processes the outcome. Conductor takes care of holding the request durably through the entire escalation chain, accepting the final decision with the responder's level, tracking the complete timeline from submission through resolution, and retrying finalization if downstream systems are temporarily unavailable. ### What You Write: Workers

EscSubmitWorker prepares the request for the analyst-manager-VP chain, and EscFinalizeWorker records which level made the final decision, the escalation logic between them lives outside the workers.

| Worker | Task | What It Does |
|---|---|---|
| **EscSubmitWorker** | `esc_submit` | Submits the request for escalation-chain approval: validates the request ID, enriches with context, and marks it as ready for the first level (analyst) |
| *WAIT task* | `esc_approval` | Pauses the workflow until someone in the escalation chain (analyst, manager, or VP) makes a final decision, completing it via `POST /tasks/{taskId}` with the decision and the level at which it was made | Built-in Conductor WAIT.; no worker needed |
| **EscFinalizeWorker** | `esc_finalize` | Finalizes the escalation: records the decision, who made it, and at which level (analyst/manager/VP), then triggers downstream actions |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### The Workflow

```
esc_submit
 │
 ▼
esc_approval [WAIT]
 │
 ▼
esc_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
