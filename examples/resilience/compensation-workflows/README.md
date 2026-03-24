# Compensation Workflows

A three-step provisioning pipeline creates a temp file, inserts a database record, and sends a notification. When step C fails, the earlier resources must be cleaned up -- the file deleted and the record removed -- without manual intervention.

## Workflow

```
comp_step_a ──> comp_step_b ──> comp_step_c
                                     │ (fails when failAtStep="C")
                                     ▼
                              comp_undo_b ──> comp_undo_a
```

Workflow `compensatable_workflow` accepts `failAtStep` as input and times out after `30` seconds.

## Workers

**CompStepAWorker** (`comp_step_a`) -- creates a real temporary file via `Files.createTempFile("comp-resource-A-", ".txt")` and writes a timestamp string into it. Returns the absolute path in `resourcePath` so downstream undo logic can locate the file.

**CompStepBWorker** (`comp_step_b`) -- inserts a record into a static `ConcurrentHashMap<String, String>` called `RECORDS`. The key is `"record-B-" + System.nanoTime()`, the value is `"Inserted at " + Instant.now()`. Returns the `recordKey` for later removal.

**CompStepCWorker** (`comp_step_c`) -- checks `failAtStep` from task input. If the value equals `"C"`, returns `FAILED` with reason `"External service unavailable"`. Otherwise completes with `result` = `"notification-C-sent"`.

**CompUndoBWorker** (`comp_undo_b`) -- receives the `original` record key from input, calls `CompStepBWorker.RECORDS.remove(key)`, and reports whether the removal succeeded in the `removed` output flag.

**CompUndoAWorker** (`comp_undo_a`) -- receives the `original` file path from input. If the path string contains `"comp-resource-A-"`, it calls `Files.deleteIfExists(path)` and reports the `deleted` flag. This confirms the temp file is actually cleaned up on disk.

## Workflow Output

The workflow produces `stepA`, `stepB`, `stepC` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 5 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `compensatable_workflow` defines 3 tasks with input parameters `failAtStep` and a timeout of `30` seconds.

## Tests

4 tests verify the happy path, the compensation path when step C fails, and that undo workers correctly reverse real side effects.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
