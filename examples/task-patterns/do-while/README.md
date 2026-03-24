# Do-While Loop

A batch of items needs sequential processing where the count is not known until runtime. The `DO_WHILE` loop iterates based on the `batchSize` input, processing one item per iteration, then a summarize step reports the total.

## Workflow

```
DO_WHILE(loop_ref) ──> dw_summarize
    └── dw_process_item (runs batchSize times)
```

Workflow `do_while_demo` accepts `batchSize`. Times out after `120` seconds.

## Workers

**ProcessItemWorker** (`dw_process_item`) -- reads the current iteration number from task input. Reports processing the item at that iteration.

**SummarizeWorker** (`dw_summarize`) -- receives the total iteration count. Reports summarizing all processed items.

## Workflow Output

The workflow produces `totalProcessed`, `summary` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `dw_process_item`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dw_summarize`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `do_while_demo` defines 2 tasks with input parameters `batchSize` and a timeout of `120` seconds.

## Tests

8 tests verify single-iteration loops, multi-iteration loops, item processing at each iteration, and correct summarization of total items.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
