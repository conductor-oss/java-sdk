# Idempotent Start

An order processing system may receive duplicate start requests for the same `orderId`. The workflow uses `idempotencyKey` to ensure only one execution runs per order, regardless of how many times the start API is called.

## Workflow

```
idem_process
```

Workflow `idempotent_demo` accepts `orderId` and `amount`. Times out after `60` seconds.

## Workers

**IdemProcessWorker** (`idem_process`) -- reads `orderId` and `amount`. Processes the order and returns the result.

## Workflow Output

The workflow produces `orderId`, `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `idem_process`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `idempotent_demo` defines 1 task with input parameters `orderId`, `amount` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Idempotent start demo — demonstrates correlationId-based dedup and search-based idempotency.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

9 tests verify single execution, duplicate start detection via idempotency key, and correct order processing output.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
