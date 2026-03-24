# System Tasks

A user bonus calculation pipeline uses only Conductor system tasks -- no custom workers. An `INLINE` task looks up the user, another `INLINE` task calculates the bonus, and a `JSON_JQ_TRANSFORM` formats the output.

## Workflow

```
lookup_user (INLINE) ──> calculate_bonus (INLINE) ──> format_output (JSON_JQ_TRANSFORM)
```

Workflow `system_tasks_demo` accepts `userId`. Times out after `60` seconds.

## Workers

No custom workers. All three tasks use Conductor system task types: two `INLINE` tasks with JavaScript and one `JSON_JQ_TRANSFORM` with jq expressions.

## Workflow Output

The workflow produces `summary` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 0 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `system_tasks_demo` defines 3 tasks with input parameters `userId` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Demonstrates INLINE and JSON_JQ_TRANSFORM system tasks — no workers needed". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Implementation Notes

All workers implement the `com.netflix.conductor.client.worker.Worker` interface. Input parameters are read from `task.getInputData()` and output is written to `result.getOutputData()`. Workers return `TaskResult.Status.COMPLETED` on success and `TaskResult.Status.FAILED` on failure. The workflow JSON definition in `src/main/resources/` declares the task graph, input wiring via `${ref.output}` expressions, and output parameters.

To swap in production logic, replace the worker method bodies while keeping the same task names and input/output contracts. No workflow definition changes are needed.

## Tests

19 tests verify user lookup, bonus calculation logic, jq output formatting, and various userId inputs.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
