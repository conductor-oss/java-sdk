# Metrics Collection in Java with Conductor : Parallel Source Collection via FORK_JOIN, Aggregate

Collect metrics from multiple sources in parallel using FORK/JOIN, then aggregate the results. Pattern: FORK(collect_app, collect_infra, collect_business) -> JOIN -> aggregate.

## Metrics From Multiple Sources Need Unified Collection

A complete system health picture requires metrics from multiple sources: application metrics (request rate, error rate, latency from Prometheus), infrastructure metrics (CPU, memory, disk from CloudWatch), and business metrics (orders per minute, revenue from the application database). Collecting them sequentially triples the collection time. Collecting them in parallel gives you all metrics in the time of the slowest source.

After parallel collection, the metrics need aggregation. normalizing timestamps, aligning time windows, computing derived metrics (error rate = errors / total requests), and producing a unified view that spans all three sources. If one source is temporarily unavailable, the other two should still be collected and aggregated with a gap note.

## The Solution

**You write the source-specific collectors and aggregation logic. Conductor handles parallel collection, result merging, and per-source failure isolation.**

`FORK_JOIN` dispatches parallel collectors to gather metrics simultaneously from application, infrastructure, and business sources. each returning metrics with timestamps, values, and metadata. After `JOIN` collects all results, `AggregateWorker` normalizes timestamps across sources, aligns time windows, computes derived metrics, and produces a unified metrics summary. Conductor runs all collectors in parallel and records collection latency per source for monitoring pipeline health.

### What You Write: Workers

Four workers collect metrics in parallel. Gathering application, infrastructure, and business metrics simultaneously, then aggregating them into a unified view.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateMetrics** | `mc_aggregate` | Aggregates metric counts from all sources into a combined total. |
| **CollectAppMetrics** | `mc_collect_app` | Collects application-level metrics such as request rate, error rate, and latency. |
| **CollectBusinessMetrics** | `mc_collect_business` | Collects business-level metrics such as revenue, orders, and conversion rate. |
| **CollectInfraMetrics** | `mc_collect_infra` | Collects infrastructure-level metrics such as CPU, memory, and disk I/O. |

the workflow and rollback logic stay the same.

### The Workflow

```
FORK_JOIN
 ├── mc_collect_app
 ├── mc_collect_infra
 └── mc_collect_business
 │
 ▼
JOIN (wait for all branches)
mc_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
