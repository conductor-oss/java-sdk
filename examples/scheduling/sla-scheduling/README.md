# SLA Scheduling in Java Using Conductor : Ticket Prioritization, Execution, and Compliance Tracking

## The Problem

You have tickets with SLA commitments. P1 tickets need resolution within 1 hour, P2 within 4 hours, P3 within 24 hours. Tasks must be prioritized by SLA proximity (how close to breach), executed in the right order, and compliance must be tracked. If a P2 ticket will breach its SLA in 30 minutes while a P3 has 20 hours remaining, the P2 must be worked first.

Without orchestration, SLA prioritization is a manual sort in a ticketing system. Agents pick tickets by FIFO rather than SLA urgency, breaches happen because high-urgency tickets were buried, and compliance tracking requires a separate report that's always outdated.

## The Solution

**You just write the SLA prioritization rules and compliance calculations. Conductor handles the prioritize-execute-track sequence, retries on ticket system failures, and SLA compliance metrics with per-ticket timing for every scheduling cycle.**

Each SLA concern is an independent worker. prioritization by SLA urgency, task execution in priority order, and compliance tracking. Conductor runs them in sequence: prioritize the queue, execute in order, then track compliance. Every scheduling run is tracked with priority assignments, execution timing, and SLA compliance metrics.

### What You Write: Workers

Three workers enforce SLA discipline: PrioritizeWorker sorts tickets by SLA urgency so the closest-to-breach items come first, ExecuteTasksWorker processes them in priority order, and TrackComplianceWorker calculates on-time completion rates and flags breaches.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteTasksWorker** | `sla_execute_tasks` | Processes tickets in SLA priority order, resolving each and recording time-to-resolution |
| **PrioritizeWorker** | `sla_prioritize` | Sorts tickets by SLA urgency (1h, 4h, 24h deadlines), returning an ordered list with priority rankings |
| **TrackComplianceWorker** | `sla_track_compliance` | Calculates SLA compliance rate, counting on-time completions vs, breaches and average resolution time |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
sla_prioritize
 │
 ▼
sla_execute_tasks
 │
 ▼
sla_track_compliance

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
