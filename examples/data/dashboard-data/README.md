# Dashboard Data in Java Using Conductor : Metric Aggregation, KPI Computation, Widget Assembly, and Caching

## The Problem

You need to power a real-time dashboard that shows business metrics. Revenue trends, user activity, conversion rates, error counts. That means querying multiple data sources to aggregate raw metrics over a configurable time range, computing derived KPIs (growth rates, percentages, comparisons to previous periods), building widget configurations that map KPIs to specific chart types (line graphs, bar charts, gauges), and caching the assembled dashboard with a TTL so the frontend loads instantly. Each step depends on the one before it: KPIs require aggregated metrics, widgets require computed KPIs, and caching requires fully assembled widgets.

Without orchestration, you'd build a monolithic dashboard backend that queries all data sources inline, computes KPIs in the same method, constructs widget JSON, and writes to Redis in a single call chain. If the metrics aggregation query times out against a slow database, there's no automatic retry. If the cache write fails after expensive KPI computation, you'd recompute everything from scratch. Adding a new widget type or data source means modifying deeply coupled code with no visibility into which step is slow.

## The Solution

**You just write the metric aggregation, KPI computation, widget assembly, and cache workers. Conductor handles the metric-to-cache pipeline sequencing, retries when database queries time out, and per-step observability for diagnosing slow dashboard refreshes.**

Each stage of dashboard preparation is a simple, independent worker. The aggregation worker queries data sources and rolls up raw metrics for the requested time range and dashboard ID. The KPI worker computes derived values. Conversion rates, period-over-period growth, averages. The widget builder maps KPIs and metrics to chart configurations (type, labels, data series). The cache worker stores the assembled dashboard in a cache layer with a configurable TTL. Conductor executes them in sequence, passes metric data between steps, retries if a database query times out, and resumes from exactly where it left off if the process crashes.

### What You Write: Workers

Four workers power the dashboard pipeline: aggregating raw metrics from data sources, computing KPIs like conversion rates and growth, assembling chart widget configurations, and caching the result for fast frontend loads.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateMetricsWorker** | `dh_aggregate_metrics` | Aggregates raw metrics for the dashboard. |
| **BuildWidgetsWorker** | `dh_build_widgets` | Builds dashboard widget configurations from KPIs and metrics. |
| **CacheDashboardWorker** | `dh_cache_dashboard` | Caches the assembled dashboard for fast retrieval. |
| **ComputeKpisWorker** | `dh_compute_kpis` | Computes KPIs from aggregated metrics. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
dh_aggregate_metrics
 │
 ▼
dh_compute_kpis
 │
 ▼
dh_build_widgets
 │
 ▼
dh_cache_dashboard

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
