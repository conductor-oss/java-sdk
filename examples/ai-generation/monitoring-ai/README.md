# Monitoring AI: Metrics Collection, Anomaly Detection, Diagnosis, Recommendations

A production service shows degraded performance but the monitoring dashboard has dozens of metrics -- CPU, memory, disk, latency, error rate, throughput. A human operator has to mentally correlate spikes across metrics, identify which anomaly is the root cause and which are symptoms, and decide what to do about it. This cognitive load grows with every new service added.

This workflow automates the monitoring intelligence loop: collect metrics, detect anomalies, diagnose root causes, and recommend remediation actions.

## Pipeline Architecture

```
serviceName, timeWindow
         |
         v
  mai_collect_metrics    (metrics map, metricCount=4)
         |
         v
  mai_detect_anomalies   (anomalies list, anomalyCount)
         |
         v
  mai_diagnose           (diagnosis with rootCause, confidence=0.87)
         |
         v
  mai_recommend          (recommendations list)
```

## Worker: CollectMetrics (`mai_collect_metrics`)

Gathers system metrics for the specified `serviceName` over the `timeWindow`. Returns a `metrics` map with four metric categories. CPU metrics include `avg: 72`, `max: 95`, and `p99: 91` -- indicating sustained high utilization with spikes near saturation. Also returns `metricCount: 4` reflecting the number of metric categories collected.

## Worker: DetectAnomalies (`mai_detect_anomalies`)

Analyzes the collected metrics against baseline thresholds. Returns an `anomalies` list where each entry is a map with `metric`, `severity`, `value`, and `threshold`. For example: `{metric: "latency", severity: "high", value: 850, threshold: 500}` -- latency 70% above the acceptable threshold. Reports `anomalyCount` equal to the number of detected anomalies.

## Worker: Diagnose (`mai_diagnose`)

Correlates the detected anomalies to identify the root cause. Returns a `diagnosis` map with `rootCause` (the underlying issue, not just the symptom) and `confidence: 0.87`. Also outputs `rootCause` as a top-level field for easy downstream access. The confidence score reflects how strongly the evidence supports the diagnosed cause.

## Worker: Recommend (`mai_recommend`)

Generates actionable remediation `recommendations` based on the diagnosis. Returns a list of recommended actions, each specifying what to do, why it addresses the root cause, and the expected impact. Recommendations are ordered by priority.

## Tests

4 tests cover metrics collection, anomaly detection, diagnosis, and recommendations.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
