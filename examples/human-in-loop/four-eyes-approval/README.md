# Four-Eyes Approval in Java Using Conductor: Dual Independent Approvals via Parallel WAIT Tasks in FORK/JOIN

## Some Decisions Require Two Independent Approvals (Four-Eyes Principle)

Regulated industries require the four-eyes principle: no single person can approve a critical action alone. The workflow submits the request, then two independent WAIT tasks run in parallel (via FORK/JOIN), each assigned to a different approver. Both must approve before finalization proceeds. If finalization fails, you need to retry it without asking both approvers to re-approve.

## The Solution

**You just write the request submission and finalization workers. Conductor handles the parallel dual-approval gates and the join.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. Your code handles the decision logic.

### What You Write: Workers

SubmitWorker prepares the request for dual review, and FinalizeWorker records both approvers' decisions, each runs independently within the FORK/JOIN approval structure.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `fep_submit` | Submits the request for dual approval. validates the request ID and marks it as ready for the parallel FORK/JOIN approval gates |
| *FORK/JOIN* | `fork_approvers` | Launches two parallel WAIT tasks. One for each independent approver, and waits until both have responded | Built-in Conductor FORK/JOIN.; no worker needed |
| *WAIT task* | `approver_1` | Pauses for the first approver's independent decision via `POST /tasks/{taskId}` with `{ "approval": true/false }` | Built-in Conductor WAIT.; no worker needed |
| *WAIT task* | `approver_2` | Pauses for the second approver's independent decision, running in parallel with approver 1 | Built-in Conductor WAIT.; no worker needed |
| **FinalizeWorker** | `fep_finalize` | Receives both approvers' decisions and finalizes: proceeds only if both approved, otherwise rejects and records which approver dissented |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments, the workflow structure stays the same.

### The Workflow

```
fep_submit
 │
 ▼
FORK_JOIN
 ├── approver_1
 └── approver_2
 │
 ▼
JOIN (wait for all branches)
fep_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
