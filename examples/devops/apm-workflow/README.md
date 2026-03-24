# Tracing a Slow Checkout Service with APM

Your checkout service is hitting intermittent latency spikes and customers are abandoning
carts. You need to collect distributed traces, compute latency percentiles, pinpoint
bottleneck endpoints, and generate a report with concrete recommendations -- all without
manually digging through dashboards.

## Workflow

```
serviceName, timeRange, percentile
              |
              v
   +------------------+     +-------------------+     +---------------------+     +-------------+
   | apm_collect_     | --> | apm_analyze_      | --> | apm_detect_         | --> | apm_report  |
   | traces           |     | latency           |     | bottlenecks         |     |             |
   +------------------+     +-------------------+     +---------------------+     +-------------+
     25000 traces            p50=45ms p95=180ms        /api/search: N+1 query     reportUrl
     2500 sampled            p99=520ms mean=62ms       /api/export: payload ser.  generated=true
```

## Workers

**CollectTraces** -- Takes `serviceName` and `timeRange` as inputs. Returns `traceCount` of
25,000 with `sampledTraces` at 2,500 (10% sampling) and echoes the `timeRange` back.

**AnalyzeLatency** -- Receives `traceCount` and `percentile` (e.g. `"p99"`). Computes
hard-coded latency buckets: `p50Latency` = 45ms, `p95Latency` = 180ms, `p99Latency` = 520ms,
`meanLatency` = 62ms. Identifies `slowEndpoints` as `["/api/search", "/api/export"]`.

**DetectBottlenecks** -- Reads `p99Latency` and `slowEndpoints`. Returns two bottlenecks:
`/api/search` caused by an `"N+1 query"` (impact `"high"`) and `/api/export` caused by
`"large payload serialization"` (impact `"medium"`). Recommendations include adding a database
index on search fields and implementing streaming for the export endpoint.

**ApmReport** -- Takes `serviceName` and `bottlenecks`. Outputs `reportGenerated: true` with a
`reportUrl` like `https://apm.example.com/report/checkout-2026-03-08`.

## Tests

30 unit tests cover trace collection, latency analysis, bottleneck detection, and report
generation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
