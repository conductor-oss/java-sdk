# Timeout Policies

Different tasks need different timeout behaviors. A payment task should terminate the entire workflow on timeout (TIME_OUT_WF) because proceeding without payment confirmation is dangerous. An enrichment task should retry on timeout (RETRY) because it usually works on the second attempt. An analytics task can silently time out (ALERT_ONLY) because the data will be backfilled later.

## Workflow

```
tp_critical (timeoutPolicy: TIME_OUT_WF)
```

Workflow `timeout_policy_demo` accepts `mode` as input and times out after `30` seconds. Three task definitions demonstrate the three policies.

## Workers

**CriticalWorker** (`tp_critical`) -- returns `result` = `"critical-done"`. Task definition: `timeoutPolicy` = `TIME_OUT_WF`, `responseTimeoutSeconds` = `10`, `retryCount` = `0`. If the worker does not respond within 10 seconds, the entire workflow is terminated.

**RetryableWorker** (`tp_retryable`) -- returns `result` = `"retried-done"`. Task definition: `timeoutPolicy` = `RETRY`, `responseTimeoutSeconds` = `10`, `retryCount` = `2`, `retryLogic` = `FIXED`, `retryDelaySeconds` = `1`. If the worker times out, Conductor automatically retries up to 2 times.

**OptionalWorker** (`tp_optional`) -- returns `result` = `"optional-done"`. Task definition: `timeoutPolicy` = `ALERT_ONLY`, `responseTimeoutSeconds` = `10`, `retryCount` = `0`. If the worker times out, the task is marked `TIMED_OUT` but the workflow continues.

## Workflow Output

The workflow produces `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `tp_critical`: retryCount=0, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=30, responseTimeoutSeconds=10, timeoutPolicy=TIME_OUT_WF
- `tp_optional`: retryCount=0, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=30, responseTimeoutSeconds=10, timeoutPolicy=ALERT_ONLY
- `tp_retryable`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=1, timeoutSeconds=30, responseTimeoutSeconds=10, timeoutPolicy=RETRY

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `timeout_policy_demo` defines 1 task with input parameters `mode` and a timeout of `30` seconds.

## Tests

6 tests verify that each worker completes within the timeout window and that the three timeout policy configurations are correctly defined for their respective failure scenarios.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
