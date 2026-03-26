# Inline Tasks

Four computation steps run entirely within Conductor using `INLINE` tasks -- no external workers needed. The pipeline performs math aggregation, string manipulation, conditional logic, and response building, all using JavaScript expressions evaluated server-side.

## Workflow

```
math_aggregation (INLINE) ──> string_manipulation (INLINE) ──> conditional_logic (INLINE) ──> build_response (INLINE)
```

Workflow `inline_tasks_demo` accepts `numbers`, `text`, and `config`. Times out after `60` seconds.

## Workers

No custom workers. All four tasks use Conductor's `INLINE` task type with JavaScript expressions.

## Workflow Output

The workflow produces `math`, `text`, `classification`, `summary` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 0 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `inline_tasks_demo` defines 4 tasks with input parameters `numbers`, `text`, `config` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Demonstrates INLINE tasks — JavaScript that runs on the Conductor server with no workers". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Implementation Notes

All workers implement the `com.netflix.conductor.client.worker.Worker` interface. Input parameters are read from `task.getInputData()` and output is written to `result.getOutputData()`. Workers return `TaskResult.Status.COMPLETED` on success and `TaskResult.Status.FAILED` on failure. The workflow JSON definition in `src/main/resources/` declares the task graph, input wiring via `${ref.output}` expressions, and output parameters.

To swap in production logic, replace the worker method bodies while keeping the same task names and input/output contracts. No workflow definition changes are needed.

## Tests

17 tests verify math operations on number arrays, string transformations, conditional branching based on config values, and response assembly from inline results.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
