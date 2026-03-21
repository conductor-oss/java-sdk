# Zendesk Integration in Java Using Conductor

## Automating Zendesk Ticket Triage and Resolution

When a support request arrives, you need to create a ticket in Zendesk, analyze its subject and description to determine priority (urgent, high, normal, low), route it to the right agent or group based on priority and category (billing goes to finance, bugs go to engineering), and ultimately mark it as resolved. Each step depends on the previous one. you cannot classify without a ticket ID, and you cannot route without knowing the priority.

Without orchestration, you would chain Zendesk REST API calls manually, manage ticket IDs, priority levels, and agent assignments between steps, and handle edge cases like misrouted tickets or classification failures. Conductor sequences the pipeline and routes ticket IDs, priorities, and agent IDs between workers automatically.

## The Solution

**You just write the support ticket workers. Creation, priority classification, agent routing, and resolution. Conductor handles ticket-to-resolution sequencing, Zendesk API retries, and priority-based routing of ticket IDs between classification, assignment, and resolution stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers manage the support lifecycle: CreateTicketWorker opens tickets, ClassifyTicketWorker determines priority, RouteTicketWorker assigns the right agent, and ResolveTicketWorker closes the case.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyTicketWorker** | `zd_classify` | Classifies ticket priority. |
| **CreateTicketWorker** | `zd_create_ticket` | Creates a Zendesk support ticket. |
| **ResolveTicketWorker** | `zd_resolve` | Resolves a ticket. |
| **RouteTicketWorker** | `zd_route` | Routes ticket to an agent. |

the workflow orchestration and error handling stay the same.

### The Workflow

```
zd_create_ticket
 │
 ▼
zd_classify
 │
 ▼
zd_route
 │
 ▼
zd_resolve

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
