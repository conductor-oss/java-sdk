# External Payload

Large data payloads exceed Conductor's inline message size limit. The generate worker produces a summary for a configurable number of records, and the process worker handles it downstream. This pattern demonstrates passing large data through external storage.

## Workflow

```
ep_generate ──> ep_process
```

Workflow `large_payload_demo` accepts `dataSize`. Times out after `60` seconds.

## Workers

**GenerateWorker** (`ep_generate`) -- reads `dataSize` from input. Generates a summary for the specified number of records.

**ProcessWorker** (`ep_process`) -- reads the record count from the generated output. Processes the summary data.

## Workflow Output

The workflow produces `summary`, `storageRef`, `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `ep_generate`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ep_process`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `large_payload_demo` defines 2 tasks with input parameters `dataSize` and a timeout of `60` seconds.

## Tests

8 tests verify payload generation at various sizes, data transfer between workers, and correct processing of large payloads.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
