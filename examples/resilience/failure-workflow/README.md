# Failure Workflow

A processing pipeline fails mid-execution. The system must automatically clean up resources and notify the team without any manual trigger -- the failure handler must receive the original workflow's ID, type, and failure reason.

## Workflow

```
fw_process ──(fails when shouldFail=true)──> failureWorkflow: error_handler_wf
                                                     │
                                              fw_cleanup ──> fw_notify_failure
```

The primary workflow `main_with_failure_handler` runs `fw_process` with `retryCount` = `0` and `failureWorkflow` = `"error_handler_wf"`. The error handler workflow `error_handler_wf` chains `fw_cleanup` then `fw_notify_failure`, both configured with `retryCount` = `2` and `responseTimeoutSeconds` = `30`.

## Workers

**ProcessWorker** (`fw_process`) -- reads `shouldFail` from task input using a `toBoolean()` helper that handles `Boolean`, `String`, and `null` types. When `true`, returns `FAILED` with `reasonForIncompletion` = `"Intentional failure: shouldFail was true"` and outputs `status` = `"FAILED"`. Otherwise returns `status` = `"SUCCESS"`.

**CleanupWorker** (`fw_cleanup`) -- receives `failedWorkflowId`, `failedWorkflowType`, and `reason` from the error handler workflow's input parameters. Returns `cleaned` = `true` and `message` = `"Cleanup completed successfully"`.

**NotifyFailureWorker** (`fw_notify_failure`) -- receives `failedWorkflowId`, `cleaned` status from the cleanup step, and the original `reason`. Returns `notified` = `true` and `message` = `"Failure notification sent"`.

## Workflow Output

The workflow produces `status`, `message` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `fw_process`: retryCount=0, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `fw_cleanup`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `fw_notify_failure`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `main_with_failure_handler` defines 1 task with input parameters `shouldFail` and a timeout of `60` seconds.

## Tests

4 tests verify successful processing, failure triggering, cleanup execution, and notification delivery with correct failure context propagation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
