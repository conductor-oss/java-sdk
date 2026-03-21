# Milestone Tracking in Java with Conductor : Progress Checking, Health Evaluation, Conditional Routing (On-Track / At-Risk / Delayed), and Action Execution

## Why Milestone Tracking Needs Orchestration

Tracking milestones requires more than checking a percentage. you need to evaluate progress against the deadline and take different actions depending on how healthy the milestone looks. You check progress by counting completed deliverables against the total (e.g., 7 of 10 done, 70% complete). You evaluate that progress against the timeline, 70% complete at 80% of the timeline elapsed means the milestone is at risk, not on track. Based on the health status, you take fundamentally different actions: on-track milestones continue as planned, at-risk milestones get escalated to the project lead for attention, and delayed milestones trigger recovery plans with scope re-negotiation or deadline extension.

The conditional routing is the key design decision. A milestone at 70% completion needs escalation to the lead, while a milestone at 90% completion just needs a status update. Without orchestration, you'd write nested if/else blocks mixing progress queries, evaluation logic, Slack notifications for escalation, and Jira updates for status changes. making it impossible to add a new health category (e.g., "blocked"), test your escalation logic independently from progress checking, or audit which milestones were escalated and what action was taken.

## How This Workflow Solves It

**You just write the progress checking, health evaluation, and on-track/at-risk/delayed response logic. Conductor handles progress collection retries, risk alerting, and milestone audit trails.**

Each tracking stage is an independent worker. check progress, evaluate, handle on-track/at-risk/delayed, act. Conductor sequences the progress check and evaluation, then uses a SWITCH task to route to the correct handler based on the health status. On-track milestones flow to the on-track handler (action: continue), at-risk milestones flow to the at-risk handler (action: escalate to lead), and delayed milestones flow to the delayed handler. After the SWITCH branch completes, all paths converge at the act worker which executes the recommended action. Conductor retries if your PM tool's API is unavailable during the progress check, and records the full evaluation trail for every milestone.

### What You Write: Workers

Progress collection, milestone evaluation, risk flagging, and status reporting workers each monitor one aspect of project timeline adherence.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `mst_act` | Taking action for milestone |
| **AtRiskWorker** | `mst_at_risk` | At Risk. Computes and returns action |
| **CheckProgressWorker** | `mst_check_progress` | Checking milestone |
| **DelayedWorker** | `mst_delayed` | Delayed. Computes and returns action |
| **EvaluateWorker** | `mst_evaluate` | Evaluates and computes pct done |
| **OnTrackWorker** | `mst_on_track` | On Track. Computes and returns action |

### The Workflow

```
mst_check_progress
 │
 ▼
mst_evaluate
 │
 ▼
SWITCH (switch_ref)
 ├── on_track: mst_on_track
 ├── at_risk: mst_at_risk
 └── default: mst_delayed
 │
 ▼
mst_act

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
