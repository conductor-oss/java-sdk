# Fixed Retry

A task fails due to a transient issue -- a dropped database connection, a temporarily held file lock, a restarting service. The recovery time is predictable (1-2 seconds), so exponential backoff would waste time with unnecessarily long delays. Fixed-interval retries wait the same 1-second duration between every attempt.

## Workflow

```
retry_fixed_task ──(fails for failCount attempts, then succeeds)──> success
```

Workflow `retry_fixed_demo` accepts `failCount` as input. The task definition sets `retryCount` = `3`, `retryLogic` = `FIXED`, `retryDelaySeconds` = `1`, `timeoutSeconds` = `60`, and `responseTimeoutSeconds` = `30`.

## Workers

**RetryFixedWorker** (`retry_fixed_task`) -- maintains a `ConcurrentHashMap<String, AtomicInteger>` called `attemptCounters` keyed by `workflowInstanceId` (falls back to `"default"` if null). Reads `failCount` from input as a `Number`. For attempts `<= failCount`, returns `FAILED` with `error` = `"Intentional failure on attempt N"`. On the next attempt, returns `COMPLETED` with `result` = `"success"` and removes the counter entry for the workflow. The `attempts` output reports the total invocation count.

## Workflow Output

The workflow produces `attempts`, `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `retry_fixed_task`: retryCount=3, retryLogic=FIXED, retryDelaySeconds=1, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `retry_fixed_demo` defines 1 task with input parameters `failCount` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "FIXED retry strategy demo -- retries retry_fixed_task with constant 1s delay between attempts.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

9 tests verify success on first attempt, success after transient failures, correct attempt counting per workflow, counter cleanup on success, and the fixed retry task definition configuration.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
