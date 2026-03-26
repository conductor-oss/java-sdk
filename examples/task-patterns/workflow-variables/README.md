# Workflow Variables

An order processing pipeline uses workflow-level variables and INLINE tasks to share state across steps. The price calculator computes the item total, an inline task applies a tier discount, the shipping calculator checks for free shipping eligibility, and a summary builder assembles the final result.

## Workflow

```
wv_calc_price ──> apply_tier_discount (INLINE) ──> wv_calc_shipping ──> wv_build_summary
```

Workflow `workflow_variables_demo` accepts `orderId`, `items`, and `customerTier`. Times out after `60` seconds.

## Workers

**CalcPriceWorker** (`wv_calc_price`) -- computes the total from the item list. Reports item count and dollar amount.

**CalcShippingWorker** (`wv_calc_shipping`) -- determines shipping cost and free shipping eligibility.

**BuildSummaryWorker** (`wv_build_summary`) -- assembles the final order summary from all preceding steps.

The `apply_tier_discount` task uses Conductor's `INLINE` type.

## Workflow Output

The workflow produces `orderId`, `summary`, `finalTotal` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `wv_calc_price`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `wv_calc_shipping`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `wv_build_summary`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `workflow_variables_demo` defines 4 tasks with input parameters `orderId`, `items`, `customerTier` and a timeout of `60` seconds.

## Tests

6 tests verify price calculation, tier discount application, shipping computation, and summary assembly.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
