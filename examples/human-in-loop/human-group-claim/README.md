# Group Assignment with Claim Pattern in Java Using Conductor : Ticket Intake, WAIT for Group Claim, and Resolution

## Tasks Can Be Assigned to a Group and Claimed by Any Member

Some tasks do not have a specific assignee. They are published to a group (like a support queue), and any available team member can claim and work on it. The workflow processes intake, pauses at a WAIT task visible to the group, and after someone claims and completes it, the resolution step closes the ticket. If resolution fails, you need to retry it without asking someone to re-claim the task.

## The Solution

**You just write the ticket-intake and resolution workers. Conductor handles the group queue and the durable wait for a team member to claim the task.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

IntakeWorker queues the ticket to a support group, and ResolveWorker closes it after a team member claims it. Neither manages the group queue or tracks who picked up the task.

| Worker | Task | What It Does |
|---|---|---|
| **IntakeWorker** | `hgc_intake` | Processes the ticket intake. validates the ticket ID, assigns it to the specified group queue, and marks it as queued and ready for claim |
| *WAIT task* | `group_assigned_wait` | Publishes the ticket to the assigned group with instructions; pauses until a team member claims it by completing the task with `{ "claimedBy": "agent@company.com" }` via `POST /tasks/{taskId}` | Built-in Conductor WAIT. no worker needed |
| **ResolveWorker** | `hgc_resolve` | Resolves and closes the ticket, recording who claimed and completed it |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
hgc_intake
 │
 ▼
group_assigned_wait [WAIT]
 │
 ▼
hgc_resolve

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
