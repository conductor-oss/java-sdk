# Transient vs Permanent Error Detection

A worker encounters two categories of errors: transient ones (network timeouts, 503s) that will likely resolve on retry, and permanent ones (404s, authentication failures) where retrying wastes resources. The worker must signal which category applies so the orchestrator either retries with backoff or fails immediately.

## Workflow

```
tvp_smart_task
```

Workflow `transient_vs_permanent_demo` accepts `errorType` as input.

## Workers

**SmartTaskWorker** (`tvp_smart_task`) -- maintains an `AtomicInteger` called `attemptCounter`. Reads `errorType` from input and branches:

- `"permanent"`: returns `FAILED_WITH_TERMINAL_ERROR` immediately with `error` = `"Permanent error -- no retries"`. This tells Conductor to skip all remaining retries.
- `"transient"`: for attempts `< 3`, returns `FAILED` with `error` = `"Transient error on attempt N"`. On attempt 3+, returns `COMPLETED` with `result` = `"success after transient errors"`. Conductor retries with the task definition's `retryCount` = `3`, `retryLogic` = `FIXED`, `retryDelaySeconds` = `1`.
- default/null: returns `COMPLETED` immediately with `result` = `"success"`.

The key distinction is `FAILED` (retryable) vs `FAILED_WITH_TERMINAL_ERROR` (not retryable). Exposes `reset()` for testing.

## Workflow Output

The workflow produces `attempt`, `result`, `errorType`, `error` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `transient_vs_permanent_demo` defines 1 task with input parameters `errorType` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Transient vs Permanent Error Detection demo -- smart worker classifies errors and uses appropriate failure strategy.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

11 tests verify immediate success, transient failure with recovery after retries, permanent failure with no retries, correct use of `FAILED_WITH_TERMINAL_ERROR`, attempt counting, and error message accuracy.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
