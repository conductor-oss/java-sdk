# Capacity Monitoring in Java Using Conductor : Resource Measurement, Forecasting, and Capacity Alerts

## The Problem

You need to monitor infrastructure capacity. CPU, memory, disk, network. across your clusters. Beyond current utilization, you need to forecast when resources will run out based on growth trends. If current usage exceeds thresholds or the forecast predicts exhaustion within your planning window, capacity alerts must fire so you can provision before outages occur.

Without orchestration, capacity monitoring is a dashboard that shows current state but doesn't predict. Forecasting runs separately from measurement, uses stale data, and alerting is disconnected from both. By the time someone notices a capacity issue, it's already causing production problems.

## The Solution

**You just write the resource measurement and capacity forecasting logic. Conductor handles the measure-forecast-alert pipeline, retries when cluster metric endpoints are slow, and historical tracking of capacity trends over time.**

Each capacity concern is an independent worker. resource measurement, growth forecasting, and alerting. Conductor runs them in sequence: measure current state, forecast future needs, then alert if thresholds are breached. Every monitoring run is tracked with measurements, forecasts, and alert decisions. ### What You Write: Workers

Three workers monitor infrastructure capacity: MeasureResourcesWorker samples CPU, memory, and disk utilization, ForecastWorker predicts days until exhaustion based on growth trends, and CapAlertWorker fires when capacity thresholds are breached or exhaustion is imminent.

| Worker | Task | What It Does |
|---|---|---|
| **CapAlertWorker** | `cap_alert` | Sends a capacity alert if forecasted disk exhaustion is within 30 days, with severity based on urgency |
| **ForecastWorker** | `cap_forecast` | Forecasts days until CPU, memory, and disk exhaustion based on current usage trends, with scaling recommendations |
| **MeasureResourcesWorker** | `cap_measure_resources` | Measures current CPU, memory, and disk utilization percentages and node count for a cluster |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
cap_measure_resources
 │
 ▼
cap_forecast
 │
 ▼
cap_alert

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
