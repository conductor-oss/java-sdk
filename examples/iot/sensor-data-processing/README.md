# Sensor Data Processing in Java with Conductor :  Collection, Validation, Aggregation, Anomaly Detection, and Alerting

A Java Conductor workflow example that orchestrates a sensor data processing pipeline .  collecting batches of temperature and humidity readings from sensor groups, validating data quality (null checks, offline sensor detection), computing aggregate statistics (avg, min, max, std dev), detecting anomalies like temperature spikes above thresholds, and triggering escalation alerts via email and SMS when anomalies are found. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Sensor Data Pipelines Need Orchestration

Processing IoT sensor data at scale is a multi-stage pipeline where data quality and ordering matter. You collect 1,200 readings from 50 sensors in a time window. You validate each reading for data quality .  flagging null values, detecting offline sensors, and filtering bad data. You aggregate the validated readings into summary statistics: average temperature, min/max bounds, standard deviation, humidity averages. You analyze those aggregated metrics for patterns and anomalies ,  is the temperature trend rising? Did any sensor exceed the 85F threshold? If anomalies are detected, you trigger alerts at the appropriate escalation level.

Each stage depends on clean output from the previous one .  aggregation on validated data, pattern analysis on aggregated metrics, alerting on detected anomalies. If you aggregate raw data without validation, offline sensors inject zeros that skew your averages. If you skip pattern analysis and alert on raw readings, you get false positives from sensor noise. Without orchestration, you'd build a monolithic data processor that mixes ingestion, validation, statistics, and alerting ,  making it impossible to tune the anomaly detection threshold without risking the data validation logic.

## How This Workflow Solves It

**You just write the sensor pipeline workers. Data collection, validation, aggregation, anomaly detection, and alert escalation. Conductor handles validated-data-first ordering, sensor group retries, and traced records linking each alert to the readings and anomalies that caused it.**

Each pipeline stage is an independent worker .  collect readings, validate data, aggregate metrics, analyze patterns, trigger alerts. Conductor sequences them, passes the validated reading count and cleaned data between stages, retries if a sensor group poll times out, and tracks exactly which readings, metrics, and anomalies led to each alert.

### What You Write: Workers

Five workers process sensor streams: CollectReadingsWorker ingests batches from sensor groups, ValidateDataWorker filters bad data and offline sensors, AggregateReadingsWorker computes summary statistics, AnalyzePatternsWorker detects anomalies, and TriggerAlertsWorker escalates notifications.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateReadingsWorker** | `sen_aggregate_readings` | Aggregates validated sensor readings into summary metrics. |
| **AnalyzePatternsWorker** | `sen_analyze_patterns` | Analyzes aggregated sensor metrics for patterns and anomalies. |
| **CollectReadingsWorker** | `sen_collect_readings` | Collects sensor readings from a sensor group within a time window. |
| **TriggerAlertsWorker** | `sen_trigger_alerts` | Triggers alerts based on detected anomalies. |
| **ValidateDataWorker** | `sen_validate_data` | Validates sensor readings for data quality. |

Workers simulate device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs .  the workflow and alerting logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input -> AggregateReadingsWorker -> AnalyzePatternsWorker -> CollectReadingsWorker -> TriggerAlertsWorker -> ValidateDataWorker -> Output
```

## Example Output

```
=== Sensor Data Processing Demo ===

Step 1: Registering task definitions...
  Registered: sen_collect_readings, sen_validate_data, sen_aggregate_readings, sen_analyze_patterns, sen_trigger_alerts

Step 2: Registering workflow 'sensor_data_processing_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [aggregate] Aggregating
  [analyze] Max temp:
  [collect] Collecting readings from sensor group
  [alert] Trend:
  [validate] Validated

  Status: COMPLETED
  Output: {aggregatedMetrics=..., timeRange=..., trend=..., anomalies=...}

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
java -jar target/sensor-data-processing-1.0.0.jar
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
java -jar target/sensor-data-processing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sensor_data_processing \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sensor_data_processing -s COMPLETED -c 5
```

## How to Extend

Connect CollectReadingsWorker to your IoT message broker (MQTT, Kafka), ValidateDataWorker to your data quality rules engine, and TriggerAlertsWorker to your operations notification system (PagerDuty, OpsGenie). The workflow definition stays exactly the same.

- **CollectReadingsWorker** (`sen_collect_readings`): subscribe to your MQTT broker, query your time-series database (InfluxDB, TimescaleDB), or poll your IoT platform API to collect real sensor readings within a time window
- **ValidateDataWorker** (`sen_validate_data`): apply real data quality rules: range checks, null detection, offline sensor flagging, duplicate detection, and outlier filtering using configurable thresholds
- **AggregateReadingsWorker** (`sen_aggregate_readings`): compute real statistical aggregates (mean, median, percentiles, standard deviation) and write summary metrics to your time-series store or data warehouse
- **AnalyzePatternsWorker** (`sen_analyze_patterns`): run anomaly detection algorithms (Z-score, isolation forest, ARIMA forecasting) against the aggregated metrics to identify threshold breaches and trend deviations
- **TriggerAlertsWorker** (`sen_trigger_alerts`): send real alerts via PagerDuty, Opsgenie, or SNS, with escalation levels based on anomaly severity and notification routing rules

Swap each worker for a production sensor gateway or alerting service while keeping the same return fields, and the data pipeline requires no changes.

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
sensor-data-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sensordataprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SensorDataProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateReadingsWorker.java
│       ├── AnalyzePatternsWorker.java
│       ├── CollectReadingsWorker.java
│       ├── TriggerAlertsWorker.java
│       └── ValidateDataWorker.java
└── src/test/java/sensordataprocessing/workers/
    ├── AggregateReadingsWorkerTest.java        # 8 tests
    ├── AnalyzePatternsWorkerTest.java        # 8 tests
    ├── CollectReadingsWorkerTest.java        # 8 tests
    ├── TriggerAlertsWorkerTest.java        # 9 tests
    └── ValidateDataWorkerTest.java        # 8 tests
```
