# Signals (Wait Tasks)

An order workflow pauses at two points waiting for external signals -- once for shipping confirmation and once for delivery confirmation. `WAIT` tasks suspend the workflow until a signal is received via Conductor's API.

## Workflow

```
sig_prepare ──> WAIT(wait_shipping) ──> sig_process_shipping ──> WAIT(wait_delivery) ──> sig_complete
```

Workflow `signal_demo` accepts `orderId`. Times out after `120` seconds.

## Workers

**SigPrepareWorker** (`sig_prepare`) -- prepares the order for processing.

**SigProcessShippingWorker** (`sig_process_shipping`) -- processes the shipping details after the shipping signal is received.

**SigCompleteWorker** (`sig_complete`) -- completes the order after the delivery signal is received.

## Workflow Output

The workflow produces `orderId`, `tracking`, `delivered` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `sig_prepare`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sig_process_shipping`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sig_complete`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `signal_demo` defines 5 tasks with input parameters `orderId` and a timeout of `120` seconds.

## Tests

6 tests verify order preparation, wait task suspension, signal reception, shipping processing, and delivery completion.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
