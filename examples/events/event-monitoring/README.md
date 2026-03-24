# Event Monitoring in Java Using Conductor

Sequential event monitoring workflow that collects metrics, analyzes throughput, latency, and errors, then generates a report.

## The Problem

You need to monitor the health of your event processing pipeline. This means collecting throughput, latency, and error rate metrics over a specified time range, analyzing those metrics for anomalies (throughput drops, latency spikes, error rate increases), and generating a monitoring report. Without monitoring, you only learn about pipeline problems when downstream systems start failing.

Without orchestration, you'd build a monitoring script that queries multiple metrics sources, runs analysis logic inline, generates reports, and sends alerts. manually handling metrics API timeouts, correlating data across different monitoring systems, and scheduling the monitoring job itself.

## The Solution

**You just write the metrics-collection, throughput/latency/error analysis, and report-generation workers. Conductor handles sequential metric analysis, retry on metrics API timeouts, and a historical record of every monitoring run.**

Each monitoring concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of collecting metrics, analyzing them for anomalies, and generating the report, retrying if a metrics API times out, tracking every monitoring run, and providing a complete history of pipeline health assessments.

### What You Write: Workers

Five workers power the monitoring pipeline: CollectMetricsWorker gathers raw data, AnalyzeThroughputWorker, AnalyzeLatencyWorker, and AnalyzeErrorsWorker each assess a different dimension, and GenerateReportWorker produces the final health assessment.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeErrorsWorker** | `em_analyze_errors` | Analyzes error rate from raw metrics. |
| **AnalyzeLatencyWorker** | `em_analyze_latency` | Analyzes latency from raw metrics. |
| **AnalyzeThroughputWorker** | `em_analyze_throughput` | Analyzes throughput from raw metrics. |
| **CollectMetricsWorker** | `em_collect_metrics` | Collects raw metrics for a given pipeline and time range. |
| **GenerateReportWorker** | `em_generate_report` | Generates a monitoring report from throughput, latency, and error analysis. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
em_collect_metrics
 │
 ▼
em_analyze_throughput
 │
 ▼
em_analyze_latency
 │
 ▼
em_analyze_errors
 │
 ▼
em_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
