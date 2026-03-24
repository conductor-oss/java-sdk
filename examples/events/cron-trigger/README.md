# Cron Trigger in Java Using Conductor

Cron-like scheduled workflow: check if the current time matches a schedule expression, decide whether to run via SWITCH, and execute or skip accordingly.

## The Problem

You need to execute scheduled jobs based on cron expressions. The workflow must evaluate whether the current time falls within a cron schedule window, decide whether to run or skip the job, execute the scheduled tasks if triggered, and record the run for audit. Skipping a scheduled job without logging it means you lose visibility into why a task did not execute.

Without orchestration, you'd build a cron daemon that parses expressions, forks processes for each job, manages PID files, and writes to a run log. manually handling overlapping executions, retrying failed jobs, and grepping through log files to figure out why Tuesday's report did not run.

## The Solution

**You just write the schedule-check, task-execution, skip-logging, and run-recording workers. Conductor handles cron-based SWITCH routing, durable run recording, and automatic retry of failed job executions.**

Each scheduling concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing the schedule check, routing via a SWITCH task to either execute or skip, recording the result, retrying if the job execution fails, and tracking every scheduled run with full input/output details.

### What You Write: Workers

Four workers implement scheduled execution: CheckScheduleWorker evaluates a cron expression against the current time, ExecuteTasksWorker runs the job, LogSkipWorker records skipped windows, and RecordRunWorker logs completed executions.

| Worker | Task | What It Does |
|---|---|---|
| **CheckScheduleWorker** | `cn_check_schedule` | Checks whether a cron schedule matches the current time window. |
| **ExecuteTasksWorker** | `cn_execute_tasks` | Executes the scheduled tasks for a cron job. |
| **LogSkipWorker** | `cn_log_skip` | Logs that a scheduled job was skipped. |
| **RecordRunWorker** | `cn_record_run` | Records a completed cron job run. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
cn_check_schedule
 │
 ▼
SWITCH (switch_ref)
 ├── yes: cn_execute_tasks -> cn_record_run
 ├── no: cn_log_skip

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
