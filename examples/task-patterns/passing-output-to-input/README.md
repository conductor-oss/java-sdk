# Passing Output to Input

A report generation pipeline demonstrates Conductor's data wiring. The first worker generates raw data, the second enriches it with revenue figures, and the third summarizes with growth rate and health status. Each step's output feeds directly into the next step's input via `${ref.output}` expressions.

## Workflow

```
generate_report ──> enrich_report ──> summarize_report
```

Workflow `data_wiring_demo` accepts `region` and `period`. Times out after `60` seconds.

## Workers

**GenerateReportWorker** (`generate_report`) -- reads `region` and `period`. Generates raw report data.

**EnrichReportWorker** (`enrich_report`) -- adds revenue figures to the generated report.

**SummarizeReportWorker** (`summarize_report`) -- computes `growthRate` percentage and health status from the enriched data.

## Workflow Output

The workflow produces `summary`, `recommendation` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `generate_report`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `enrich_report`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `summarize_report`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `data_wiring_demo` defines 3 tasks with input parameters `region`, `period` and a timeout of `60` seconds.

## Tests

6 tests verify data generation, enrichment, summarization, and that output-to-input wiring correctly propagates data through the pipeline.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
