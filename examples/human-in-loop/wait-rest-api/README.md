# WAIT Task Completion via REST API in Java Using Conductor : Prepare, WAIT for External HTTP Callback, SWITCH on Decision, and Route to Approved/Rejected Handlers

## External Systems Need to Resume Workflows via REST API

Sometimes the signal to continue a workflow comes from an external system, a webhook, a third-party service callback, or an admin tool. The WAIT task pauses the workflow, and a REST API call completes it with the decision payload. The workflow prepares the request, pauses, then handles the decision when the external system calls back. If decision handling fails, you need to retry it without waiting for another external callback.

## The Solution

**You just write the request-preparation and decision-handling workers. Conductor handles the REST API callback endpoint and the durable pause for the external system.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

PrepareWorker sets up the callback context, and HandleDecisionWorker processes the approval outcome, the REST API endpoint that external systems call to resume the workflow is provided by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareWorker** | `wapi_prepare` | Prepares the workflow for the WAIT approval gate. sets up the context and returns ready=true to indicate the workflow is ready to receive an external callback |
| *WAIT task* | `WAIT` | Pauses the workflow until an external system sends a REST API call (`POST /tasks/{taskId}`) with a decision payload containing the approval outcome | Built-in Conductor WAIT. no worker needed |
| *SWITCH* | `decision_switch` | Routes based on the decision from the WAIT task output: "rejected" goes to a rejection handler, default (approved) goes to an approval handler | Built-in Conductor SWITCH. no worker needed |
| **HandleDecisionWorker** | `wapi_handle_decision` | Processes the approval outcome. reads the decision field from input and returns result="handled-{decision}" to confirm the decision was processed (used by both the approved and rejected SWITCH branches) |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
wapi_prepare
 │
 ▼
WAIT [WAIT]
 │
 ▼
SWITCH (decision_switch_ref)
 ├── rejected: wapi_handle_decision
 └── default: wapi_handle_decision

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
