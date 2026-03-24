# Dashboard Data

A business intelligence team needs to compile data from multiple microservices into a single dashboard payload. Each data source returns a different shape, latency varies, and if one source is slow the dashboard times out. They need to fetch, normalize, and merge data into a unified response with freshness metadata.

## Pipeline

```
[dh_aggregate_metrics]
     |
     v
[dh_compute_kpis]
     |
     v
[dh_build_widgets]
     |
     v
[dh_cache_dashboard]
```

**Workflow inputs:** `dashboardId`, `timeRange`, `refreshInterval`

## Workers

**AggregateMetricsWorker** (task: `dh_aggregate_metrics`)

Aggregates raw metrics for the dashboard.

- Writes `metrics`, `metricCount`

**BuildWidgetsWorker** (task: `dh_build_widgets`)

Builds dashboard widget configurations from KPIs and metrics.

- Uses java streams
- Writes `widgets`, `widgetCount`

**CacheDashboardWorker** (task: `dh_cache_dashboard`)

Caches the assembled dashboard for fast retrieval.

- Records wall-clock milliseconds
- Writes `cacheKey`, `ttl`, `ready`, `cachedAt`

**ComputeKpisWorker** (task: `dh_compute_kpis`)

Computes KPIs from aggregated metrics.

- Formats output strings, filters with predicates
- Writes `kpis`, `kpiCount`

---

**19 tests** | Workflow: `dashboard_data` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
