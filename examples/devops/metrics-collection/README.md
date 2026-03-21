# Metrics Collection in Java with Conductor :  Parallel Source Collection via FORK_JOIN, Aggregate

Collect metrics from multiple sources in parallel using FORK/JOIN, then aggregate the results. Pattern: FORK(collect_app, collect_infra, collect_business) -> JOIN -> aggregate. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Metrics From Multiple Sources Need Unified Collection

A complete system health picture requires metrics from multiple sources: application metrics (request rate, error rate, latency from Prometheus), infrastructure metrics (CPU, memory, disk from CloudWatch), and business metrics (orders per minute, revenue from the application database). Collecting them sequentially triples the collection time. Collecting them in parallel gives you all metrics in the time of the slowest source.

After parallel collection, the metrics need aggregation .  normalizing timestamps, aligning time windows, computing derived metrics (error rate = errors / total requests), and producing a unified view that spans all three sources. If one source is temporarily unavailable, the other two should still be collected and aggregated with a gap note.

## The Solution

**You write the source-specific collectors and aggregation logic. Conductor handles parallel collection, result merging, and per-source failure isolation.**

`FORK_JOIN` dispatches parallel collectors to gather metrics simultaneously from application, infrastructure, and business sources .  each returning metrics with timestamps, values, and metadata. After `JOIN` collects all results, `AggregateWorker` normalizes timestamps across sources, aligns time windows, computes derived metrics, and produces a unified metrics summary. Conductor runs all collectors in parallel and records collection latency per source for monitoring pipeline health.

### What You Write: Workers

Four workers collect metrics in parallel. Gathering application, infrastructure, and business metrics simultaneously, then aggregating them into a unified view.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateMetrics** | `mc_aggregate` | Aggregates metric counts from all sources into a combined total. |
| **CollectAppMetrics** | `mc_collect_app` | Collects application-level metrics such as request rate, error rate, and latency. |
| **CollectBusinessMetrics** | `mc_collect_business` | Collects business-level metrics such as revenue, orders, and conversion rate. |
| **CollectInfraMetrics** | `mc_collect_infra` | Collects infrastructure-level metrics such as CPU, memory, and disk I/O. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

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
java -jar target/metrics-collection-1.0.0.jar

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
java -jar target/metrics-collection-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow metrics_collection_411 \
  --version 1 \
  --input '{"environment": "staging", "timeRange": "2026-01-01T00:00:00Z"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w metrics_collection_411 -s COMPLETED -c 5

```

## How to Extend

Each worker collects from one metric source .  replace the simulated calls with Prometheus PromQL, CloudWatch GetMetricData, or SQL queries for real application, infrastructure, and business metrics, and the collection workflow runs unchanged.

- **CollectAppMetrics** (`mc_collect_app`): query Prometheus via PromQL for application-level metrics like request rate, error rate, and latency percentiles
- **CollectInfraMetrics** (`mc_collect_infra`): pull infrastructure metrics from AWS CloudWatch GetMetricData, GCP Monitoring API, or node_exporter for CPU, memory, disk, and network utilization
- **CollectBusinessMetrics** (`mc_collect_business`): query SQL databases, Stripe API, or analytics platforms for business KPIs like revenue, active users, and conversion rates
- **AggregateMetrics** (`mc_aggregate`): implement time-series alignment (interpolation for mismatched intervals), derived metric computation, and anomaly detection on the unified dataset

Connect to Prometheus, CloudWatch, and your application database; the parallel collection pipeline maintains the same aggregation interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
metrics-collection/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/metricscollection/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MetricsCollectionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateMetrics.java
│       ├── CollectAppMetrics.java
│       ├── CollectBusinessMetrics.java
│       └── CollectInfraMetrics.java
└── src/test/java/metricscollection/workers/
    ├── AggregateMetricsTest.java        # 8 tests
    ├── CollectAppMetricsTest.java        # 7 tests
    ├── CollectBusinessMetricsTest.java        # 7 tests
    └── CollectInfraMetricsTest.java        # 7 tests

```
