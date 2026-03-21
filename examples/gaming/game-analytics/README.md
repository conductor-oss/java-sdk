# Game Analytics in Java Using Conductor

Runs a game analytics pipeline: collecting raw event data, processing it into structured records, aggregating time-series metrics, computing KPIs (DAU, retention, ARPU), and generating a report. ## The Problem

You need to analyze game performance metrics over a date range. The workflow collects raw event data (sessions, purchases, achievements, crashes), processes it into structured records, aggregates it into time-series summaries, computes key performance indicators (DAU, retention, ARPU, session length), and generates an analytics report. Without analytics, you are flying blind. you do not know which features drive engagement, where players churn, or whether a new update improved retention.

Without orchestration, you'd build an analytics pipeline that queries event logs, runs ETL transforms, computes KPIs in SQL or code, and renders reports. manually handling schema changes in event data, retrying failed queries on large datasets, and managing the compute resources for expensive aggregations.

## The Solution

**You just write the event collection, data processing, metric aggregation, KPI calculation, and report generation logic. Conductor handles ingestion retries, metric aggregation sequencing, and analytics pipeline tracking.**

Each analytics concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect, process, aggregate, compute KPIs, report), retrying if the data warehouse is temporarily unavailable, tracking every analytics run, and resuming from the last step if the process crashes. ### What You Write: Workers

Event ingestion, metric aggregation, trend analysis, and dashboard update workers each process one layer of game telemetry data.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `gan_aggregate` | Aggregates processed events into time-series summaries (DAU, average session length, 7-day retention) |
| **CollectEventsWorker** | `gan_collect_events` | Collects raw events (logins, matches, purchases, achievements) for a game over a date range |
| **ComputeKpisWorker** | `gan_compute_kpis` | Computes key performance indicators: DAU, ARPDAU, 7-day retention, average session length, and conversion rate |
| **ProcessWorker** | `gan_process` | Processes raw events into structured records: sessions, matches, and purchases |
| **ReportWorker** | `gan_report` | Generates the final analytics report with all KPIs and publishes it |

### The Workflow

```
gan_collect_events
 │
 ▼
gan_process
 │
 ▼
gan_aggregate
 │
 ▼
gan_compute_kpis
 │
 ▼
gan_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
