# Workflow Metadata

A workflow demonstrates how to use metadata fields (`category`, `priority`) for workflow management, search, and filtering. The task worker reads and reports the metadata values.

## Workflow

```
md_task
```

Workflow `metadata_demo` accepts `category` and `priority`. Times out after `60` seconds.

## Workers

**MetadataTaskWorker** (`md_task`) -- reads `category` and `priority` from input. Reports both values for downstream use.

## Workflow Output

The workflow produces `category` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `md_task`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `metadata_demo` defines 1 task with input parameters `category`, `priority` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Demonstrates workflow metadata and search". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

6 tests verify metadata propagation, category and priority handling, and correct output reporting.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
