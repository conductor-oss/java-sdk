# Change Management in Java with Conductor : Submit, Assess Risk, Approve, Implement

Automates ITIL-style change management using [Conductor](https://github.com/conductor-oss/conductor). This workflow submits a change request with a tracking ID, assesses the risk level (low/medium/high), routes through Change Advisory Board (CAB) approval, and implements the approved change.

## Controlled Changes, Not Cowboy Deploys

An engineer wants to modify the production database connection pool settings. Without a process, they SSH in and change it. With change management, the request is submitted and tracked, risk is assessed (is this a low-risk config change or a high-risk schema migration?), CAB approval is obtained for anything above low risk, and only then is the change implemented. If the approval is denied or the risk assessment flags concerns, the workflow stops cleanly.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the risk assessment and approval logic. Conductor handles the submit-assess-approve-implement pipeline and the full change audit trail.**

`SubmitChangeWorker` creates the change request with description, justification, affected systems, implementation plan, and rollback procedure. `AssessRiskWorker` evaluates the change's risk level based on affected systems, blast radius, change complexity, and historical failure rates for similar changes. `ApproveChangeWorker` routes the change through the approval process. auto-approving low-risk changes or queuing for CAB review. `ImplementChangeWorker` executes the approved change with monitoring and records the outcome. Conductor tracks the full change lifecycle for compliance auditing.

### What You Write: Workers

Four workers implement the change process. Submitting the request, assessing risk, routing through CAB approval, and implementing the change.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveChange** | `cm_approve` | Processes CAB (Change Advisory Board) approval for a change. |
| **AssessRisk** | `cm_assess_risk` | Assesses risk level for a submitted change request. |
| **ImplementChange** | `cm_implement` | Implements the approved change. |
| **SubmitChange** | `cm_submit` | Submits a change request and generates a tracking ID. |

the workflow and rollback logic stay the same.

### The Workflow

```
Input -> ApproveChange -> AssessRisk -> ImplementChange -> SubmitChange -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
