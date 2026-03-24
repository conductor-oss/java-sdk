# Exponential Backoff Retry

An API enforces rate limits and returns HTTP `429` errors under load. Retrying immediately makes things worse by burning through the quota faster. The system must double the wait time between each retry -- 1s, 2s, 4s, 8s -- to give the service time to recover.

## Workflow

```
retry_expo_task ──(429 on attempts 1-2, 200 on attempt 3)──> success
```

Workflow `retry_expo_demo` accepts `apiUrl` as input. The task definition for `retry_expo_task` sets `retryCount` = `4`, `retryLogic` = `EXPONENTIAL_BACKOFF`, `retryDelaySeconds` = `1`, `timeoutSeconds` = `120`, and `responseTimeoutSeconds` = `60`.

## Workers

**RetryExpoTaskWorker** (`retry_expo_task`) -- maintains an `AtomicInteger` called `attemptCounter`. Reads `apiUrl` from input (defaults to `"https://api.example.com/data"` if null or blank). The constructor accepts a `failUntilAttempt` parameter (default `2`) controlling how many attempts return `FAILED`. For attempts `<= failUntilAttempt`, returns `FAILED` with `reasonForIncompletion` = `"HTTP 429 Too Many Requests (attempt N)"` and `error` = `"429 Too Many Requests"`. For later attempts, returns `COMPLETED` with `data` = `Map.of("status", "ok")`. The `attempts` output tracks the total count. Exposes `resetAttempts()` and `getAttemptCount()`.

## Workflow Output

The workflow produces `attempts`, `data` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `retry_expo_task`: retryCount=4, retryLogic=EXPONENTIAL_BACKOFF, retryDelaySeconds=1, timeoutSeconds=120, responseTimeoutSeconds=60

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `retry_expo_demo` defines 1 task with input parameters `apiUrl` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Exponential backoff retry demo -- worker returns 429 errors, Conductor retries with doubling delay.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

12 tests cover first-attempt failure, success after retries, configurable failure thresholds, attempt counting, the exponential backoff task definition, and URL input handling.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
