# Multi-Level Approval Chain in Java Using Conductor : Manager, Director, VP Sequential WAIT/SWITCH with Early Rejection Termination

## High-Value Requests Must Pass Through Manager, Director, and VP Approval

Some requests require sequential approval from multiple levels of management. First a Manager, then a Director, then a VP. Each level uses a WAIT task for human input, followed by a SWITCH that checks the decision. If any level rejects, the workflow terminates immediately. If all three approve, the request is finalized. If finalization fails after VP approval, you need to retry it without asking all three approvers to re-approve.

## The Solution

**You just write the request-submission and finalization workers. Conductor handles the sequential Manager-Director-VP approval chain and the early rejection short-circuit.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

SubmitWorker identifies the Manager, Director, and VP in the chain, and FinalizeWorker records the complete approval history, the sequential WAIT/SWITCH gates at each level are orchestrated by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `mla_submit` | Submits the request for multi-level approval. validates the request and identifies the Manager, Director, and VP in the approval chain |
| *WAIT + SWITCH* | Manager level | WAIT pauses for the Manager's decision; SWITCH checks if approved (advance to Director) or rejected (terminate workflow) | Built-in Conductor WAIT + SWITCH. no worker needed |
| *WAIT + SWITCH* | Director level | WAIT pauses for the Director's decision; SWITCH checks if approved (advance to VP) or rejected (terminate workflow) | Built-in Conductor WAIT + SWITCH. no worker needed |
| *WAIT + SWITCH* | VP level | WAIT pauses for the VP's decision; SWITCH checks if approved (proceed to finalization) or rejected (terminate workflow) | Built-in Conductor WAIT + SWITCH. no worker needed |
| **FinalizeWorker** | `mla_finalize` | Finalizes the request after all three levels approve. records the complete approval chain and triggers downstream fulfillment |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
mla_submit
 │
 ▼
wait_manager_approval [WAIT]
 │
 ▼
SWITCH (check_manager_decision)
 ├── false: terminate_manager_rejected
 └── default: wait_director_approval -> check_director_decision

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
