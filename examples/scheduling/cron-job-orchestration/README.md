# Cron Job Orchestration

A scheduled job needs full lifecycle management. The pipeline parses the cron expression, executes the job and measures its duration in milliseconds, logs the result, and cleans up resources afterward.

## Workflow

```
cj_schedule_job ──> cj_execute_job ──> cj_log_result ──> cj_cleanup
```

Workflow `cron_job_orchestration_401` accepts `jobName`, `cronExpression`, and `command`. Times out after `60` seconds.

## Workers

**ScheduleJobWorker** (`cj_schedule_job`) -- parses the cron expression and schedules the job.

**ExecuteJobWorker** (`cj_execute_job`) -- executes the job and reports completion duration in milliseconds.

**LogResultWorker** (`cj_log_result`) -- logs the execution result.

**CleanupWorker** (`cj_cleanup`) -- cleans up resources after execution.

## Workflow Output

The workflow produces `scheduledAt`, `exitCode`, `logged`, `cleanedUp` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `cron_job_orchestration_401` defines 4 tasks with input parameters `jobName`, `cronExpression`, `command` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Orchestrate cron jobs: schedule the job, execute it, log the result, and clean up temporary resources.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

1 test verifies the end-to-end cron job lifecycle.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
