# Time-Based Triggers

A job must run in a specific time window based on the current hour and timezone. The pipeline checks the time, classifies the window (morning, afternoon, or evening), and routes to the appropriate job handler via a SWITCH task.

## Workflow

```
tb_check_time ──> SWITCH(window)
                    ├── "morning" ──> tb_morning_job
                    ├── "afternoon" ──> tb_afternoon_job
                    └── "evening" ──> tb_evening_job
```

Workflow `time_based_triggers_405` accepts `timezone` and `currentHour`. Times out after `60` seconds.

## Workers

**CheckTimeWorker** (`tb_check_time`) -- evaluates the current hour and timezone. Classifies the time window.

**MorningJobWorker** (`tb_morning_job`) -- runs the morning job variant.

**AfternoonJobWorker** (`tb_afternoon_job`) -- runs the afternoon job variant.

**EveningJobWorker** (`tb_evening_job`) -- runs the evening job variant.

## Workflow Output

The workflow produces `timeWindow`, `localTime`, `jobExecuted` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `time_based_triggers_405` defines 2 tasks with input parameters `timezone`, `currentHour` and a timeout of `60` seconds.

## Tests

2 tests verify time window classification and correct job routing.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
