# Building a Custom Metrics Pipeline for E-Commerce KPIs

Standard infrastructure metrics (CPU, memory) do not tell you why checkout conversions
dropped. You need business-specific metrics -- order processing time, cart abandonment rate,
checkout success count, payment retry rate -- collected, aggregated, and pushed to a
dashboard. This workflow registers metric definitions, collects raw data, aggregates over
a time window, and updates the dashboard.

## Workflow

```
metricDefinitions, collectionInterval, aggregationWindow
                     |
                     v
+---------------------+     +--------------------+     +----------------+     +-----------------------+
| cus_define_metrics  | --> | cus_collect_data   | --> | cus_aggregate  | --> | cus_update_dashboard  |
+---------------------+     +--------------------+     +----------------+     +-----------------------+
  4 metrics registered        4800 raw data points     4 aggregated           dashboardUpdated=true
  (histogram, gauge,          collectedAt timestamp    metrics computed       dashboardUrl set
   counter, gauge)
```

## Workers

**DefineMetrics** -- Registers 4 custom metrics: `order_processing_time` (histogram),
`cart_abandonment_rate` (gauge), `checkout_success_count` (counter), and
`payment_retry_rate` (gauge). Returns `registeredMetrics: 4`.

**CollectData** -- Takes `registeredMetrics` count and `collectionInterval`. Collects
`rawDataPoints: 4800` data points at `collectedAt: "2026-03-08T06:00:00Z"`.

**Aggregate** -- Aggregates 4800 raw data points over the specified `aggregationWindow`.
Returns 4 aggregated metrics: `order_processing_time` (p50=1200, p99=5000),
`cart_abandonment_rate` (12.5), `checkout_success_count` (total=8420),
`payment_retry_rate` (2.3).

**UpdateDashboard** -- Pushes the aggregated metrics to the dashboard. Returns
`dashboardUrl: "https://dashboard.example.com/custom-metrics"` and
`updatedAt: "2026-03-08T06:01:00Z"`.

## Tests

28 unit tests cover metric definition, data collection, aggregation, and dashboard updates.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
