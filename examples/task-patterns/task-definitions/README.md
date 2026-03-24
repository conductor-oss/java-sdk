# Task Definitions

A minimal workflow demonstrates how task definitions configure retry, timeout, and response timeout settings independently of worker code. The single fast task has explicit definition-level configuration.

## Workflow

```
td_fast_task
```

Workflow `task_def_test` takes no inputs. Times out after `30` seconds.

## Workers

**FastTaskWorker** (`td_fast_task`) -- executes quickly and returns a completion status.

## Workflow Output

The workflow produces `done` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `td_critical_task`: retryCount=5, retryLogic=EXPONENTIAL_BACKOFF, retryDelaySeconds=2, timeoutSeconds=300, responseTimeoutSeconds=60
- `td_fast_task`: retryCount=1, retryLogic=FIXED, retryDelaySeconds=1, timeoutSeconds=10, responseTimeoutSeconds=5
- `td_limited_task`: retryCount=3, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `task_def_test` defines 1 task with input parameters none and a timeout of `30` seconds.

## Tests

6 tests verify task definition registration, timeout configuration, retry settings, and worker execution within the defined constraints.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
