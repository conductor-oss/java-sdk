# Nested Sub-Workflows

An order processing system uses nested sub-workflows for fraud checking and payment processing. The main workflow calls a fraud check sub-workflow, which itself may call other sub-workflows, demonstrating multi-level workflow composition.

## Workflow

```
nest_check_fraud ──> nest_charge ──> nest_fulfill
```

## Workers

**CheckFraudWorker** (`nest_check_fraud`) -- reads `email` and `amount`. Checks fraud indicators and returns the assessment.

**ChargeWorker** (`nest_charge`) -- reads the `amount`. Charges the customer and returns the charge confirmation.

**FulfillWorker** (`nest_fulfill`) -- reads the `orderId`. Fulfills the order and returns the fulfillment status.

## Task Configuration

- `nest_check_fraud`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `nest_charge`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `nest_fulfill`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `?` defines 0 tasks with input parameters none and a timeout of `?` seconds.

## Implementation Notes

Workers are implemented as standard Conductor `Worker` interface implementations. Each worker reads input from `task.getInputData()`, performs its logic, and writes results to `result.getOutputData()`. All workers return `TaskResult.Status.COMPLETED` on success.

## Tests

7 tests verify fraud checking, payment charging, order fulfillment, and correct sub-workflow nesting and data passing.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
