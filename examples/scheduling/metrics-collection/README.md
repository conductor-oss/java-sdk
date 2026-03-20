# Metrics Collection in Java Using Conductor :  Infrastructure, Application, and Business Metrics in Parallel

A Java Conductor workflow example for metrics collection .  gathering infrastructure metrics (CPU, memory, disk), application metrics (latency, error rates, throughput), and business metrics (revenue, conversions) in parallel via FORK/JOIN, then aggregating into a unified view.

## The Problem

You need to collect metrics from three distinct layers .  infrastructure (servers, containers), application (API performance, error rates), and business (conversion rates, revenue per user). These collections are independent and should run in parallel, but the results must be aggregated into a single view that correlates infrastructure capacity with application performance and business outcomes.

Without orchestration, each metrics layer has its own collection pipeline that runs independently. Infrastructure metrics are in Prometheus, app metrics in Datadog, and business metrics in a BI tool. Nobody can correlate a revenue drop with an API latency spike caused by infrastructure resource exhaustion because the data is siloed.

## The Solution

**You just write the per-layer metric collectors and aggregation logic. Conductor handles parallel collection so one slow layer does not block the others, retries on metric source timeouts, and per-layer timing and sample counts for every collection run.**

Conductor's FORK/JOIN collects infrastructure, application, and business metrics in parallel. An aggregation worker combines all three layers into a unified view. If one collector is slow, the others don't wait. Every collection run is tracked with per-layer timing and sample counts. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three layer-specific collectors run in parallel via FORK/JOIN: CollectAppWorker gathers latency and error rates, CollectBusinessWorker captures revenue and conversions, and a CollectInfraWorker measures CPU and memory, then AggregateWorker combines them into a unified cross-layer view.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `mc_aggregate` | Combines metrics from all three collection sources (app, infra, business) into a unified total with source count |
| **CollectAppWorker** | `mc_collect_app` | Collects application-level metrics (latency, error rates, throughput) from a given environment |
| **CollectBusinessWorker** | `mc_collect_business` | Collects business metrics (revenue, conversions, user engagement) from a given environment |
| **CollectInfraWorker** | `mc_collect_infra` | Collects infrastructure metrics (CPU, memory, disk, network) from a given environment |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

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
  --input '{"environment": "test-value", "timeRange": "2026-01-01T00:00:00Z"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w metrics_collection_411 -s COMPLETED -c 5
```

## How to Extend

Each worker collects one metrics layer .  connect the infrastructure collector to Prometheus, the application collector to Datadog, the business collector to your analytics database, and the parallel-collect-then-aggregate workflow stays the same.

- **AggregateWorker** (`mc_aggregate`): correlate metrics across layers, compute composite health scores, and push to a unified Grafana dashboard
- **CollectAppWorker** (`mc_collect_app`): pull application metrics from Datadog, New Relic, or your app's /metrics endpoint (Micrometer, StatsD)
- **CollectBusinessWorker** (`mc_collect_business`): query your analytics database, Stripe API, or data warehouse for business KPIs

Hook up Prometheus, Datadog, and your BI tool, and the parallel collection-then-aggregation pipeline works without any orchestration changes.

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
│       ├── AggregateWorker.java
│       ├── CollectAppWorker.java
│       ├── CollectBusinessWorker.java
│       └── CollectInfraWorker.java
└── src/test/java/metricscollection/workers/
    └── AggregateWorkerTest.java        # 2 tests
```
