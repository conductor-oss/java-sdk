# Simple Plus System Tasks

A retail analytics workflow mixes custom SIMPLE workers with INLINE system tasks. Custom workers fetch orders and generate visual reports, while INLINE tasks handle statistical calculations and summary formatting -- no external workers needed for the computation steps.

## Workflow

```
fetch_orders (SIMPLE) ──> calculate_stats (INLINE) ──> generate_visual_report (SIMPLE) ──> format_summary (INLINE)
```

Workflow `mixed_tasks_demo` accepts `storeId` and `dateRange`. Times out after `60` seconds.

## Workers

**FetchOrdersWorker** (`fetch_orders`) -- fetches orders for the specified store and date range.

**GenerateVisualReportWorker** (`generate_visual_report`) -- generates a chart from the computed statistics.

The `calculate_stats` and `format_summary` tasks use Conductor's `INLINE` type with JavaScript expressions.

## Workflow Output

The workflow produces `stats`, `summary` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `fetch_orders`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `generate_visual_report`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `mixed_tasks_demo` defines 4 tasks with input parameters `storeId`, `dateRange` and a timeout of `60` seconds.

## Tests

6 tests verify order fetching, inline stat calculation, chart generation, and inline summary formatting.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
