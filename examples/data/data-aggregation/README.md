# Data Aggregation in Java Using Conductor: Group-By, Statistical Computation, and Report Generation

The VP of Sales opens the regional revenue dashboard Monday morning and it takes 45 seconds to load. The dashboard runs a query that scans 12 million raw transaction rows, groups them by region, and computes sum/average/min/max: live, on every page load. By Wednesday, when the table has grown by another 2 million rows, the query times out entirely. So someone adds a materialized view, but it's stale by the time the next sales call happens. The real problem is that aggregation is happening at read time against raw data, instead of being computed once, stored, and served instantly. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You have a dataset of records. Sales transactions, sensor readings, user events, and you need to answer questions like "what's the average revenue per region?" or "what's the total count per department?" That means loading the dataset, grouping rows by a specified dimension (region, category, department), computing statistical aggregates (count, sum, average, min, max) on a numeric field for each group, formatting the results into a readable report, and delivering the output. Each step depends on the previous one: you can't compute aggregates without groups, and you can't group without loaded data.

Without orchestration, you'd write a single method that reads data, does the grouping with `Collectors.groupingBy`, computes stats inline, formats output, and returns everything. If the data source times out, you'd add manual retry logic. If the process crashes after expensive computation but before emitting results, that work is lost. Changing the grouping dimension or adding a new aggregate function (median, percentiles) means touching deeply coupled code with no visibility into which step is slow.

## The Solution

**You just write the data loading, grouping, aggregation, formatting, and emission workers. Conductor handles the load-group-aggregate-format-emit sequence, retries on data source failures, and detailed observability into record counts at every stage.**

Each stage of the aggregation pipeline is a simple, independent worker. The loader reads records from the data source. The grouper partitions records by the specified dimension field. The aggregator computes count, sum, average, min, and max for each group on the specified numeric field. The formatter turns raw aggregates into human-readable report lines. The emitter delivers the final output. Conductor executes them in sequence, passes grouped data between steps, retries if a data source query fails, and resumes from exactly where it left off if the process crashes.

### What You Write: Workers

Five workers cover the full aggregation lifecycle: loading records, grouping by a configurable dimension, computing statistical aggregates (count, sum, average, min, max), formatting results, and emitting the final report.

| Worker | Task | What It Does |
|---|---|---|
| **ComputeAggregatesWorker** | `agg_compute_aggregates` | Computes aggregate statistics (count, sum, avg, min, max) for each group on a specified numeric field. |
| **EmitResultsWorker** | `agg_emit_results` | Emits the final aggregation summary. |
| **FormatReportWorker** | `agg_format_report` | Formats aggregate results into human-readable report lines. |
| **GroupByDimensionWorker** | `agg_group_by_dimension` | Groups records by a specified dimension field. |
| **LoadDataWorker** | `agg_load_data` | Loads input records and passes them through with a count. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### The Workflow

```
agg_load_data
 │
 ▼
agg_group_by_dimension
 │
 ▼
agg_compute_aggregates
 │
 ▼
agg_format_report
 │
 ▼
agg_emit_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
