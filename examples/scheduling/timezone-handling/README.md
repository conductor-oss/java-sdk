# Timezone Handling

A user in one timezone schedules a job that must execute at the correct UTC time. The pipeline detects the user's timezone, converts the requested time from source to target timezone, schedules the job at the computed UTC time, and executes it.

## Workflow

```
tz_detect_zone ──> tz_convert_time ──> tz_schedule_job ──> tz_execute_job
```

Workflow `timezone_handling_407` accepts `userId`, `requestedTime`, and `jobName`. Times out after `60` seconds.

## Workers

**DetectZoneWorker** (`tz_detect_zone`) -- detects the timezone for the specified user.

**ConvertTimeWorker** (`tz_convert_time`) -- converts the requested time from the source timezone to the target timezone.

**ScheduleJobWorker** (`tz_schedule_job`) -- schedules the job at the computed UTC time.

**ExecuteJobWorker** (`tz_execute_job`) -- executes the job at the scheduled UTC time.

## Workflow Output

The workflow produces `detectedTimezone`, `utcTime`, `scheduled`, `executed` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `timezone_handling_407` defines 4 tasks with input parameters `userId`, `requestedTime`, `jobName` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify timezone detection, time conversion, scheduling, and execution.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
