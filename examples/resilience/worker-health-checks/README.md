# Worker Health Checks

Workers are deployed across multiple containers. The operations team needs to know: are they polling? How many tasks have they completed? If a worker stops polling due to a crash or network issue, tasks queue up and workflows stall. Health metrics must be tracked at the worker level without requiring external monitoring infrastructure.

## Workflow

```
whc_task
```

Workflow `worker_health_checks_demo` accepts `data` as input.

## Workers

**WhcWorker** (`whc_task`) -- maintains two `AtomicInteger` counters: `pollCount` (incremented when a task is picked up) and `completedCount` (incremented after successful completion). Reads `data` from input (defaults to empty string if null). Returns `result` = `"done-" + data`, `pollCount` = the current poll counter, and `completedCount` = the current completion counter. Exposes `getPollCount()` and `getCompletedCount()` methods for external health monitoring systems to query. Both counters are thread-safe and persist across invocations within the same JVM.

## Workflow Output

The workflow produces `result`, `pollCount`, `completedCount` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `worker_health_checks_demo` defines 1 task with input parameters `data` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Worker health checks demo -- runs whc_task and demonstrates health monitoring APIs.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Implementation Notes

All workers implement the `com.netflix.conductor.client.worker.Worker` interface. Input parameters are read from `task.getInputData()` and output is written to `result.getOutputData()`. Workers return `TaskResult.Status.COMPLETED` on success and `TaskResult.Status.FAILED` on failure. The workflow JSON definition in `src/main/resources/` declares the task graph, input wiring via `${ref.output}` expressions, and output parameters.

To swap in production logic, replace the worker method bodies while keeping the same task names and input/output contracts. No workflow definition changes are needed.

## Tests

9 tests verify task processing, poll counter accuracy, completion counter accuracy, counter consistency across multiple invocations, and that the health metrics correctly reflect worker activity over time.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
