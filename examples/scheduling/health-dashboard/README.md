# Health Dashboard in Java Using Conductor : Parallel Service Health Checks and Dashboard Rendering

## The Problem

You need a health dashboard that shows the status of every component. API servers, databases, caches. at a glance. Each health check is independent and can run in parallel for speed (don't wait for the database check to finish before checking the cache). The results must be aggregated into a single dashboard view showing green/yellow/red status for each component.

Without orchestration, health dashboards either poll each component from the browser (slow, client-heavy) or run a monolithic script that checks everything serially (slow, single point of failure). If one health check hangs, the entire dashboard is stale.

## The Solution

**You just write the health check endpoints and dashboard rendering. Conductor handles parallel health checks so one slow component does not block the others, retries on transient check failures, and per-component timing for every dashboard refresh.**

Conductor's FORK/JOIN runs API, database, and cache health checks in parallel. A render worker aggregates all results into a unified dashboard. If one check is slow, the others still complete quickly. Every dashboard refresh is tracked with per-component timing and status. ### What You Write: Workers

Three health checkers run in parallel via FORK/JOIN. CheckApiWorker probes API servers, CheckDbWorker tests database connectivity, CheckCacheWorker verifies Redis/Memcached, then a RenderDashboardWorker aggregates results into a green/yellow/red status view.

| Worker | Task | What It Does |
|---|---|---|
| **CheckApiWorker** | `hd_check_api` | Checks API server health for a given environment, returning status and response time in milliseconds |
| **CheckCacheWorker** | `hd_check_cache` | Checks cache (Redis/Memcached) health for a given environment, returning status and response time |
| **CheckDbWorker** | `hd_check_db` | Checks database health for a given environment, returning status and response time |
| **RenderDashboardWorker** | `hd_render_dashboard` | Aggregates all component statuses into an overall health rating (GREEN/DEGRADED) and generates a dashboard URL |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
FORK_JOIN
 ├── hd_check_api
 ├── hd_check_db
 └── hd_check_cache
 │
 ▼
JOIN (wait for all branches)
hd_render_dashboard

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
