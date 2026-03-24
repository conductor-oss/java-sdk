# Observability Pipeline in Java with Conductor

Orchestrates a full observability pipeline using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects metrics from a service, correlates distributed traces across microservices, detects anomalies (latency spikes, error rate increases), and either fires alerts or stores the data for dashboarding.

## Seeing Through the Noise

Your checkout-service generated 15,000 metrics and 3,200 traces in the last hour. Somewhere in that data, there is a latency spike and an error rate increase. Finding the signal in the noise requires collecting metrics, correlating traces to understand request flow, detecting anomalies, and deciding whether to page someone or just store it for later analysis. Doing this manually means staring at Grafana dashboards hoping to spot the pattern.

Without orchestration, you'd run a cron job that scrapes Prometheus, pipe the output into a Python script that computes z-scores, grep trace logs for correlated spans, and email yourself when something looks off. If the metrics collector fails, the anomaly detector runs on stale data and either misses the spike or fires a false alarm. There's no structured pipeline from raw telemetry to actionable alert, and no record of which anomalies were detected, when, or what the system looked like at the time.

## The Solution

**You write the metrics collection and anomaly detection logic. Conductor handles the telemetry-to-alert pipeline, correlation sequencing, and execution history.**

Each stage of the observability pipeline is a simple, independent worker. The metrics collector gathers CPU, memory, request rate, and error rate from the target service over the specified time window. The trace correlator links distributed traces across microservices to build end-to-end request flow maps, connecting a slow checkout response to a database query three services deep. The anomaly detector identifies latency spikes and error rate increases by comparing current metrics against baseline thresholds. The alert-or-store worker routes anomalies to PagerDuty or Slack, and stores clean metrics in the time-series database for dashboarding. Conductor executes them in strict sequence, ensures anomaly detection only runs after traces are correlated with metrics, retries if the metrics endpoint is temporarily unreachable, and tracks every pipeline execution with full input/output history.

### What You Write: Workers

Four workers build the observability pipeline. Collecting metrics, correlating traces, detecting anomalies, and routing alerts or storing data.

| Worker | Task | What It Does |
|---|---|---|
| **AlertOrStoreWorker** | `op_alert_or_store` | Routes anomalies to alerting channels or stores clean metrics in the time-series database |
| **CollectMetricsWorker** | `op_collect_metrics` | Gathers metrics (CPU, memory, request rate, error rate) from the target service |
| **CorrelateTracesWorker** | `op_correlate_traces` | Links distributed traces across microservices to build end-to-end request flow |
| **DetectAnomaliesWorker** | `op_detect_anomalies` | Identifies anomalies in the correlated data (latency spikes, error rate increases) |

the workflow and rollback logic stay the same.

### The Workflow

```
op_collect_metrics
 │
 ▼
op_correlate_traces
 │
 ▼
op_detect_anomalies
 │
 ▼
op_alert_or_store

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
