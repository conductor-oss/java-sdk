# Custom Metrics Pipeline in Java Using Conductor :  Define, Collect, Aggregate, and Dashboard

A Java Conductor workflow example for custom metrics .  defining business-specific metrics, collecting raw data from application sources, aggregating values over configurable windows, and updating dashboards with the computed results.

## The Problem

You need business-specific metrics that off-the-shelf monitoring doesn't provide .  revenue per API call, feature adoption rates, customer health scores. These custom metrics require defining what to measure, collecting raw data from application databases and APIs, aggregating over time windows (hourly, daily), and pushing results to dashboards. Each step depends on the previous one, and the pipeline must run reliably on schedule.

Without orchestration, custom metrics are one-off scripts that query databases, compute aggregates in memory, and push to Grafana. When the database query times out, the dashboard shows stale data with no indication. Adding a new metric means cloning a script and hoping it runs.

## The Solution

**You just write the metric definitions and aggregation queries. Conductor handles the define-collect-aggregate-dashboard pipeline, retries when data source queries time out, and tracking of every collection timestamp and aggregated result.**

Each metrics concern is an independent worker .  metric definition, data collection, aggregation, and dashboard update. Conductor runs them in sequence: define what to measure, collect the raw data, aggregate it, then update the dashboard. Every pipeline run is tracked with collection timestamps and aggregation results. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers power the metrics pipeline: DefineMetricsWorker registers business-specific measurements, CollectDataWorker gathers raw data points, CusAggregateWorker computes percentiles and averages, and UpdateDashboardWorker pushes computed results to visualization tools.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `cus_collect_data` | Collects raw data points for all registered custom metrics from application sources |
| **CusAggregateWorker** | `cus_aggregate` | Aggregates raw data points into computed metric values (e.g., percentiles like p50) across the configured window |
| **DefineMetricsWorker** | `cus_define_metrics` | Registers custom metric definitions and returns the count of metrics available for collection |
| **UpdateDashboardWorker** | `cus_update_dashboard` | Pushes aggregated metric values to the dashboard for visualization |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cus_define_metrics
    │
    ▼
cus_collect_data
    │
    ▼
cus_aggregate
    │
    ▼
cus_update_dashboard
```

## Example Output

```
=== Example 422: Custom Metrics ===

Step 1: Registering task definitions...
  Registered: cus_define_metrics, cus_collect_data, cus_aggregate, cus_update_dashboard

Step 2: Registering workflow 'custom_metrics_422'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [collect] Collecting data for
  [aggregate] Aggregating
  [define] Registering custom metric definitions
  [dashboard] Updating dashboard

  Status: COMPLETED
  Output: {rawDataPoints=..., metricCount=..., aggregatedMetrics=..., registeredMetrics=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/custom-metrics-1.0.0.jar
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
java -jar target/custom-metrics-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow custom_metrics_422 \
  --version 1 \
  --input '{"metricDefinitions": "sample-metricDefinitions", "order_processing_time": "2025-01-15T10:00:00Z", "collectionInterval": "sample-collectionInterval", "10s": "sample-10s", "aggregationWindow": "sample-aggregationWindow"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w custom_metrics_422 -s COMPLETED -c 5
```

## How to Extend

Each worker owns one metrics step .  connect the data collector to your application databases, the dashboard updater to Grafana or Datadog, and the define-collect-aggregate-dashboard workflow stays the same.

- **CollectDataWorker** (`cus_collect_data`): query real data sources .  application databases, event streams (Kafka, Kinesis), or REST APIs
- **CusAggregateWorker** (`cus_aggregate`): compute real aggregations .  sum, avg, percentile, count ,  over the configured time window
- **DefineMetricsWorker** (`cus_define_metrics`): load metric definitions from a config file, database, or API .  metric name, data source, aggregation function, and window

Connect to your application databases and Grafana, and the metrics pipeline runs in production with the same workflow definition.

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
custom-metrics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/custommetrics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CustomMetricsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectDataWorker.java
│       ├── CusAggregateWorker.java
│       ├── DefineMetricsWorker.java
│       └── UpdateDashboardWorker.java
└── src/test/java/custommetrics/workers/
    └── DefineMetricsWorkerTest.java        # 2 tests
```
