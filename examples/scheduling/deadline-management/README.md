# Deadline Management in Java Using Conductor: Check Deadlines, Route by Urgency, and Handle Overdue Items

The SOC 2 compliance filing was due Friday. On Monday morning, the auditor emails asking where it is. You check Jira, the ticket was assigned to someone who went on vacation. The "deadline reminder" was a cron job that sent an email three days before the due date, but the assignee's inbox had 200 unread messages. Nobody escalated because the system doesn't distinguish between "due in two weeks" and "due in four hours." By the time the overdue item surfaces, you're already in breach, scrambling to file late, and explaining to leadership why a known deadline slipped through a process that was supposed to prevent exactly this.

## The Problem

You manage tasks with deadlines: support tickets, deliverables, compliance filings. Each task's urgency depends on how close it is to its due date: still on track (normal processing), approaching deadline (urgent, escalate priority), or past due (overdue, immediate escalation and notification). The routing must be automatic and consistent across all tasks.

Without orchestration, deadline tracking lives in spreadsheets or dashboards that require manual review. Someone checks the list daily, misses an overdue item, and it becomes a fire drill. Building deadline automation as a script means hardcoding urgency thresholds and manually routing to different handling paths.

## The Solution

**You just write the deadline evaluation and escalation rules. Conductor handles urgency-based routing via SWITCH tasks, retries on escalation service failures, and a full record of every deadline evaluation and routing decision.**

A deadline checker worker evaluates the task's due date against current time. Conductor's SWITCH task routes to the appropriate handler: normal, urgent, or overdue, based on the urgency classification. Each handler takes the right action for its urgency level. Every deadline evaluation is tracked, so you can audit which items were escalated and when.

### What You Write: Workers

Workers handle each urgency level: CheckDeadlinesWorker evaluates how close a task is to its due date, then Conductor routes to HandleNormalWorker for on-track items, HandleUrgentWorker for approaching deadlines, or HandleOverdueWorker for past-due escalation.

| Worker | Task | What It Does |
|---|---|---|
| **CheckDeadlinesWorker** | `ded_check_deadlines` | Evaluates a task's due date, returning an urgency classification (normal/urgent/overdue) and hours remaining or overdue |
| **HandleNormalWorker** | `ded_handle_normal` | Handles on-track tasks by setting them to monitor status with a 24-hour check-in interval |
| **HandleOverdueWorker** | `ded_handle_overdue` | Escalates overdue tasks to management with P0 priority, reporting hours past deadline |
| **HandleUrgentWorker** | `ded_handle_urgent` | Escalates urgent tasks to the senior team with P1 priority, reporting remaining hours before deadline |

### The Workflow

```
ded_check_deadlines
 │
 ▼
SWITCH (ded_switch_ref)
 ├── urgent: ded_handle_urgent
 ├── overdue: ded_handle_overdue
 └── default: ded_handle_normal

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
