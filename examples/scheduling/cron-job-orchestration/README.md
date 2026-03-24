# Cron Job Orchestration in Java Using Conductor: Schedule, Execute, Log, and Clean Up

The nightly data export runs at 2 AM. It creates 4 GB of temp files in `/tmp`, writes results to S3, and is supposed to clean up after itself. Last Thursday, the S3 upload timed out. The cron job exited with code 1. Nobody noticed. Cron sent an email to root, which nobody reads. The temp files stayed. Friday night, same thing. By Monday, `/tmp` was full, and every other service on the box started failing with "No space left on device." You SSH in, find 16 GB of orphaned export files, and realize there's no log of which runs succeeded, which failed, or what they left behind.

## The Problem

You have cron jobs that need more than just `crontab -e`. You need to track execution results, clean up temp files after the job runs, and know when a scheduled job fails silently. Traditional cron fires and forgets: if the job fails, the only evidence is buried in syslog. If temp files accumulate, disk fills up. If the job takes longer than expected, the next invocation starts before the first finishes.

Without orchestration, cron job management means parsing syslog for failures, writing cleanup scripts that may or may not run, and building custom locking to prevent overlapping executions. Each job has its own ad-hoc monitoring, and there's no unified view of job health.

## The Solution

**You just write the job execution and cleanup commands. Conductor handles sequential execution with guaranteed cleanup, retries on job failures, and a unified view of every cron job's execution history, exit codes, and cleanup status.**

Each cron job concern is an independent worker. Scheduling, execution, result logging, and cleanup. Conductor orchestrates them in sequence: schedule the job, execute it, log the results, then clean up. Every job run is tracked with execution time, output, exit code, and cleanup status.

### What You Write: Workers

Four workers manage each cron job run: ScheduleJobWorker registers the schedule, ExecuteJobWorker runs the command, LogResultWorker records stdout/stderr and exit codes, and CleanupWorker removes temporary files created during execution.

| Worker | Task | What It Does |
|---|---|---|
| **CleanupWorker** | `cj_cleanup` | Cleans up temporary files produced by a cron job. |
| **ExecuteJobWorker** | `cj_execute_job` | Executes a cron job command. |
| **LogResultWorker** | `cj_log_result` | Logs the result of a cron job execution. |
| **ScheduleJobWorker** | `cj_schedule_job` | Schedules a cron job with the given name and expression. |

### The Workflow

```
cj_schedule_job
 │
 ▼
cj_execute_job
 │
 ▼
cj_log_result
 │
 ▼
cj_cleanup

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
