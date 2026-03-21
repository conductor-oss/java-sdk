# Trouble Ticket in Java Using Conductor

## Why Trouble Ticket Management Needs Orchestration

Handling a customer trouble ticket requires a structured progression where each step depends on the outcome of the previous one. You open a ticket with the customer's ID, issue type, and description. You diagnose the reported issue to categorize it (network fault, equipment failure, billing dispute, provisioning error). You assign the ticket to a technician with the right skills for that category. The assigned technician resolves the issue and records the resolution. Finally, you close the ticket with the resolution details and timestamp.

If diagnosis categorizes incorrectly, the ticket gets assigned to the wrong team and bounces around for days. If the resolution completes but the ticket isn't closed, SLA metrics look worse than reality and the customer gets follow-up calls about an already-fixed problem. Without orchestration, you'd build a monolithic ticket handler that mixes CRM lookups, network diagnostics, workforce scheduling, and notification logic. making it impossible to swap ticketing systems, test diagnosis rules independently, or track mean time to resolution across issue categories.

## The Solution

**You just write the ticket creation, issue diagnosis, technician assignment, resolution, and ticket closure logic. Conductor handles diagnostic retries, dispatch routing, and ticket resolution audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Ticket creation, diagnostic routing, technician dispatch, and resolution tracking workers each manage one phase of customer issue resolution.

| Worker | Task | What It Does |
|---|---|---|
| **AssignWorker** | `tbt_assign` | Assigns the ticket to a technician with the right skills based on the diagnosed category. |
| **CloseWorker** | `tbt_close` | Closes the ticket with the resolution details and records the closure timestamp. |
| **DiagnoseWorker** | `tbt_diagnose` | Diagnoses the reported issue to categorize it (network fault, equipment failure, billing, provisioning). |
| **OpenWorker** | `tbt_open` | Opens a trouble ticket with the customer ID, issue type, and returns a ticket ID. |
| **ResolveWorker** | `tbt_resolve` | Records the resolution performed by the assigned technician for the ticket. |

### The Workflow

```
tbt_open
 │
 ▼
tbt_diagnose
 │
 ▼
tbt_assign
 │
 ▼
tbt_resolve
 │
 ▼
tbt_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
