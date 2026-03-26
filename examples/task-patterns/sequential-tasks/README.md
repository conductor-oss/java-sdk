# Sequential Tasks (ETL)

A classic Extract-Transform-Load pipeline. The extract worker pulls data from a source, the transform worker processes and reshapes it, and the load worker writes the transformed records to the destination.

## Workflow

```
seq_extract ──> seq_transform ──> seq_load
```

Workflow `sequential_etl` accepts `source` and `format`. Times out after `60` seconds.

## Workers

**ExtractWorker** (`seq_extract`) -- reads `source` from input. Extracts data from the specified source.

**TransformWorker** (`seq_transform`) -- reads extracted data from the previous step. Transforms and reshapes the records.

**LoadWorker** (`seq_load`) -- reads transformed data. Loads the records and reports the count.

## Workflow Output

The workflow produces `loaded`, `totalRecords`, `source` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `seq_extract`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `seq_transform`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `seq_load`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `sequential_etl` defines 3 tasks with input parameters `source`, `format` and a timeout of `60` seconds.

## Tests

7 tests verify extraction, transformation, loading, and correct data flow through the ETL pipeline.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
