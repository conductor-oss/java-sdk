# Next.js Approval Dashboard in Java Using Conductor : Request Processing and Human Approval via WAIT Task

## The Problem

You need a web-based approval dashboard where managers can see all pending requests and approve or reject them with a single click. Requests come in with a type (expense, purchase, time-off), title, dollar amount, and requester name. The system must validate and process each request, then pause and wait. potentially for hours or days, until a human approver makes a decision. The dashboard needs to show all pending, approved, and rejected requests in real time. Without a built-in WAIT mechanism, you'd poll a database for the approver's decision, building your own state management, timeout handling, and dashboard query logic.

Without orchestration, you'd build a full-stack app where the backend writes requests to a database, the frontend polls for pending items, and a separate cron job checks for decisions and advances the workflow. If the server restarts while waiting for approval, you'd need to rebuild state from the database. There is no built-in way to track how long each request has been pending, which approver acted, or when the approval happened.

## The Solution

**You just write the request-processing worker. Conductor handles the pending-approval queue and the dashboard API.**

The WAIT task is the key pattern here. After processing the request, the workflow pauses at the WAIT task. Conductor holds the state durably until the Next.js dashboard calls the Conductor API to complete it with the approver's decision. Conductor takes care of holding the pending request for hours or days, accepting the approval decision (approved/rejected and approver identity) via a simple API call from the dashboard, tracking the complete lifecycle from submission through approval, and providing the API that the Next.js dashboard queries to list all pending, approved, and rejected requests.

### What You Write: Workers

NxtProcessWorker validates incoming requests with type, title, and amount. It has no awareness of the Next.js dashboard or the WAIT task that powers the approval queue.

| Worker | Task | What It Does |
|---|---|---|
| **NxtProcessWorker** | `nxt_process` | Validates and processes the incoming approval request. checks the type, title, amount, and requester, and marks it as ready for human review |
| *WAIT task* | `nxt_approval` | Pauses the workflow until the Next.js dashboard sends an approval decision via `POST /tasks/{taskId}` with the approver's identity and approved/rejected status | Built-in Conductor WAIT. no worker needed |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
nxt_process
 │
 ▼
nxt_approval [WAIT]

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
