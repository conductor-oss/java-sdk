# Custom Metrics Pipeline in Java with Conductor :  Define, Collect, Aggregate, Dashboard Update

Automates custom metrics pipelines using [Conductor](https://github.com/conductor-oss/conductor). This workflow defines custom metric definitions, collects raw data points for those metrics, aggregates them over a time window (sum, average, percentiles), and updates dashboards with the results. You write the metrics logic, Conductor handles retries, failure routing, durability, and observability for free.

## Business Metrics That Infrastructure Tools Cannot See

Your standard monitoring covers CPU, memory, and request latency. But the business needs to track checkout conversion rate, cart abandonment by region, and API quota usage per tenant. These custom metrics require defining what to measure, collecting the raw events, aggregating them into meaningful numbers over time windows, and pushing the results to a dashboard the team actually watches.

Without orchestration, you'd wire all of this together in a single monolithic class .  managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the metric definitions and aggregation logic. Conductor handles the define-collect-aggregate-display pipeline and tracks every collection cycle.**

`DefineMetricsWorker` specifies the metrics to collect .  name, data source, collection interval, aggregation method (count, average, p99), and retention period. `CollectDataWorker` gathers raw data points from the configured sources ,  parsing application logs, querying databases, or consuming event streams. `AggregateWorker` computes aggregated values for each metric using the specified method ,  rolling averages, percentile calculations, rate computations. `UpdateDashboardWorker` pushes the aggregated metrics to monitoring dashboards in the appropriate format. Conductor records each collection and aggregation cycle for metrics pipeline health monitoring.

### What You Write: Workers

Four workers manage custom metrics. Defining what to measure, collecting raw data, aggregating over time windows, and updating dashboards.

| Worker | Task | What It Does |
|---|---|---|
| **Aggregate** | `cus_aggregate` | Aggregates raw data points over the specified window. |
| **CollectData** | `cus_collect_data` | Collects data points for registered custom metrics. |
| **DefineMetrics** | `cus_define_metrics` | Registers custom metric definitions. |
| **UpdateDashboard** | `cus_update_dashboard` | Updates the dashboard with aggregated metrics. |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input -> Aggregate -> CollectData -> DefineMetrics -> UpdateDashboard -> Output
```

## Example Output

```
=== Example 422: Custom Metrics ===

Step 1: Registering task definitions...
  Registered: cus_aggregate, cus_collect_data, cus_define_metrics, cus_update_dashboard

Step 2: Registering workflow 'custom_metrics_422'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [cus_aggregate] Aggregating
  [cus_collect_data] Collecting data for
  [cus_define_metrics] Registering custom metric definitions...
  [cus_update_dashboard] Updating dashboard with

  Status: COMPLETED
  Output: {metricCount=..., aggregatedMetrics=..., rawDataPoints=..., collectedAt=...}

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
  --workflow custom_metrics \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w custom_metrics -s COMPLETED -c 5
```

## How to Extend

Each worker handles one metrics pipeline step .  replace the simulated calls with Prometheus custom exporters, Grafana dashboard APIs, or CloudWatch PutMetricData, and the metrics workflow runs unchanged.

- **DefineMetrics** (`cm_define_metrics`): load metric specifications from a YAML config or API, defining which custom metrics to collect, their data types, labels, and expected ranges
- **CollectData** (`cm_collect_data`): query real data sources: CloudWatch custom metrics, Prometheus custom metric endpoints, or application log files parsed with regex/JSON extractors
- **Aggregate** (`cm_aggregate`): implement proper statistical aggregation: HdrHistogram for accurate percentile calculation, exponential moving averages for trend detection, and rate calculations with time-window normalization
- **UpdateDashboard** (`cm_update_dashboard`): push metrics to Grafana via the HTTP API, Datadog via DogStatsD, or CloudWatch via PutMetricData for real-time dashboard updates

Plug in your real data sources and Grafana dashboard API; the metrics pipeline uses the same collection-to-display contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

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
│       ├── Aggregate.java
│       ├── CollectData.java
│       ├── DefineMetrics.java
│       └── UpdateDashboard.java
└── src/test/java/custommetrics/workers/
    ├── AggregateTest.java        # 7 tests
    ├── CollectDataTest.java        # 7 tests
    ├── DefineMetricsTest.java        # 7 tests
    └── UpdateDashboardTest.java        # 7 tests
```
