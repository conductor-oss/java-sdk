# Per-Task Retry Configuration

An order pipeline has three steps with different failure characteristics: validation rarely fails (1 retry is enough), payment hits rate limits (needs 5 retries with exponential backoff), and notification has transient SMTP issues (needs 3 fixed retries). A single retry policy would either waste time on validation or give up too early on payment.

## Workflow

```
ptr_validate ──> ptr_payment ──> ptr_notify
```

Workflow `per_task_retry_demo` accepts `orderId` and times out after `300` seconds.

## Workers

**PtrValidate** (`ptr_validate`) -- reads `orderId` from input. Returns `result` = `"valid"` and the `orderId`. Task definition: `retryCount` = `1`, `retryLogic` = `FIXED`, `retryDelaySeconds` = `1`, `responseTimeoutSeconds` = `30`.

**PtrPayment** (`ptr_payment`) -- reads `orderId` from input and tracks the attempt via `task.getRetryCount() + 1`. Returns `result` = `"charged"`, `attempt` = the current attempt number, and the `orderId`. Task definition: `retryCount` = `5`, `retryLogic` = `EXPONENTIAL_BACKOFF`, `retryDelaySeconds` = `1`, `responseTimeoutSeconds` = `60`.

**PtrNotify** (`ptr_notify`) -- reads `orderId` from input. Returns `result` = `"email_sent"` and the `orderId`. Task definition: `retryCount` = `3`, `retryLogic` = `FIXED`, `retryDelaySeconds` = `2`, `responseTimeoutSeconds` = `30`.

## Workflow Output

The workflow produces `validation`, `payment`, `notification` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `ptr_validate`: retryCount=1, retryLogic=FIXED, retryDelaySeconds=1, timeoutSeconds=60, responseTimeoutSeconds=30
- `ptr_payment`: retryCount=5, retryLogic=EXPONENTIAL_BACKOFF, retryDelaySeconds=1, timeoutSeconds=120, responseTimeoutSeconds=60
- `ptr_notify`: retryCount=3, retryLogic=FIXED, retryDelaySeconds=2, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `per_task_retry_demo` defines 3 tasks with input parameters `orderId` and a timeout of `300` seconds.

## Tests

4 tests verify that each worker completes with the correct output and that the per-task retry configurations are applied independently to each step.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
