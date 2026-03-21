# APM Workflow in Java with Conductor : Analyze Latency, Report Metrics, Collect Traces, Detect Bottlenecks

Automates Application Performance Monitoring (APM) analysis using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects distributed traces for a service, analyzes latency percentiles (p50/p95/p99), detects performance bottlenecks like N+1 queries and large payload serialization, and generates an APM report with actionable recommendations.

## Finding the Slow Endpoints

Your checkout service handled 25,000 requests in the last hour. Most responded in 45ms, but the p99 is 520ms. Something is dragging the tail. Two endpoints are suspiciously slow: `/api/search` has an N+1 query problem, and `/api/export` is choking on large payload serialization. You need to collect the traces, crunch the latency numbers, pinpoint the bottlenecks, and produce a report the team can act on.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the trace analysis and bottleneck detection logic. Conductor handles the collect-analyze-detect-report pipeline and tracks every performance investigation.**

`AnalyzeLatencyWorker` examines request latency distributions across endpoints. identifying which services and endpoints have degraded performance with p50/p95/p99 breakdowns. `ApmReportWorker` generates a performance report summarizing latency trends, error rates, throughput, and resource utilization. `CollectTracesWorker` gathers distributed traces for the slowest requests, showing the full request path across services. `DetectBottlenecksWorker` analyzes the traces to identify specific bottlenecks, slow database queries, overloaded services, or network latency. Conductor chains these four steps for automated performance investigation.

### What You Write: Workers

Four workers handle APM analysis. Collecting distributed traces, analyzing latency percentiles, detecting bottlenecks, and generating performance reports.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeLatency** | `apm_analyze_latency` | Analyzes latency percentiles from collected traces. |
| **ApmReport** | `apm_report` | Generates the final APM report summarizing bottlenecks and recommendations. |
| **CollectTraces** | `apm_collect_traces` | Collects application traces for the specified service and time range. |
| **DetectBottlenecks** | `apm_detect_bottlenecks` | Detects performance bottlenecks based on latency analysis. |

the workflow and rollback logic stay the same.

### The Workflow

```
Input -> AnalyzeLatency -> ApmReport -> CollectTraces -> DetectBottlenecks -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
