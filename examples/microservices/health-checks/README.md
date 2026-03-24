# Health Checks in Java with Conductor

Check health of multiple services in parallel.

## The Problem

Monitoring the health of multiple services (API gateway, database, cache) requires hitting each service's health endpoint, collecting status and latency data, and producing a consolidated health report. These checks should run in parallel to minimize total check time, and the report must account for partial failures.

Without orchestration, health checks are run sequentially in a cron job, making the check cycle slow and providing no structured report. If one health endpoint times out, it blocks all subsequent checks, and there is no historical record of health status over time.

## The Solution

**You just write the service-check and report-generation workers. Conductor handles parallel execution of all checks, per-service timeout isolation, and historical tracking of every health run.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Two worker types power the health pipeline: CheckServiceWorker probes individual service endpoints in parallel, then GenerateReportWorker consolidates all results into a single health report.

| Worker | Task | What It Does |
|---|---|---|
| **CheckServiceWorker** | `hc_check_service` | Checks health of an individual service. |
| **GenerateReportWorker** | `hc_generate_report` | Generates a health report from individual service checks. |

the workflow coordination stays the same.

### The Workflow

```
FORK_JOIN
 ├── hc_check_service
 ├── hc_check_service
 └── hc_check_service
 │
 ▼
JOIN (wait for all branches)
hc_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
