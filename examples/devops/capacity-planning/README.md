# Capacity Planning in Java with Conductor

Automates infrastructure capacity planning using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects resource utilization metrics over a configurable period, analyzes growth trends, forecasts when capacity will be exhausted, and generates cost-aware scaling recommendations.

## Running Out of Runway

Your service is growing 15% month-over-month. At some point, current infrastructure will not be enough; but when? And how much should you add? Without automated capacity planning, teams either over-provision (wasting money) or under-provision (causing outages). This workflow turns raw metrics into a concrete recommendation: "add 3 nodes in 21 days, estimated cost $450/month."

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the metrics analysis and forecasting logic. Conductor handles the collection-to-recommendation pipeline and tracks every planning cycle.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers handle the capacity planning cycle. Collecting utilization metrics, analyzing growth trends, forecasting exhaustion, and recommending scaling actions.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeTrendsWorker** | `cp_analyze_trends` | Computes growth trends from collected data points (e.g., 15% month-over-month increase) |
| **CollectMetricsWorker** | `cp_collect_metrics` | Gathers resource utilization metrics (CPU, memory, disk) over the specified time period |
| **ForecastWorker** | `cp_forecast` | Projects when current capacity will be exhausted based on the growth trend, with a confidence score |
| **RecommendWorker** | `cp_recommend` | Generates a scaling recommendation with node count and estimated monthly cost |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
cp_collect_metrics
    │
    ▼
cp_analyze_trends
    │
    ▼
cp_forecast
    │
    ▼
cp_recommend

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
java -jar target/capacity-planning-1.0.0.jar

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
java -jar target/capacity-planning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow capacity_planning_workflow \
  --version 1 \
  --input '{"service": "order-service", "period": "sample-period"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w capacity_planning_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one planning stage. plug in CloudWatch, Prometheus, or AWS Forecast for real utilization data and trend analysis, and the planning workflow runs unchanged.

- **AnalyzeTrendsWorker** (`cp_analyze_trends`): use Prometheus range queries, Datadog metric aggregation, or CloudWatch GetMetricStatistics to compute growth rates
- **CollectMetricsWorker** (`cp_collect_metrics`): pull utilization data from your cloud provider's monitoring API (AWS CloudWatch, GCP Monitoring, Azure Monitor)
- **ForecastWorker** (`cp_forecast`): integrate with a time-series forecasting library (Prophet, ARIMA) or cloud-native forecasting (AWS Forecast)

Connect to real CloudWatch or Prometheus metrics and the planning pipeline produces live forecasts without workflow changes.

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
capacity-planning-capacity-planning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/capacityplanning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeTrendsWorker.java
│       ├── CollectMetricsWorker.java
│       ├── ForecastWorker.java
│       └── RecommendWorker.java
└── src/test/java/capacityplanning/
    └── MainExampleTest.java        # 2 tests. workflow resource loading, worker instantiation

```
