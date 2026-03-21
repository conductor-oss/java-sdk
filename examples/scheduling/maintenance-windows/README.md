# Maintenance Window Management in Java Using Conductor : Time-Window Checks with Execute or Defer

## The Problem

You need to perform maintenance. database migrations, certificate rotations, patch deployments; but only during approved maintenance windows. If a maintenance task is triggered outside the window, it must be deferred rather than executed. The system must check the current time against the window schedule and route accordingly: execute now or schedule for later.

Without orchestration, maintenance windows are enforced by human judgment. an engineer checks the clock before running a script. Automated scripts either ignore maintenance windows entirely (running at any time) or are scheduled with cron at fixed times that don't adapt when windows change.

## The Solution

**You just write the window schedule checks and maintenance task logic. Conductor handles time-window evaluation with conditional routing, retries on maintenance task failures, and a record of every execution or deferral with timing details.**

A window checker worker evaluates whether the current time falls within the maintenance window. Conductor's SWITCH task routes to either the execute path or the defer path. If maintenance runs, it's tracked with timing and results. If deferred, the deferral is recorded with the reason and next available window. ### What You Write: Workers

CheckWindowWorker determines if the current time falls within the maintenance window, then Conductor routes to either ExecuteMaintenanceWorker to run tasks like db-vacuum and index-rebuild, or DeferMaintenanceWorker to schedule for the next approved window.

| Worker | Task | What It Does |
|---|---|---|
| **CheckWindowWorker** | `mnw_check_window` | Checks whether the current time falls within the approved maintenance window for a system, returning window status and remaining minutes |
| **DeferMaintenanceWorker** | `mnw_defer_maintenance` | Defers maintenance to the next available window, recording the reason and scheduled time |
| **ExecuteMaintenanceWorker** | `mnw_execute_maintenance` | Runs maintenance tasks (db-vacuum, index-rebuild, cache-clear) on the target system and reports duration and completed tasks |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
mnw_check_window
 │
 ▼
SWITCH (mnw_switch_ref)
 ├── in_window: mnw_execute_maintenance
 └── default: mnw_defer_maintenance

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
