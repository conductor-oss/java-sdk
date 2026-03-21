# Switch Task in Java with Conductor

SWITCH task demo. routes support tickets to different handlers based on priority level (LOW/MEDIUM/HIGH) with a default catch-all for unrecognized values. Uses Conductor's `value-param` evaluator for declarative conditional branching without JavaScript expressions. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to route support tickets to different handlers based on priority. LOW priority tickets should be auto-resolved with a canned response. MEDIUM priority tickets should be assigned to the support team for review. HIGH priority tickets should be immediately escalated to a manager. Unknown or missing priorities should be flagged for triage. After the ticket is handled: regardless of which priority path was taken, the action must be logged for audit and SLA tracking.

Without orchestration, you'd write a switch statement or if/else chain, calling different handler functions based on the priority string. When a new priority level is added (e.g., CRITICAL), you modify the routing code and hope the else clause still catches edge cases. If the escalation handler fails for a HIGH ticket, there is no automatic retry, and the ticket sits unhandled. There is no record of which handler processed a given ticket without searching through application logs.

## The Solution

**You just write the priority-specific ticket handlers and the audit logger. Conductor handles the SWITCH routing, default-case fallback, and execution tracking.**

This example demonstrates Conductor's SWITCH task with `value-param` evaluator for ticket priority routing. The SWITCH matches on the `priority` input: `LOW` routes to AutoHandleWorker (auto-resolution), `MEDIUM` routes to TeamReviewWorker (team assignment), `HIGH` routes to EscalateWorker (manager escalation), and unrecognized values fall to the default case handled by UnknownPriorityWorker (triage flagging). After the SWITCH resolves, LogActionWorker records the action taken on the ticket regardless of which branch executed. Conductor tracks which priority branch was taken for every ticket, giving you a complete audit trail of ticket routing decisions.

### What You Write: Workers

Five workers cover the priority-based ticket routing: AutoHandleWorker auto-resolves LOW tickets, TeamReviewWorker assigns MEDIUM tickets, EscalateWorker pages managers for HIGH tickets, UnknownPriorityWorker flags unrecognized values, and LogActionWorker records the audit trail after every branch.

| Worker | Task | What It Does |
|---|---|---|
| **AutoHandleWorker** | `sw_auto_handle` | Handles LOW priority tickets: returns handler="auto" and resolved=true, indicating the ticket was auto-resolved without human intervention. |
| **TeamReviewWorker** | `sw_team_review` | Handles MEDIUM priority tickets: returns handler="team" and assignedTo="support-team-1", indicating the ticket was routed to a support team for manual review. |
| **EscalateWorker** | `sw_escalate` | Handles HIGH priority tickets: returns handler="manager" and escalatedTo="manager@example.com", indicating immediate manager escalation. |
| **UnknownPriorityWorker** | `sw_unknown_priority` | Handles unrecognized priority values (default case): returns handler="default" and needsClassification=true, flagging the ticket for triage. |
| **LogActionWorker** | `sw_log_action` | Runs after every SWITCH branch. Logs the ticketId and priority, returns logged=true to confirm the audit trail entry was recorded. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic, the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
SWITCH (route_ref)
 ├── LOW: sw_auto_handle
 ├── MEDIUM: sw_team_review
 ├── HIGH: sw_escalate
 └── default: sw_unknown_priority
 │
 ▼
sw_log_action

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
