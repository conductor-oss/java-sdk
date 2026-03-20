# Capacity Monitoring in Java Using Conductor :  Resource Measurement, Forecasting, and Capacity Alerts

A Java Conductor workflow example for capacity monitoring .  measuring current resource utilization across clusters, forecasting future capacity needs, and alerting when capacity thresholds are breached or exhaustion is predicted.

## The Problem

You need to monitor infrastructure capacity. CPU, memory, disk, network .  across your clusters. Beyond current utilization, you need to forecast when resources will run out based on growth trends. If current usage exceeds thresholds or the forecast predicts exhaustion within your planning window, capacity alerts must fire so you can provision before outages occur.

Without orchestration, capacity monitoring is a dashboard that shows current state but doesn't predict. Forecasting runs separately from measurement, uses stale data, and alerting is disconnected from both. By the time someone notices a capacity issue, it's already causing production problems.

## The Solution

**You just write the resource measurement and capacity forecasting logic. Conductor handles the measure-forecast-alert pipeline, retries when cluster metric endpoints are slow, and historical tracking of capacity trends over time.**

Each capacity concern is an independent worker .  resource measurement, growth forecasting, and alerting. Conductor runs them in sequence: measure current state, forecast future needs, then alert if thresholds are breached. Every monitoring run is tracked with measurements, forecasts, and alert decisions. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers monitor infrastructure capacity: MeasureResourcesWorker samples CPU, memory, and disk utilization, ForecastWorker predicts days until exhaustion based on growth trends, and CapAlertWorker fires when capacity thresholds are breached or exhaustion is imminent.

| Worker | Task | What It Does |
|---|---|---|
| **CapAlertWorker** | `cap_alert` | Sends a capacity alert if forecasted disk exhaustion is within 30 days, with severity based on urgency |
| **ForecastWorker** | `cap_forecast` | Forecasts days until CPU, memory, and disk exhaustion based on current usage trends, with scaling recommendations |
| **MeasureResourcesWorker** | `cap_measure_resources` | Measures current CPU, memory, and disk utilization percentages and node count for a cluster |

Workers simulate scheduled operations with realistic outputs so you can see the scheduling pattern without external systems. Replace with real job logic .  the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
cap_measure_resources
    │
    ▼
cap_forecast
    │
    ▼
cap_alert
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
java -jar target/capacity-monitoring-1.0.0.jar
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
java -jar target/capacity-monitoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow capacity_monitoring_418 \
  --version 1 \
  --input '{"cluster": "test-value", "forecastDays": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w capacity_monitoring_418 -s COMPLETED -c 5
```

## How to Extend

Each worker handles one monitoring phase .  connect the measurement worker to CloudWatch or Prometheus, the alerting worker to PagerDuty, and the measure-forecast-alert workflow stays the same.

- **CapAlertWorker** (`cap_alert`): send capacity alerts via PagerDuty when critical, Slack for warnings, and auto-create Jira tickets for capacity planning
- **ForecastWorker** (`cap_forecast`): implement capacity forecasting using linear regression, exponential smoothing, or call a capacity planning service
- **MeasureResourcesWorker** (`cap_measure_resources`): query Kubernetes metrics API, CloudWatch, Prometheus, or Datadog for real CPU/memory/disk/network utilization

Point at your Kubernetes or cloud provider metrics, and the measurement-to-alert pipeline adapts without any orchestration changes.

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
capacity-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/capacitymonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CapacityMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CapAlertWorker.java
│       ├── ForecastWorker.java
│       └── MeasureResourcesWorker.java
└── src/test/java/capacitymonitoring/workers/
    └── CapAlertWorkerTest.java        # 3 tests
```
