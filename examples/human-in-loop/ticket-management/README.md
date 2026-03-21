# Ticket Management in Java with Conductor

## Managing Tickets from Creation to Closure

When a user reports an issue, the ticket needs to be created, categorized by type and priority, assigned to the right agent, worked on until resolved, and formally closed. Dropping any step means tickets get lost, misrouted, or left in limbo. Each step depends on the previous one. you cannot assign a ticket before classifying its priority, and you cannot close it before it is resolved.

This workflow drives a single ticket through its full lifecycle. The creator generates a ticket ID and records the subject and reporter. The classifier determines category (bug, feature request, question) and priority (critical, high, medium, low). The assigner routes the ticket to an appropriate agent based on category and priority. The resolver records the fix and resolution details. The closer marks the ticket as done and captures final metadata.

## The Solution

**You just write the ticket-creation, classification, assignment, resolution, and closure workers. Conductor handles the full lifecycle sequencing.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

CreateTicketWorker generates a unique ID, ClassifyTicketWorker determines category and priority, AssignTicketWorker routes to an agent, ResolveTicketWorker records the fix, and CloseTicketWorker completes the lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **AssignTicketWorker** | `tkt_assign` | Routes the ticket to an appropriate agent based on category and priority. |
| **ClassifyTicketWorker** | `tkt_classify` | Determines the ticket's category (bug, feature request, question) and priority level. |
| **CloseTicketWorker** | `tkt_close` | Marks the ticket as closed and records final metadata. |
| **CreateTicketWorker** | `tkt_create` | Creates a new ticket with a unique ID from the subject, description, and reporter. |
| **ResolveTicketWorker** | `tkt_resolve` | Records the resolution details and marks the ticket as resolved. |

### The Workflow

```
tkt_create
 │
 ▼
tkt_classify
 │
 ▼
tkt_assign
 │
 ▼
tkt_resolve
 │
 ▼
tkt_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
