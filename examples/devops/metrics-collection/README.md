# Parallel Metrics Collection from App, Infra, and Business Sources

You need a single dashboard showing request rate alongside CPU usage and revenue numbers,
but those metrics live in three different systems. Collecting them sequentially triples the
pipeline time. This workflow uses FORK_JOIN to collect application, infrastructure, and
business metrics in parallel, then aggregates all 95 metrics from 3 sources.

## Workflow

```
environment, timeRange
         |
         v
    FORK_JOIN ---------------------------+
    |                |                   |
    v                v                   v
+------------------+ +------------------+ +----------------------+
| mc_collect_app   | | mc_collect_infra | | mc_collect_business  |
+------------------+ +------------------+ +----------------------+
  45 metrics          32 metrics           18 metrics
  requestRate=1200    cpuUsage=65%         revenue=$24500
  errorRate=0.5%      memoryUsage=72%      orders=340
  p99Latency=230ms    diskIO=340           conversionRate=3.2%
    |                |                   |
    +------ JOIN ---+-------------------+
                |
                v
      +----------------+
      | mc_aggregate   |   totalMetrics: 95, sources: 3
      +----------------+
```

## Workers

**CollectAppMetrics** -- Collects 45 application metrics from the specified `environment`.
Key values: `requestRate: 1200`, `errorRate: 0.5`, `p99Latency: 230`.

**CollectInfraMetrics** -- Collects 32 infrastructure metrics. Key values: `cpuUsage: 65`,
`memoryUsage: 72`, `diskIO: 340`.

**CollectBusinessMetrics** -- Collects 18 business metrics. Key values: `revenue: 24500`,
`orders: 340`, `conversionRate: 3.2`.

**AggregateMetrics** -- Sums counts from all 3 sources: `totalMetrics: 95`, `sources: 3`,
`aggregatedAt: "2026-03-08T06:00:00Z"`.

## Tests

29 unit tests cover each collector and the aggregation step.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
