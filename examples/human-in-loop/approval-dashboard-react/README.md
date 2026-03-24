# React Approval Dashboard in Java Using Conductor : Task Processing and Priority-Based Pending Approval via WAIT Task

## The Problem

You need a React-based approval dashboard where approvers can see their pending tasks, filter by priority (high, medium, low), and act on them. Each request comes in with a title describing what needs approval, a priority level, and the assigned approver. The system must process the request and then wait for the human approver to make a decision. which could take minutes or days depending on the priority. The dashboard needs to query for all pending tasks assigned to a specific person and display them sorted by priority, with real-time status updates as approvals are completed.

Without orchestration, you'd build a React frontend backed by a custom API server that manages a pending-tasks table, polls for status changes, and tracks approval timing. If the backend restarts while tasks are pending, you'd need to rebuild the approval queue from the database. There is no built-in way to see which tasks are pending for which assignee, how long each has been waiting, or to enforce SLA timeouts on high-priority items.

## The Solution

**You just write the task-processing worker. Conductor handles the priority-based approval queue and the searchable task API.**

The WAIT task is the key pattern here. After processing the request, the workflow pauses at the WAIT task. Conductor holds the state with the title, priority, and assignee metadata until the React dashboard completes the task via the API. Conductor takes care of holding pending approvals durably, providing a searchable task API that the React dashboard queries by assignee and status, accepting the approval decision when the assignee acts, and tracking the complete timeline from request to approval.

### What You Write: Workers

DashTaskWorker processes requests with title, priority, and assignee metadata. It has no knowledge of the React dashboard or how pending approvals are queried and filtered.

| Worker | Task | What It Does |
|---|---|---|
| **DashTaskWorker** | `dash_task` | Processes the incoming approval request. validates the title, priority, and assignee, and marks it as ready for human review in the dashboard |
| *WAIT task* | `pending_approval` | Pauses the workflow with title, priority, and assignee metadata until the React dashboard calls `POST /tasks/{taskId}` with the approver's decision | Built-in Conductor WAIT. no worker needed |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
dash_task
 │
 ▼
pending_approval [WAIT]

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
