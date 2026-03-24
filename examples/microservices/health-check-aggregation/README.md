# Health Check Aggregation in Java with Conductor

System-wide health check aggregation using FORK/JOIN.

## The Problem

Determining overall system health requires checking multiple infrastructure components. API gateway, database, cache, message queue, and aggregating their individual statuses into a single health verdict. These checks are independent and should run in parallel for speed, but the aggregation must wait for all of them to complete.

Without orchestration, health aggregation is a polling loop that sequentially pings each component, making the overall check slow (sum of all latencies). Partial failures (e.g., cache is down but everything else is healthy) are hard to represent without a structured aggregation step.

## The Solution

**You just write the per-component health-check workers and the aggregation worker. Conductor handles parallel health probes, per-component timeouts, and automatic aggregation once all checks complete.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Five workers probe infrastructure in parallel: CheckApiWorker, CheckDbWorker, CheckCacheWorker, and CheckQueueWorker each report component health, then AggregateHealthWorker produces an overall system verdict.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateHealthWorker** | `hc_aggregate_health` | Aggregates all component health checks into an overall system status (healthy/degraded). |
| **CheckApiWorker** | `hc_check_api` | Checks the API gateway health and reports latency. |
| **CheckCacheWorker** | `hc_check_cache` | Checks the Redis cache health and reports memory usage. |
| **CheckDbWorker** | `hc_check_db` | Checks the database health and reports connection pool utilization. |
| **CheckQueueWorker** | `hc_check_queue` | Checks the Kafka message queue health and reports consumer lag. |

the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
 ├── hc_check_api
 ├── hc_check_db
 ├── hc_check_cache
 └── hc_check_queue
 │
 ▼
JOIN (wait for all branches)
hc_aggregate_health

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
