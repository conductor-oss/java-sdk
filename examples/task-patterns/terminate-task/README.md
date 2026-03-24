# Terminate Task

An order validation pipeline uses a SWITCH task to check the validation result. If validation fails, the workflow terminates early using a `TERMINATE` task instead of proceeding to processing. This prevents invalid orders from reaching downstream steps.

## Workflow

```
term_validate ──> SWITCH(check_validation)
                    ├── valid ──> term_process
                    └── invalid ──> TERMINATE
```

Workflow `terminate_demo` accepts `orderId`, `amount`, and `currency`. Times out after `60` seconds.

## Workers

**ValidateWorker** (`term_validate`) -- validates the order by checking `orderId`, `amount`, and `currency`. Reports the validation outcome.

**ProcessWorker** (`term_process`) -- processes the validated order. Only runs if validation passes.

## Workflow Output

The workflow produces `orderId`, `status`, `processedAmount` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `term_validate`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `term_process`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `terminate_demo` defines 3 tasks with input parameters `orderId`, `amount`, `currency` and a timeout of `60` seconds.

## Tests

13 tests verify successful validation and processing, invalid order termination, various validation failure scenarios, and that terminated workflows do not execute downstream tasks.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
