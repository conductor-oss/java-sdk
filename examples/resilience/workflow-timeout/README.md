# Workflow Timeout

Individual task timeouts prevent hung workers, but a workflow with 10 tasks can still run indefinitely if something causes it to stall between tasks -- long queue waits, stuck decision logic, or unexpected loops. A workflow-level timeout sets a hard ceiling on total execution time.

## Workflow

```
wft_fast
```

Two workflow definitions demonstrate different timeout bounds:
- `wf_timeout_demo`: `timeoutSeconds` = `30` (short timeout for quick operations)
- `wf_timeout_long`: `timeoutSeconds` = `3600` (1-hour timeout for batch operations)

Both accept `mode` as input. The task definition for `wft_fast` sets `retryCount` = `2`, `timeoutSeconds` = `60`, and `responseTimeoutSeconds` = `30`.

## Workers

**FastWorker** (`wft_fast`) -- reads `mode` from input (defaults to `"default"` if null or blank). Returns `result` = `"done-" + mode`. Completes immediately, well within either workflow timeout. The point is that the `timeoutSeconds` on the workflow catches scenarios that per-task timeouts miss -- for example, if tasks complete fine individually but the workflow stalls between them.

## Workflow Output

The workflow produces `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `wft_fast`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `wf_timeout_demo` defines 1 task with input parameters `mode` and a timeout of `30` seconds.

## Tests

6 tests verify fast completion within both timeout bounds, mode input handling, default mode behavior, and that the workflow-level `timeoutSeconds` setting is correctly applied independently of task-level timeouts.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
