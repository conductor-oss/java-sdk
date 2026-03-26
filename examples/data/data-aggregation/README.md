# Data Aggregation

A retail analytics platform ingests individual transaction records from 200+ store locations. Before the nightly report runs, the raw transactions need grouping by region, aggregation into sums and averages, and outlier detection. A single corrupted record should not invalidate the entire regional rollup.

## Pipeline

```
[agg_load_data]
     |
     v
[agg_group_by_dimension]
     |
     v
[agg_compute_aggregates]
     |
     v
[agg_format_report]
     |
     v
[agg_emit_results]
```

**Workflow inputs:** `records`, `groupBy`, `aggregateField`

## Workers

**ComputeAggregatesWorker** (task: `agg_compute_aggregates`)

Computes aggregate statistics (count, sum, avg, min, max) for each group on a specified numeric field.

- Reads `groups`, `aggregateField`. Writes `aggregates`

**EmitResultsWorker** (task: `agg_emit_results`)

Emits the final aggregation summary.

- Reads `report`, `groupCount`. Writes `summary`

**FormatReportWorker** (task: `agg_format_report`)

Formats aggregate results into human-readable report lines.

- Reads `aggregates`. Writes `report`

**GroupByDimensionWorker** (task: `agg_group_by_dimension`)

Groups records by a specified dimension field.

- Reads `records`, `groupBy`. Writes `groups`, `groupCount`

**LoadDataWorker** (task: `agg_load_data`)

Loads input records and passes them through with a count.

- Reads `records`. Writes `records`, `count`

---

**41 tests** | Workflow: `data_aggregation_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
