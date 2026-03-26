# Forecasting When Your Infrastructure Hits Capacity

Your service is growing 15% month-over-month but you only find out you need more nodes when
latency spikes. This workflow collects 30 days of metrics, analyzes the growth trend,
forecasts the day you run out of headroom, and recommends how many nodes to add and what
it will cost.

## Workflow

```
service, period
      |
      v
+--------------------+     +--------------------+     +--------------+     +----------------+
| cp_collect_metrics | --> | cp_analyze_trends  | --> | cp_forecast  | --> | cp_recommend   |
+--------------------+     +--------------------+     +--------------+     +----------------+
  43200 dataPoints          trend: "growing"          21 days until         action: "scale-up"
                            rate: 15%                 capacity              nodes: 3, $450/mo
                                                      confidence: 0.89
```

## Workers

**CollectMetricsWorker** -- Takes `service` and `period` inputs. Returns `dataPoints: 43200`
representing 30 days of minute-level metrics.

**AnalyzeTrendsWorker** -- Receives the data point count. Determines the trend is `"growing"`
at a `rate` of 15% month-over-month.

**ForecastWorker** -- Uses the trend to project that capacity will be exceeded in
`daysUntilCapacity: 21` with a `confidence` of 0.89.

**RecommendWorker** -- Based on the forecast, recommends `action: "scale-up"` by adding
`nodes: 3` at a `cost` of $450/month.

## Tests

2 unit tests cover the end-to-end pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
