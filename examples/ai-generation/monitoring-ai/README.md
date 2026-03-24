# Monitoring: Collect Metrics, Detect Anomalies, Diagnose, Recommend

Collect system metrics (CPU: avg 72, max 95, p99 91), detect anomalies (`{metric: "latency", severity: "high", value: 850, threshold: 500}`), diagnose the root cause (confidence: 0.87), and recommend actions.

## Workflow

```
serviceName, timeWindow
  -> mai_collect_metrics -> mai_detect_anomalies -> mai_diagnose -> mai_recommend
```

## Tests

8 tests cover metric collection, anomaly detection, diagnosis, and recommendations.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
