# Analytics Reporting Pipeline in Java Using Conductor : Event Collection, Data Aggregation, KPI Computation, and Dashboard Generation

## Why Analytics Pipelines Need Orchestration

Building an analytics report requires a strict data pipeline. You collect 1.25 million raw events from multiple sources and write them to a staging area. You aggregate those events into session-level summaries. 85K total sessions, 42K unique users, 320K page views, 245-second average session duration. You compute business-critical KPIs from the aggregated data: daily/weekly/monthly active users, 38.5% bounce rate, 4.2% conversion rate. Finally, you generate an interactive dashboard report and schedule delivery to stakeholders.

Each stage depends on clean output from the previous one. aggregation needs complete event data, KPI computation needs accurate aggregates, and the report needs final metrics. If the event collection fails partway through, you do not want to compute KPIs on incomplete data. Without orchestration, you'd build a monolithic ETL script that mixes data ingestion, aggregation queries, metric calculations, and report rendering, making it impossible to rerun just the KPI computation when a formula changes, or to swap your event source without touching the report generator.

## How This Workflow Solves It

**You just write the analytics workers. Event collection, data aggregation, KPI computation, and dashboard generation. Conductor handles stage ordering, data source retries, and per-stage timing metrics for pipeline optimization.**

Each pipeline stage is an independent worker. collect events, aggregate data, compute metrics, generate report. Conductor sequences them, passes raw data paths and aggregated metrics between stages, retries if a data source query times out, and tracks exactly how long each ETL stage takes for pipeline optimization.

### What You Write: Workers

Four workers form the analytics pipeline: CollectEventsWorker ingests raw user events, AggregateDataWorker computes session-level metrics, ComputeMetricsWorker derives business KPIs, and GenerateReportWorker builds interactive dashboards.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateDataWorker** | `anr_aggregate_data` | Aggregates data |
| **CollectEventsWorker** | `anr_collect_events` | Collects events |
| **ComputeMetricsWorker** | `anr_compute_metrics` | Computes metrics |
| **GenerateReportWorker** | `anr_generate_report` | Generates the report |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
anr_collect_events
 │
 ▼
anr_aggregate_data
 │
 ▼
anr_compute_metrics
 │
 ▼
anr_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
