# Anomaly Detection in Java Using Conductor: Data Collection, Baseline Computation, Detection, Classification, and Alerting

Request latency on your checkout service crept from 120ms to 450ms over six hours. Nobody noticed because it never crossed the static 500ms alert threshold. By the time it finally spiked to 800ms and paged someone, six hours of degraded checkout experience had already cost you an estimated $40K in abandoned carts. A fixed threshold can't catch gradual drift, and your on-call shouldn't have to stare at Grafana dashboards to notice that "120ms average" has quietly become "450ms average." You need detection that computes a baseline from recent history, spots deviations statistically, and pages you while the problem is still small.

## The Problem

You need to detect when a metric deviates from normal behavior. CPU spikes, latency increases, error rate jumps. Simple threshold checks miss gradual degradation and fire on expected seasonal patterns. You need to compute a baseline from historical data, detect deviations using statistical methods, classify the severity (warning vs critical), and alert only on genuine anomalies.

Without orchestration, anomaly detection is either a monolithic ML pipeline that's hard to debug or a collection of scripts that don't share state. When the baseline computation fails, detection runs against stale data. Nobody knows whether a missed alert was due to bad baseline, weak detection, or a notification failure.

## The Solution

**You just write the baseline computation and deviation detection logic. Conductor handles the collect-baseline-detect-classify-alert sequence, retries when metric sources are temporarily unavailable, and tracking of every baseline computed and anomaly detected.**

Each anomaly detection step is an independent worker: data collection, baseline computation, deviation detection, classification, and alerting. Conductor runs them in sequence, passing the baseline to the detector and the anomaly details to the classifier. Every detection run is tracked, you can see the baseline used, deviations found, and whether alerts fired.

### What You Write: Workers

Five workers form the detection pipeline: CollectDataWorker gathers historical metrics, ComputeBaselineWorker calculates statistical baselines, DetectWorker computes z-scores against the baseline, ClassifyWorker categorizes anomaly severity, and AlertWorker notifies the appropriate channel.

| Worker | Task | What It Does |
|---|---|---|
| **AlertWorker** | `anom_alert` | Sends an alert with severity and classification details to the configured channel (e.g., Slack) |
| **ClassifyWorker** | `anom_classify` | Classifies anomalies by z-score magnitude into normal/moderate/significant/spike categories with corresponding severity levels |
| **CollectDataWorker** | `anom_collect_data` | Collects historical metric data over a configurable lookback window and returns the latest value and data point count |
| **ComputeBaselineWorker** | `anom_compute_baseline` | Computes a statistical baseline (mean and standard deviation) from collected data points for anomaly comparison |
| **DetectWorker** | `anom_detect` | Calculates the z-score of the latest metric value against the baseline to determine if it is anomalous |

### The Workflow

```
anom_collect_data
 │
 ▼
anom_compute_baseline
 │
 ▼
anom_detect
 │
 ▼
anom_classify
 │
 ▼
anom_alert

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
