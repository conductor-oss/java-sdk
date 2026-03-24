# Cron Trigger

An operations team needs to run a cleanup job every night at 2 AM, a report generation every Monday at 6 AM, and a health check every 15 minutes. Each scheduled trigger needs parsing from a cron expression, execution of the associated job, and logging of the result with the next scheduled run time.

## Pipeline

```
[cn_check_schedule]
     |
     v
     <SWITCH>
       |-- yes -> [cn_execute_tasks]
       |-- yes -> [cn_record_run]
       |-- no -> [cn_log_skip]
```

**Workflow inputs:** `cronExpression`, `jobName`

## Workers

**CheckScheduleWorker** (task: `cn_check_schedule`)

Checks whether a cron schedule matches the current time window.

- Reads `cronExpression`, `jobName`. Writes `shouldRun`, `scheduledTime`, `cronExpression`

**ExecuteTasksWorker** (task: `cn_execute_tasks`)

Executes the scheduled tasks for a cron job.

- Sets `result` = `"success"`
- Reads `jobName`, `scheduledTime`. Writes `result`, `executedAt`, `duration`

**LogSkipWorker** (task: `cn_log_skip`)

Logs that a scheduled job was skipped.

- Reads `jobName`, `reason`. Writes `skipped`, `reason`

**RecordRunWorker** (task: `cn_record_run`)

Records a completed cron job run.

- Reads `jobName`, `result`. Writes `recorded`, `runId`

---

**32 tests** | Workflow: `cron_trigger` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
