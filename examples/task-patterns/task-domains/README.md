# Task Domains

A task worker registers under a specific domain (e.g., `gpu`), enabling Conductor to route tasks to workers with specific capabilities. The workflow demonstrates how `taskDomain` configuration directs work to specialized worker pools.

## Workflow

```
td_process
```

Workflow `task_domain_demo` accepts `data`. Times out after `60` seconds.

## Workers

**TdProcessWorker** (`td_process`) -- reads the processing `type` from input. Reports the processing type it handles, demonstrating domain-based routing.

## Workflow Output

The workflow produces `result`, `worker` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `td_process`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `task_domain_demo` defines 1 task with input parameters `data` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Task Domains demo — route tasks to specific worker groups using domains.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

8 tests verify domain-based task routing, worker capability matching, and correct processing output.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
