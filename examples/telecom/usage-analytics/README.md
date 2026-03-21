# Usage Analytics in Java Using Conductor

## Why Usage Analytics Needs Orchestration

Analyzing telecom usage data requires a pipeline where each transformation depends on the previous one. You collect raw CDRs from network switches and media gateways for a region and time period. You process each record. normalizing formats, enriching with subscriber metadata, filtering duplicates, and converting to a standard usage event schema. You aggregate the processed records into metrics (total minutes, data volume, peak hours, geographic distribution) and detect anomalies (usage spikes, fraud patterns, revenue leakage). You generate the analytics report for business stakeholders. Finally, you raise alerts for any anomalies that need immediate attention.

If processing fails partway through, you need to know which CDRs were already processed to avoid counting them twice in the aggregation. If the report generates successfully but the alert worker fails, fraud patterns go unnotified even though they were detected. Without orchestration, you'd build a batch ETL job that mixes CDR collection, format normalization, aggregation SQL, report generation, and alerting into a single cron script. making it impossible to reprocess a subset of CDRs, test anomaly detection rules independently, or audit which raw records contributed to which report figures.

## The Solution

**You just write the CDR collection, record processing, metric aggregation, report generation, and anomaly alerting logic. Conductor handles ingestion retries, pattern analysis sequencing, and analytics audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Data ingestion, pattern analysis, anomaly detection, and report generation workers each process one layer of subscriber usage intelligence.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `uag_aggregate` | Aggregates processed records into usage metrics and detects anomalies (spikes, fraud patterns, revenue leakage). |
| **AlertWorker** | `uag_alert` | Raises alerts for detected anomalies that need immediate attention from operations or fraud teams. |
| **CollectCdrsWorker** | `uag_collect_cdrs` | Collects raw call detail records (CDRs) from network switches for a region and time period. |
| **ProcessWorker** | `uag_process` | Processes raw CDRs. normalizing formats, enriching with subscriber metadata, and filtering duplicates. |
| **ReportWorker** | `uag_report` | Generates the usage analytics report with aggregated metrics, trends, and anomaly summaries. |

### The Workflow

```
uag_collect_cdrs
 │
 ▼
uag_process
 │
 ▼
uag_aggregate
 │
 ▼
uag_report
 │
 ▼
uag_alert

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
