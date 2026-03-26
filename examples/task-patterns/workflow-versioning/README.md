# Workflow Versioning

Two versions of a calculation workflow run side by side. Version 1 has two steps (calculate then audit). Version 2 adds a bonus step between them. This demonstrates running multiple workflow versions simultaneously without disrupting existing executions.

## Workflow

```
Version 1: ver_calc ──> ver_audit
Version 2: ver_calc ──> ver_bonus ──> ver_audit
```

## Workers

**VerCalcWorker** (`ver_calc`) -- reads a `value` and multiplies by 2. Returns the calculated result.

**VerBonusWorker** (`ver_bonus`) -- adds 10 to the base result. Only runs in version 2.

**VerAuditWorker** (`ver_audit`) -- audits the final result. Returns `audited` = `true`.

## Task Configuration

- `ver_calc`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ver_bonus`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ver_audit`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `?` defines 0 tasks with input parameters none and a timeout of `?` seconds.

## Implementation Notes

Workers are implemented as standard Conductor `Worker` interface implementations. Each worker reads input from `task.getInputData()`, performs its logic, and writes results to `result.getOutputData()`. All workers return `TaskResult.Status.COMPLETED` on success.

## Tests

5 tests verify version 1 execution, version 2 execution with the bonus step, and that both versions produce correct but different outputs for the same input.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
