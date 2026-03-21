# WAIT Task Timeout Escalation in Java Using Conductor : Request Preparation, WAIT with Deadline, Timeout-Triggered Escalation to Manager, and Normal Response Processing

## WAIT Tasks Should Escalate When No One Responds in Time

If a human does not respond to a WAIT task within a deadline, the workflow should not hang indefinitely. Instead, it should escalate. Notify a manager, auto-approve, or take a default action. The workflow prepares the request, pauses at a WAIT task with a timeout, and a SWITCH routes to the escalation path if the timeout fires or to the normal processing path if the human responds in time.

## The Solution

**You just write the request-preparation, escalation-notification, and response-processing workers. Conductor handles the deadline timeout and the escalation-vs-normal routing.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

WtePrepareWorker sets up the escalation context, WteProcessWorker handles timely responses, and WteEscalateWorker notifies the manager on timeout, the deadline enforcement and escalation-vs-normal routing are managed by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **WtePrepareWorker** | `wte_prepare` | Prepares the request before the WAIT deadline starts. takes the requestId, sets up the escalation context, and returns ready=true |
| *WAIT task* | `wait_for_response` | Pauses for human input with a deadline. if the human responds via `POST /tasks/{taskId}` before timeout, the response flows to normal processing; if the timeout fires, the workflow routes to escalation | Built-in Conductor WAIT, no worker needed |
| **WteProcessWorker** | `wte_process` | Processes the human's response when they reply before the deadline. reads the response from the WAIT task output and returns the processed result |
| **WteEscalateWorker** | `wte_escalate` | Handles escalation when the WAIT task times out. notifies the manager (manager@company.com), flags the request as escalated, and returns escalated=true with the escalation target |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
wte_prepare
 │
 ▼
wait_for_response [WAIT]
 │
 ▼
wte_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
