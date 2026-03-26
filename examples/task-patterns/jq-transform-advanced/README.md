# Advanced JQ Transforms

An order processing pipeline uses three `JSON_JQ_TRANSFORM` tasks to flatten nested order structures, aggregate totals, and classify orders by value tier -- all without custom workers.

## Workflow

```
jq_flatten (JSON_JQ_TRANSFORM) ──> jq_aggregate (JSON_JQ_TRANSFORM) ──> jq_classify (JSON_JQ_TRANSFORM)
```

Workflow `jq_advanced_demo` accepts `orders`. Times out after `60` seconds.

## Workers

No custom workers. All three tasks use Conductor's `JSON_JQ_TRANSFORM` task type with jq expressions for data manipulation.

## Workflow Output

The workflow produces `flattenedOrders`, `customerSummary`, `tieredCustomers` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 0 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `jq_advanced_demo` defines 3 tasks with input parameters `orders` and a timeout of `60` seconds.

## Workflow Definition Details

Workflow description: "Advanced JQ data transformations — flatten orders, aggregate by customer, classify into tiers". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Implementation Notes

All workers implement the `com.netflix.conductor.client.worker.Worker` interface. Input parameters are read from `task.getInputData()` and output is written to `result.getOutputData()`. Workers return `TaskResult.Status.COMPLETED` on success and `TaskResult.Status.FAILED` on failure. The workflow JSON definition in `src/main/resources/` declares the task graph, input wiring via `${ref.output}` expressions, and output parameters.

To swap in production logic, replace the worker method bodies while keeping the same task names and input/output contracts. No workflow definition changes are needed.

## Tests

24 tests verify order flattening, total aggregation, tier classification, and correct jq expression evaluation across various input shapes.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
