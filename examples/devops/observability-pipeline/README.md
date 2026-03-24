# Full Observability Pipeline: Metrics, Traces, Anomaly Detection

Your checkout-service emits metrics and traces but nobody correlates them. A latency spike
in the metrics does not get connected to the slow database trace that caused it. This
workflow collects 15,000 metrics, correlates 3,200 distributed traces, detects 2 anomalies
(latency spike and error rate increase), and fires alerts while storing data in Grafana.

## Workflow

```
service, timeWindow
       |
       v
+----------------------+     +-----------------------+     +------------------------+     +---------------------+
| op_collect_metrics   | --> | op_correlate_traces   | --> | op_detect_anomalies    | --> | op_alert_or_store   |
+----------------------+     +-----------------------+     +------------------------+     +---------------------+
  COLLECT_METRICS-1365        3200 distributed              2 anomalies: latency          alerts sent for 2
  15000 metrics               traces correlated             spike + error rate             anomalies, stored
  from checkout-service                                     increase                       in Grafana
```

## Workers

**CollectMetricsWorker** -- Collects 15,000 metrics from the specified service. Returns
`collect_metricsId: "COLLECT_METRICS-1365"`.

**CorrelateTracesWorker** -- Correlates 3,200 distributed traces across microservices.
Returns `correlate_traces: true`.

**DetectAnomaliesWorker** -- Detects 2 anomalies: a latency spike and an error rate increase.
Returns `detect_anomalies: true`.

**AlertOrStoreWorker** -- Fires alerts for the 2 anomalies and stores all metrics in Grafana.
Returns `alert_or_store: true`.

## Tests

2 unit tests cover the observability pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
