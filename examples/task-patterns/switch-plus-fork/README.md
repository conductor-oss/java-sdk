# Switch Plus Fork

A workflow combines SWITCH routing with FORK parallel execution. Based on `type`, the workflow either processes a single item or forks into two parallel lanes (A and B) for batch processing. Both paths converge at a finalize step.

## Workflow

```
SWITCH(type)
  ├── "single" ──> single_process
  ├── "parallel" ──> FORK(sf_process_a, sf_process_b) ──> JOIN
  └── default ──> (empty)
                    │
              sf_finalize
```

Workflow `switch_fork_demo` accepts `type` and `items`. Times out after `60` seconds.

## Workers

**SingleProcessWorker** (`single`) -- processes a single item.

**ProcessAWorker** (`sf_process_a`) -- lane A processes items in parallel.

**ProcessBWorker** (`sf_process_b`) -- lane B processes items in parallel.

**FinalizeWorker** (`sf_finalize`) -- finalizes after either single or parallel processing.

## Workflow Output

The workflow produces `type`, `done` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `sf_process_a`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sf_process_b`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sf_single_process`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sf_finalize`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `switch_fork_demo` defines 2 tasks with input parameters `type`, `items` and a timeout of `60` seconds.

## Tests

5 tests verify single-item routing, parallel lane execution, and post-switch/fork finalization.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
