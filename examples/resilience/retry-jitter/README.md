# Retry with Jitter

When a shared dependency fails, all workers retry at the same instant. The dependency recovers, gets slammed by simultaneous requests, and fails again. Adding a jitter delay spreads retries over time so the recovering service handles them gradually instead of in a thundering herd.

## Workflow

```
jitter_api_call
```

Workflow `retry_jitter_demo` accepts `endpoint` as input.

## Workers

**JitterApiCallWorker** (`jitter_api_call`) -- maintains an `AtomicInteger` called `callCount`. Reads `endpoint` from input (defaults to `"default"` if absent). Computes a deterministic jitter delay: `jitterMs = Math.abs(endpoint.hashCode() % 500)`, producing a fixed 0-499ms value per endpoint string. Calls `Thread.sleep(jitterMs)` to apply the delay, then returns `result` = `"ok"`, `jitterMs` = the computed delay, and `attempt` = the current call count. The deterministic formula based on `hashCode()` makes the worker fully testable without randomness -- the same endpoint always produces the same jitter value.

## Workflow Output

The workflow produces `result`, `jitterMs`, `attempt` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 1 worker implementation in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `retry_jitter_demo` defines 1 task with input parameters `endpoint` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Retry with jitter demo -- adds jitter delay before API calls to avoid thundering herd.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Implementation Notes

All workers implement the `com.netflix.conductor.client.worker.Worker` interface. Input parameters are read from `task.getInputData()` and output is written to `result.getOutputData()`. Workers return `TaskResult.Status.COMPLETED` on success and `TaskResult.Status.FAILED` on failure. The workflow JSON definition in `src/main/resources/` declares the task graph, input wiring via `${ref.output}` expressions, and output parameters.

To swap in production logic, replace the worker method bodies while keeping the same task names and input/output contracts. No workflow definition changes are needed.

## Tests

9 tests verify that the jitter value is deterministic per endpoint, that different endpoints produce different jitter values, that the delay is bounded to 0-499ms, and that the call counter increments correctly across invocations.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
