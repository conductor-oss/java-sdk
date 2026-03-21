# WAIT Task Basics in Java Using Conductor : Pre-WAIT Preparation, Pause for External Approval Signal, and Post-WAIT Processing with Approval Payload

## Workflows Need to Pause and Wait for External Signals

The WAIT task is the fundamental building block for human-in-the-loop workflows. It pauses execution until an external signal (REST API call, SDK call, or UI interaction) completes the task with a payload. The workflow prepares data before the pause, waits, then processes the data provided when the WAIT task is completed. This example demonstrates the core WAIT task mechanics.

## The Solution

**You just write the preparation and post-approval processing workers. Conductor handles the external signal wait and the data flow through the WAIT task.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

WaitBeforeWorker validates the requestId and prepares context, and WaitAfterWorker processes the approval payload, the data flow through the WAIT task's output (like the approval field) is wired automatically by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **WaitBeforeWorker** | `wait_before` | Prepares the workflow before the pause. takes the requestId from workflow input and returns prepared=true, setting up context for the WAIT task |
| *WAIT task* | `wait_for_approval` | Pauses the workflow until an external signal completes it via `POST /tasks/{taskId}` with an approval payload. the output (including the approval field) becomes available to the next task | Built-in Conductor WAIT, no worker needed |
| **WaitAfterWorker** | `wait_after` | Processes the approval response. receives the requestId from workflow input and the approval value from the WAIT task's output, then returns the final result |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
wait_before
 │
 ▼
wait_for_approval [WAIT]
 │
 ▼
wait_after

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
