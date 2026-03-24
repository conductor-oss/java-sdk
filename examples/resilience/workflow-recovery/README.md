# Workflow Recovery

A Conductor server restarts mid-execution -- OOM kill, planned maintenance, bad deploy. A batch processing workflow that completed 4,000 of 10,000 orders must resume at order 4,001, not restart from the beginning. The worker itself implements checkpoint-based recovery so that re-delivery of the same batch skips already-processed work.

## Workflow

```
wr_durable_task
```

Workflow `workflow_recovery_demo` accepts `batch` as input.

## Workers

**DurableTaskWorker** (`wr_durable_task`) -- maintains two static `ConcurrentHashMap` stores: `CHECKPOINTS` (batch name to checkpoint ID) and `TIMESTAMPS` (batch name to ISO timestamp). On each execution:

1. Checks if `CHECKPOINTS.get(batch)` returns an existing checkpoint.
2. If yes, returns `resumed` = `true` with the `originalProcessedAt` timestamp and the existing `checkpointId`. No reprocessing occurs.
3. If no checkpoint exists, processes the batch, computes a deterministic checkpoint ID via `SHA-256` hash of `"checkpoint:" + batch` (taking the first 8 bytes formatted as hex with a `"chk-"` prefix), stores both the checkpoint and timestamp, and returns `resumed` = `false`.

Returns `processed` = `true`, `batch`, `checkpointId`, and `processedAt` in all cases. The static `clearCheckpoints()` method resets both maps for testing.

## Workflow Output

The workflow produces `processed`, `batch` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `workflow_recovery_demo` defines 1 task with input parameters `batch` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Workflow Recovery demo -- demonstrates Conductor persistence across server restarts.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

10 tests verify first-time batch processing, checkpoint creation, recovery from existing checkpoints, deterministic checkpoint IDs across runs, timestamp preservation, and the SHA-256 based ID computation.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
