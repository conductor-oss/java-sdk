# Poll Timeout

A task is scheduled but no worker picks it up -- the worker process crashed, the deployment failed, or the worker is polling the wrong queue. Without a poll timeout, the task sits in the queue indefinitely and the workflow hangs. The system needs to detect absent workers within a bounded time window.

## Workflow

```
poll_normal_task
```

Workflow `poll_timeout_demo` accepts `mode` as input. The task definition for `poll_normal_task` sets `pollTimeoutSeconds` = `30`, `retryCount` = `2`, `timeoutSeconds` = `60`, and `responseTimeoutSeconds` = `30`.

## Workers

**PollNormalTaskWorker** (`poll_normal_task`) -- reads `mode` from input. If `mode` is `null`, blank, or empty, defaults to `"default"`. Returns `result` = `"processed"`. The `pollTimeoutSeconds` = `30` setting means if no worker picks up this task within 30 seconds of scheduling, Conductor marks it as timed out and applies retry logic.

## Workflow Output

The workflow produces `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `poll_normal_task`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `poll_timeout_demo` defines 1 task with input parameters `mode` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Poll timeout demo — demonstrates how pollTimeoutSeconds defines how long a task waits in the queue for a worker.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

8 tests verify successful task pickup, mode input handling, default mode behavior, and that the poll timeout configuration is correctly applied to detect absent workers.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
