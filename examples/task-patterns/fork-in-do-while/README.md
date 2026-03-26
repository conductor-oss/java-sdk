# Fork Inside Do-While

A batch processing pipeline uses a `DO_WHILE` loop where each iteration processes a batch item. After all iterations complete, a summary worker reports the results. This demonstrates combining loop and fork patterns.

## Workflow

```
DO_WHILE(batch_loop) ──> fl_summary
    └── fl_process_batch (runs totalBatches times)
```

Workflow `fork_loop_demo` accepts `totalBatches`. Times out after `120` seconds.

## Workers

**ProcessBatchWorker** (`fl_process_batch`) -- reads the current iteration number and computes a batch ID. Reports processing the batch.

**SummaryWorker** (`fl_summary`) -- receives the accumulated summary. Reports the final results.

## Workflow Output

The workflow produces `totalIterations`, `summary` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `fl_process_batch`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `fl_summary`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `fork_loop_demo` defines 2 tasks with input parameters `totalBatches` and a timeout of `120` seconds.

## Tests

9 tests verify single-batch loops, multi-batch loops, batch ID generation per iteration, and correct summary aggregation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
