# Graceful Degradation

An order processing pipeline has one required step (create the order) and two optional enhancements (enrichment and analytics). When the enrichment API or analytics service goes down on Black Friday, the order must still complete -- but the output should flag which services were unavailable so downstream systems know the data is incomplete.

## Workflow

```
gd_core_process ──> FORK ──┬── gd_enrich ──────┐
                            └── gd_analytics ───┤
                                                JOIN
                                                 │
                                          gd_finalize
```

Workflow `graceful_degradation_demo` accepts `data`, `enrichAvailable`, and `analyticsAvailable`. The `FORK_JOIN` runs enrichment and analytics in parallel, then `JOIN` waits on `enrich_ref` and `analytics_ref`. The finalize step reads both outputs.

## Workers

**CoreProcessWorker** (`gd_core_process`) -- reads `data` from input (defaults to `"default"` if absent). Returns `result` = `"processed-" + data`. Always completes.

**EnrichWorker** (`gd_enrich`) -- reads `available` from input (defaults to `true`). Returns `enriched` = `true` when available, `enriched` = `false` when the service is down.

**AnalyticsWorker** (`gd_analytics`) -- reads `available` from input (defaults to `true`). Returns `tracked` = `true` when available, `tracked` = `false` when the service is down.

**FinalizeWorker** (`gd_finalize`) -- reads `enriched` and `analytics` booleans from input (both default to `false`). Computes `degraded` = `!enriched || !analytics`. Returns all three flags so callers know whether the response includes full or partial data.

## Workflow Output

The workflow produces `coreResult`, `enriched`, `analytics`, `degraded` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `graceful_degradation_demo` defines 4 tasks with input parameters `data`, `enrichAvailable`, `analyticsAvailable` and a timeout of `120` seconds.

## Tests

7 tests verify full-service operation, enrichment-down degradation, analytics-down degradation, both-down degradation, and that the `degraded` flag is set correctly in each scenario.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
