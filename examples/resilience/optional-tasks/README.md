# Optional Tasks

A data pipeline has three stages: required processing, optional enrichment, and summarization. The enrichment step adds metadata but is not critical. If it fails, the summarize step must detect the missing data and produce a degraded summary rather than failing the entire pipeline.

## Workflow

```
opt_required ──> opt_optional_enrich (optional: true) ──> opt_summarize
```

Workflow `optional_tasks_demo` accepts `data` as input. The enrichment task `opt_optional_enrich_ref` has `optional` = `true` in the workflow definition, so its failure does not fail the workflow.

## Workers

**RequiredWorker** (`opt_required`) -- reads `data` from input (defaults to empty string if `null`). Returns `result` = `"processed-" + data`. Always completes.

**OptionalEnrichWorker** (`opt_optional_enrich`) -- receives `processedData` from `${opt_required_ref.output.result}`. Returns `enriched` = `"extra-data-added"`. When this task fails, the workflow continues because `optional` is `true`.

**SummarizeWorker** (`opt_summarize`) -- receives `processedData` from the required worker and `enrichedData` from the optional enrichment. If `enrichedData` is non-null and non-empty, returns `summary` = `"Processed: " + processedData + ", Enriched: " + enrichedData`. If enrichment was skipped, returns `summary` = `"Processed: " + processedData + ", Enrichment: skipped"`.

## Workflow Output

The workflow produces `summary` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `optional_tasks_demo` defines 3 tasks with input parameters `data` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Optional task demo -- required task, optional enrichment (continues on failure), and summarize that handles missing data.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

8 tests verify the full enrichment path, the degraded path when enrichment fails, that the summary correctly reflects whether enrichment data was present, and that the `optional` flag allows the workflow to continue past enrichment failures.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
