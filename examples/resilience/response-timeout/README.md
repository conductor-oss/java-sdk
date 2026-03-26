# Response Timeout

A worker picks up a task but gets stuck -- deadlocked on a resource, blocked on a network call that never returns, or trapped in an infinite loop. Without a response timeout, Conductor waits forever, the workflow hangs, and all downstream steps are blocked. The system needs to detect stuck workers and trigger retry logic within a bounded time.

## Workflow

```
resp_timeout_task
```

Workflow `resp_timeout_demo` accepts `mode` as input and times out after `30` seconds. The task definition for `resp_timeout_task` sets `responseTimeoutSeconds` = `3`, `retryCount` = `2`, `retryLogic` = `FIXED`, and `retryDelaySeconds` = `1`.

## Workers

**RespTimeoutWorker** (`resp_timeout_task`) -- maintains an `AtomicInteger` called `attempt` that increments on every invocation. Reads `mode` from input (defaults to `"fast"` if null or blank). Returns `result` = `"fast-response"` and `attempt` = the current counter value. Exposes `getAttemptCount()` and `resetAttemptCount()` for monitoring and testing. The `responseTimeoutSeconds` = `3` means Conductor will mark this task as timed out if the worker does not respond within 3 seconds of picking it up.

## Workflow Output

The workflow produces `result`, `attempt` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `resp_timeout_task`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=1, timeoutSeconds=30, responseTimeoutSeconds=3

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `resp_timeout_demo` defines 1 task with input parameters `mode` and a timeout of `30` seconds.

## Workflow Definition Details

Workflow description: "Response timeout demo — detect stuck workers using responseTimeoutSeconds.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

8 tests verify fast completion within the timeout window, attempt counter tracking, mode input handling, and that the `responseTimeoutSeconds` configuration correctly bounds worker response time.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
