# Monitoring AI in Java with Conductor : Collect Metrics, Detect Anomalies, Diagnose, and Recommend

## Going Beyond Dashboards to Actionable Intelligence

Traditional monitoring shows you metrics on a dashboard. You still have to notice the anomaly, figure out what caused it, and decide what to do. AI-powered monitoring automates that reasoning chain: collect the metrics, detect what is abnormal, diagnose why, and recommend what to do about it. Each step builds on the previous. you cannot diagnose without anomalies, and you cannot recommend without a diagnosis.

This workflow runs one monitoring analysis cycle. The metrics collector pulls system data (CPU, memory, latency, error rates) for the service over the specified time window. The anomaly detector scans the metrics for deviations from normal behavior. The diagnoser analyzes detected anomalies to identify the root cause (memory leak, traffic spike, degraded dependency). The recommender produces specific actions (scale up, restart, roll back, investigate further) based on the diagnosis.

## The Solution

**You just write the metrics-collection, anomaly-detection, diagnosis, and recommendation workers. Conductor handles the monitoring intelligence pipeline.**

Four workers form the monitoring pipeline. metrics collection, anomaly detection, diagnosis, and recommendation. The collector gathers system metrics. The detector identifies anomalous patterns. The diagnoser determines root causes. The recommender suggests specific actions. Conductor sequences the four steps and passes metrics, anomalies, and diagnoses between them via JSONPath.

### What You Write: Workers

CollectMetricsWorker pulls CPU, memory, and latency data, DetectAnomaliesWorker flags deviations, DiagnoseWorker identifies root causes, and RecommendWorker suggests actions like scaling or rolling back.

| Worker | Task | What It Does |
|---|---|---|
| **CollectMetricsWorker** | `mai_collect_metrics` | Pulls system metrics (CPU, memory, latency, error rates) for the service over the time window. |
| **DetectAnomaliesWorker** | `mai_detect_anomalies` | Scans collected metrics for deviations from normal behavior and flags anomalies. |
| **DiagnoseWorker** | `mai_diagnose` | Identifies the root cause of detected anomalies (memory leak, traffic spike, degraded dependency). |
| **RecommendWorker** | `mai_recommend` | Produces specific actionable recommendations (scale up, restart, roll back) based on the diagnosis. |

### The Workflow

```
mai_collect_metrics
 │
 ▼
mai_detect_anomalies
 │
 ▼
mai_diagnose
 │
 ▼
mai_recommend

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
