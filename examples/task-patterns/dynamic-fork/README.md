# Dynamic Fork

A list of URLs needs to be fetched in parallel, but the number of URLs is not known until runtime. The prepare worker converts the URL list into dynamic task definitions, `FORK_JOIN_DYNAMIC` fans them out, and the aggregate worker combines results.

## Workflow

```
df_prepare_tasks ──> FORK_JOIN_DYNAMIC ──> JOIN ──> df_aggregate
```

Workflow `dynamic_fork_demo` accepts `urls`. Times out after `120` seconds.

## Workers

**PrepareTasksWorker** (`df_prepare_tasks`) -- converts the `urls` input list into dynamic task definitions. Reports preparing N dynamic tasks.

**FetchUrlWorker** (`df_fetch_url`) -- fetches a single URL by index. Reports fetching URL #N.

**AggregateWorker** (`df_aggregate`) -- combines all fetched results. Reports the total result count and aggregate `totalSize`.

## Workflow Output

The workflow produces `results`, `totalProcessed`, `totalSize` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `df_prepare_tasks`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `df_fetch_url`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `df_aggregate`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `dynamic_fork_demo` defines 4 tasks with input parameters `urls` and a timeout of `120` seconds.

## Tests

8 tests verify dynamic task preparation, parallel URL fetching, result aggregation, and correct handling of varying URL list sizes.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
