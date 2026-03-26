# Event Aggregation

A metrics pipeline receives individual events (page views, clicks, API calls) at high volume but dashboards only need minute-level or hour-level aggregates. The system needs to buffer events, compute rollup statistics (count, sum, average, min, max) per time window, and emit a single aggregated event per window.

## Pipeline

```
[eg_collect_events]
     |
     v
[eg_aggregate_metrics]
     |
     v
[eg_generate_summary]
     |
     v
[eg_publish_batch]
```

**Workflow inputs:** `windowId`, `windowDurationSec`, `eventSource`

## Workers

**AggregateMetricsWorker** (task: `eg_aggregate_metrics`)

Aggregates metrics from collected events. Produces fixed totals: totalRevenue=478.47, avgOrderValue=100.70, purchaseCount=5, refundCount=1, and a product breakdown.

- Parses strings to `int`
- Reads `events`, `eventCount`. Writes `aggregation`

**CollectEventsWorker** (task: `eg_collect_events`)

Collects events from a time window. Returns a fixed set of 6 transaction events (purchases and refunds) along with the total event count.

- Parses strings to `int`
- Reads `windowId`, `windowDurationSec`, `eventSource`. Writes `events`, `eventCount`

**GenerateSummaryWorker** (task: `eg_generate_summary`)

Generates a human-readable summary report from the aggregated metrics, including highlights and a destination for the analytics pipeline.

- Reads `aggregation`, `windowId`. Writes `summary`, `destination`

**PublishBatchWorker** (task: `eg_publish_batch`)

Publishes the aggregated summary batch to the configured destination. Returns a fixed batch ID and publish timestamp.

- Reads `summary`, `destination`. Writes `batchId`, `published`, `publishedAt`

---

**36 tests** | Workflow: `event_aggregation_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
