# Bulk Operations

A batch processing system receives a `batchId` and must process it through two sequential stages -- initial processing and finalization -- with each stage passing its output to the next.

## Workflow

```
bulk_step1 ──> bulk_step2
```

Workflow `bulk_ops_demo` accepts `batchId`. Times out after `60` seconds.

## Workers

**Step1Worker** (`bulk_step1`) -- reads `batchId` from input. Returns `data` = `"batch-" + batchId` and the `batchId`.

**Step2Worker** (`bulk_step2`) -- reads `data` from the previous step. Returns `result` = `"done-" + data`.

## Workflow Output

The workflow produces `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `bulk_step1`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `bulk_step2`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `bulk_ops_demo` defines 2 tasks with input parameters `batchId` and a timeout of `60` seconds.

## Tests

6 tests verify batch processing, data passing between stages, and correct output composition.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
