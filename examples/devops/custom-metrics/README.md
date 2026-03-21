# Custom Metrics Pipeline in Java with Conductor : Define, Collect, Aggregate, Dashboard Update

Automates custom metrics pipelines using [Conductor](https://github.com/conductor-oss/conductor). This workflow defines custom metric definitions, collects raw data points for those metrics, aggregates them over a time window (sum, average, percentiles), and updates dashboards with the results.

## Business Metrics That Infrastructure Tools Cannot See

Your standard monitoring covers CPU, memory, and request latency. But the business needs to track checkout conversion rate, cart abandonment by region, and API quota usage per tenant. These custom metrics require defining what to measure, collecting the raw events, aggregating them into meaningful numbers over time windows, and pushing the results to a dashboard the team actually watches.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the metric definitions and aggregation logic. Conductor handles the define-collect-aggregate-display pipeline and tracks every collection cycle.**

`DefineMetricsWorker` specifies the metrics to collect. name, data source, collection interval, aggregation method (count, average, p99), and retention period. `CollectDataWorker` gathers raw data points from the configured sources, parsing application logs, querying databases, or consuming event streams. `AggregateWorker` computes aggregated values for each metric using the specified method, rolling averages, percentile calculations, rate computations. `UpdateDashboardWorker` pushes the aggregated metrics to monitoring dashboards in the appropriate format. Conductor records each collection and aggregation cycle for metrics pipeline health monitoring.

### What You Write: Workers

Four workers manage custom metrics. Defining what to measure, collecting raw data, aggregating over time windows, and updating dashboards.

| Worker | Task | What It Does |
|---|---|---|
| **Aggregate** | `cus_aggregate` | Aggregates raw data points over the specified window. |
| **CollectData** | `cus_collect_data` | Collects data points for registered custom metrics. |
| **DefineMetrics** | `cus_define_metrics` | Registers custom metric definitions. |
| **UpdateDashboard** | `cus_update_dashboard` | Updates the dashboard with aggregated metrics. |

the workflow and rollback logic stay the same.

### The Workflow

```
Input -> Aggregate -> CollectData -> DefineMetrics -> UpdateDashboard -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
