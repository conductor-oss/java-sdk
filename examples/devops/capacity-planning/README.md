# Capacity Planning in Java with Conductor

Automates infrastructure capacity planning using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects resource utilization metrics over a configurable period, analyzes growth trends, forecasts when capacity will be exhausted, and generates cost-aware scaling recommendations.

## Running Out of Runway

Your service is growing 15% month-over-month. At some point, current infrastructure will not be enough; but when? And how much should you add? Without automated capacity planning, teams either over-provision (wasting money) or under-provision (causing outages). This workflow turns raw metrics into a concrete recommendation: "add 3 nodes in 21 days, estimated cost $450/month."

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the metrics analysis and forecasting logic. Conductor handles the collection-to-recommendation pipeline and tracks every planning cycle.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers handle the capacity planning cycle. Collecting utilization metrics, analyzing growth trends, forecasting exhaustion, and recommending scaling actions.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeTrendsWorker** | `cp_analyze_trends` | Computes growth trends from collected data points (e.g., 15% month-over-month increase) |
| **CollectMetricsWorker** | `cp_collect_metrics` | Gathers resource utilization metrics (CPU, memory, disk) over the specified time period |
| **ForecastWorker** | `cp_forecast` | Projects when current capacity will be exhausted based on the growth trend, with a confidence score |
| **RecommendWorker** | `cp_recommend` | Generates a scaling recommendation with node count and estimated monthly cost |

the workflow and rollback logic stay the same.

### The Workflow

```
cp_collect_metrics
 │
 ▼
cp_analyze_trends
 │
 ▼
cp_forecast
 │
 ▼
cp_recommend

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
