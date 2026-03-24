# Parallel Service Health Checks with Overall Status Report

You have multiple services and need a single report showing which are healthy and which are
degraded. This workflow checks multiple services in parallel using FORK_JOIN, waits for all
results, and generates a report with `overallStatus` (ALL HEALTHY or DEGRADED) and per-
service details.

## Workflow

```
(no inputs)
    |
    v
  FORK_JOIN: hc_check_service (one per service)
    each checks serviceName at endpoint
    returns health map per service
    |
    v
  JOIN
    |
    v
+---------------------+
| hc_generate_report  |   overallStatus + per-service details + checkedAt
+---------------------+
```

## Workers

**CheckServiceWorker** -- Checks `serviceName` at `endpoint`. Returns a `health` map with
service status details.

**GenerateReportWorker** -- Aggregates results: `overallStatus` is "ALL HEALTHY" or
"DEGRADED" depending on whether all checks passed. Returns `services` details and a
`checkedAt` timestamp.

## Tests

9 unit tests cover individual service checks and report generation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
