# WAIT Task Completion via SDK (TaskClient) in Java Using Conductor : Ticket Initialization, WAIT for Programmatic Resolution, and Ticket Finalization

## Programmatic Workflow Resumption via the Conductor SDK

Instead of using REST APIs directly, the Conductor SDK's TaskClient can complete WAIT tasks programmatically from Java code. This is useful when another Java service or a scheduled job needs to resume a paused workflow. The workflow initializes a ticket, pauses at a WAIT task, and a separate process uses the SDK to complete it. The finalization step closes the ticket.

## The Solution

**You just write the ticket-initialization and finalization workers. Conductor handles the durable pause and the type-safe SDK-based task completion.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

InitWorker opens the support ticket, and FinalizeWorker closes it after resolution, the type-safe SDK-based task completion from a separate Java process is coordinated through Conductor's TaskClient.

| Worker | Task | What It Does |
|---|---|---|
| **InitWorker** | `wsdk_init` | Initializes a support ticket. takes the ticketId from workflow input and sets its status to "open", making the ticket available for resolution |
| *WAIT task* | `WAIT` | Pauses with the ticketId until a separate Java service completes this task using the Conductor SDK's TaskClient. the resolving service calls `taskClient.updateTask(...)` to resume the workflow | Built-in Conductor WAIT, no worker needed |
| **FinalizeWorker** | `wsdk_finalize` | Closes the ticket after the WAIT task is resolved. marks the ticket as complete and returns the final result |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
wsdk_init
 │
 ▼
WAIT [WAIT]
 │
 ▼
wsdk_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
