# Workflow Archival

A batch processing workflow demonstrates Conductor's workflow archival feature. After execution completes, the workflow state can be archived for long-term storage, reducing the active database footprint while preserving the execution history.

## Workflow

```
arch_task
```

Workflow `archival_demo` accepts `batch`. Times out after `60` seconds.

## Workers

**ArchivalTaskWorker** (`arch_task`) -- processes the specified batch.

## Workflow Output

The workflow produces `done` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `arch_task`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `archival_demo` defines 1 task with input parameters `batch` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Archival demo workflow — single task for demonstrating cleanup policies.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

6 tests verify batch processing, workflow completion, and archival configuration.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
