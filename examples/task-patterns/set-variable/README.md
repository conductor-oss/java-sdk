# Set Variable

A workflow uses `SET_VARIABLE` system tasks to store intermediate results that can be accessed by any downstream task. The pipeline processes items, stores item results, applies business rules, stores rule results, and finalizes using all accumulated variables.

## Workflow

```
sv_process_items ──> SET_VARIABLE(store_item_results) ──> sv_apply_rules ──> SET_VARIABLE(store_rule_results) ──> sv_finalize
```

Workflow `set_variable_demo` accepts `items`. Times out after `60` seconds.

## Workers

**ProcessItemsWorker** (`sv_process_items`) -- processes items and reports the count and total dollar amount.

**ApplyRulesWorker** (`sv_apply_rules`) -- reads `category` from input. Applies business rules based on the category.

**FinalizeWorker** (`sv_finalize`) -- reads all accumulated variables. Produces the final result.

## Workflow Output

The workflow produces `decision`, `totalAmount`, `itemCount`, `category`, `needsApproval`, `riskLevel` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `sv_process_items`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sv_apply_rules`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `sv_finalize`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `set_variable_demo` defines 5 tasks with input parameters `items` and a timeout of `60` seconds.

## Tests

6 tests verify item processing, variable storage, rule application, and finalization using stored variables.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
