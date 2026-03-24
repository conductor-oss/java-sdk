# Synchronous Execution

A simple arithmetic workflow demonstrates Conductor's synchronous execution mode, where the caller blocks until the workflow completes and receives the result inline. The workflow adds two numbers and returns the sum.

## Workflow

```
sync_add
```

Workflow `sync_exec_demo` accepts `a` and `b`. Times out after `60` seconds.

## Workers

**AddWorker** (`sync_add`) -- reads `a` and `b` as numbers. Computes the sum and returns the result.

## Workflow Output

The workflow produces `sum` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `sync_add`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `sync_exec_demo` defines 1 task with input parameters `a`, `b` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Simple workflow for demonstrating sync vs async execution". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

10 tests verify synchronous execution, addition of various number pairs, input validation, and inline result delivery.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
