# Network Partitions

A distributed worker loses connectivity to the Conductor server during a cloud provider outage. When the network heals, the worker must reconnect and resume processing without duplicating work. The system needs to track how many reconnection attempts occurred so operators can correlate partition events with infrastructure incidents.

## Workflow

```
np_resilient_task
```

Workflow `network_partitions_demo` accepts `data` as input. The task definition for `np_resilient_task` is configured with `retryCount` = `3`, `retryLogic` = `FIXED`, `retryDelaySeconds` = `1`, `timeoutSeconds` = `60`, and `responseTimeoutSeconds` = `30`.

## Workers

**NetworkPartitionWorker** (`np_resilient_task`) -- maintains an `AtomicInteger` called `attemptCounter` that increments on every invocation. Reads `data` from task input (defaults to empty string if absent). Returns `result` = `"done-" + data` and `attempt` = the current counter value. Exposes `getAttemptCount()` and `reset()` methods for external monitoring and testing. The counter persists across invocations within the same JVM, so reconnection after a partition shows the accumulated attempt count.

## Workflow Output

The workflow produces `result`, `attempt` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `np_resilient_task`: retryCount=3, retryLogic=FIXED, retryDelaySeconds=1, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `network_partitions_demo` defines 1 task with input parameters `data` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Network partitions demo -- resilient worker that tracks attempts and handles reconnection.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

9 tests verify successful processing, attempt counter tracking across multiple invocations, reconnection behavior after simulated partitions, and that the `retryCount`/`retryDelaySeconds` task definition settings are respected.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
