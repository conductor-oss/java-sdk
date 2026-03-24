# Health Dashboard

A production environment needs a unified health view. The pipeline checks API, database, and cache health in parallel using a FORK, then renders a dashboard reporting overall status as GREEN (all healthy) or DEGRADED.

## Workflow

```
FORK ──┬── hd_check_api ────┐
       ├── hd_check_db ─────┤
       └── hd_check_cache ──┤
                              JOIN
                               │
                        hd_render_dashboard
```

Workflow `health_dashboard_417` accepts `environment`. Times out after `60` seconds.

## Workers

**CheckApiWorker** (`hd_check_api`) -- checks API health in the specified environment.

**CheckDbWorker** (`hd_check_db`) -- checks database health in the specified environment.

**CheckCacheWorker** (`hd_check_cache`) -- checks cache health in the specified environment.

**RenderDashboardWorker** (`hd_render_dashboard`) -- renders the dashboard. Reports overall status as GREEN when all services are healthy, DEGRADED otherwise.

## Workflow Output

The workflow produces `apiStatus`, `dbStatus`, `cacheStatus`, `overallHealth` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `health_dashboard_417` defines 3 tasks with input parameters `environment` and a timeout of `60` seconds.

## Tests

3 tests verify parallel health checks, result aggregation, and dashboard rendering with healthy and degraded scenarios.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
