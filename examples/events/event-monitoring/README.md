# Event Monitoring

An operations team needs real-time visibility into the health of their event pipeline. Each event needs metric extraction (latency, size, type), threshold checking against SLAs, alert generation when metrics breach limits, and aggregation into a monitoring dashboard payload.

## Pipeline

```
[em_collect_metrics]
     |
     v
[em_analyze_throughput]
     |
     v
[em_analyze_latency]
     |
     v
[em_analyze_errors]
     |
     v
[em_generate_report]
```

**Workflow inputs:** `pipelineName`, `timeRange`

## Workers

**AnalyzeErrorsWorker** (task: `em_analyze_errors`)

Analyzes error rate from raw metrics.

- Formats output strings
- Reads `metrics`. Writes `errorRate`

**AnalyzeLatencyWorker** (task: `em_analyze_latency`)

Analyzes latency from raw metrics.

- Reads `metrics`. Writes `latency`

**AnalyzeThroughputWorker** (task: `em_analyze_throughput`)

Analyzes throughput from raw metrics.

- Reads `metrics`. Writes `throughput`

**CollectMetricsWorker** (task: `em_collect_metrics`)

Collects raw metrics for a given pipeline and time range.

- Reads `pipelineName`, `timeRange`. Writes `rawMetrics`

**GenerateReportWorker** (task: `em_generate_report`)

Generates a monitoring report from throughput, latency, and error analysis.

- Parses strings to `double`
- Reads `pipeline`, `errorRate`. Writes `reportGenerated`, `alertLevel`

---

**40 tests** | Workflow: `event_monitoring_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
