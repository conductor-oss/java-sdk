# Dashboard Data in Java Using Conductor :  Metric Aggregation, KPI Computation, Widget Assembly, and Caching

A Java Conductor workflow example for dashboard data preparation: aggregating raw metrics over a time range, computing KPIs like conversion rates and growth percentages, assembling widget configurations for charts and gauges, and caching the assembled dashboard for fast retrieval. Uses [Conductor](https://github.

## The Problem

You need to power a real-time dashboard that shows business metrics. Revenue trends, user activity, conversion rates, error counts. That means querying multiple data sources to aggregate raw metrics over a configurable time range, computing derived KPIs (growth rates, percentages, comparisons to previous periods), building widget configurations that map KPIs to specific chart types (line graphs, bar charts, gauges), and caching the assembled dashboard with a TTL so the frontend loads instantly. Each step depends on the one before it: KPIs require aggregated metrics, widgets require computed KPIs, and caching requires fully assembled widgets.

Without orchestration, you'd build a monolithic dashboard backend that queries all data sources inline, computes KPIs in the same method, constructs widget JSON, and writes to Redis in a single call chain. If the metrics aggregation query times out against a slow database, there's no automatic retry. If the cache write fails after expensive KPI computation, you'd recompute everything from scratch. Adding a new widget type or data source means modifying deeply coupled code with no visibility into which step is slow.

## The Solution

**You just write the metric aggregation, KPI computation, widget assembly, and cache workers. Conductor handles the metric-to-cache pipeline sequencing, retries when database queries time out, and per-step observability for diagnosing slow dashboard refreshes.**

Each stage of dashboard preparation is a simple, independent worker. The aggregation worker queries data sources and rolls up raw metrics for the requested time range and dashboard ID. The KPI worker computes derived values. Conversion rates, period-over-period growth, averages. The widget builder maps KPIs and metrics to chart configurations (type, labels, data series). The cache worker stores the assembled dashboard in a cache layer with a configurable TTL. Conductor executes them in sequence, passes metric data between steps, retries if a database query times out, and resumes from exactly where it left off if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers power the dashboard pipeline: aggregating raw metrics from data sources, computing KPIs like conversion rates and growth, assembling chart widget configurations, and caching the result for fast frontend loads.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateMetricsWorker** | `dh_aggregate_metrics` | Aggregates raw metrics for the dashboard. |
| **BuildWidgetsWorker** | `dh_build_widgets` | Builds dashboard widget configurations from KPIs and metrics. |
| **CacheDashboardWorker** | `dh_cache_dashboard` | Caches the assembled dashboard for fast retrieval. |
| **ComputeKpisWorker** | `dh_compute_kpis` | Computes KPIs from aggregated metrics. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
dh_aggregate_metrics
    │
    ▼
dh_compute_kpis
    │
    ▼
dh_build_widgets
    │
    ▼
dh_cache_dashboard

```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/dashboard-data-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/dashboard-data-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow dashboard_data \
  --version 1 \
  --input '{"dashboardId": "TEST-001", "timeRange": "2026-01-01T00:00:00Z", "refreshInterval": "sample-refreshInterval"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dashboard_data -s COMPLETED -c 5

```

## How to Extend

Point the aggregation worker at your real data sources (PostgreSQL, Prometheus, Datadog), write KPI calculations for your business metrics, and cache to Redis or Memcached, the dashboard workflow runs unchanged.

- **AggregateMetricsWorker** → query real data sources (PostgreSQL, ClickHouse, Prometheus, Datadog API) to aggregate metrics over the requested time range
- **ComputeKpisWorker** → add business-specific KPI calculations (MRR growth, churn rate, NPS scores, SLA compliance percentages)
- **BuildWidgetsWorker** → generate widget configs compatible with your frontend charting library (Chart.js, D3, Grafana panels, Retool components)
- **CacheDashboardWorker** → write to Redis, Memcached, or a CDN edge cache with the configured refresh interval as TTL

Replacing data sources or KPI formulas requires no workflow changes, each worker just needs to produce the expected metrics and widget configuration structure.

**Add new stages** by inserting tasks in `workflow.json`, for example, an alerting step that fires when a KPI crosses a threshold, a comparison worker that computes deltas against the previous period, or a permissions check that filters widgets based on the viewer's role.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
dashboard-data/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dashboarddata/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DashboardDataExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateMetricsWorker.java
│       ├── BuildWidgetsWorker.java
│       ├── CacheDashboardWorker.java
│       └── ComputeKpisWorker.java
└── src/test/java/dashboarddata/workers/
    ├── AggregateMetricsWorkerTest.java        # 5 tests
    ├── BuildWidgetsWorkerTest.java        # 4 tests
    ├── CacheDashboardWorkerTest.java        # 6 tests
    └── ComputeKpisWorkerTest.java        # 4 tests

```
