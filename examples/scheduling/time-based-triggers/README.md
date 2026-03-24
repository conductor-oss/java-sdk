# Time-Based Triggers in Java Using Conductor : Route Jobs by Time Window (Morning, Afternoon, Evening)

## The Problem

Different jobs should run at different times of day. morning is for data ingestion and ETL, afternoon for report generation and distribution, evening for maintenance and cleanup. You need to check the current time window and route to the appropriate job. This is more flexible than cron (which fires at exact times) because it handles late triggers, catch-up runs, and timezone-aware routing.

Without orchestration, time-based routing is hardcoded in cron schedules or manual if/else checks at the top of scripts. When the morning job runs late, it doesn't automatically become an afternoon job. Adding a new time window means restructuring the schedule.

## The Solution

**You just write the time-window detection and per-window job logic. Conductor handles SWITCH-based time-window routing, retries when individual jobs fail, and a full history of every trigger showing which window was detected and which job executed.**

A time checker worker determines the current time window (morning, afternoon, evening). Conductor's SWITCH task routes to the appropriate job. Each job is an independent worker that does its time-window-specific work. Every trigger is tracked with the detected time window and which job ran.

### What You Write: Workers

CheckTimeWorker determines the current time window (morning, afternoon, or evening), then Conductor routes to the appropriate handler: MorningJobWorker for data ingestion, AfternoonJobWorker for report generation, or EveningJobWorker for maintenance and cleanup.

| Worker | Task | What It Does |
|---|---|---|
| **AfternoonJobWorker** | `tb_afternoon_job` | Runs afternoon tasks (e.g., report generation), returning the count of reports generated |
| **CheckTimeWorker** | `tb_check_time` | Determines the current time window (morning/afternoon/evening) based on hour and timezone |
| **EveningJobWorker** | `tb_evening_job` | Runs evening tasks (e.g., cleanup and maintenance), returning the count of files cleaned up |
| **MorningJobWorker** | `tb_morning_job` | Runs morning tasks (e.g., data sync and ETL), returning the count of records synced |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
tb_check_time
 │
 ▼
SWITCH (tb_switch_ref)
 ├── morning: tb_morning_job
 ├── afternoon: tb_afternoon_job
 └── default: tb_evening_job

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
