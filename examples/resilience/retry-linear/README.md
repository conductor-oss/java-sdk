# Linear Backoff Retry

A service goes down and needs progressively more time to recover, but exponential backoff grows too aggressively -- after just 5 retries the delay is 16 seconds. Linear backoff increases delays proportionally (1s, 2s, 3s, 4s), giving the service more time without the exponential explosion.

## Workflow

```
retry_linear_task ──(fails attempts 1-3, succeeds on attempt 4)──> success
```

Workflow `retry_linear_demo` accepts `service` as input. The task definition sets `retryCount` = `4`, `retryLogic` = `LINEAR_BACKOFF`, `retryDelaySeconds` = `1`, `timeoutSeconds` = `60`, and `responseTimeoutSeconds` = `30`.

## Workers

**RetryLinearWorker** (`retry_linear_task`) -- maintains an `AtomicInteger` called `attemptCounter`. Reads `service` from input (defaults to `"default-service"` if null or blank). For attempts `< 4`, returns `FAILED` with `reasonForIncompletion` = `"Service unavailable (attempt N)"` and `serviceStatus` = `"unavailable"`. On attempt 4+, returns `COMPLETED` with `serviceStatus` = `"healthy"`. The `attempts` output tracks total invocations. Exposes `getAttemptCount()` and `resetAttemptCounter()`.

## Workflow Output

The workflow produces `attempts`, `serviceStatus` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `retry_linear_task`: retryCount=4, retryLogic=LINEAR_BACKOFF, retryDelaySeconds=1, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `retry_linear_demo` defines 1 task with input parameters `service` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "LINEAR_BACKOFF retry demo — delay increases linearly (delay * attempt).". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

8 tests verify first-attempt failure, success on the fourth attempt, attempt counter accuracy, linear delay progression, service input handling, and the `LINEAR_BACKOFF` task definition configuration.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
