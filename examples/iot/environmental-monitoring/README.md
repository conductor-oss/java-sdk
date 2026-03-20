# Environmental Monitoring in Java with Conductor :  Air Quality Sensing, Threshold Alerts, and Compliance Reporting

A Java Conductor workflow example that orchestrates environmental monitoring .  collecting air quality readings (PM2.5, PM10, CO2, NO2, ozone), checking pollutant concentrations against regulatory thresholds, triggering alerts when AQI breaches occur, and generating compliance reports. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Air Quality Monitoring Needs Orchestration

Environmental monitoring stations collect readings across multiple pollutants .  particulate matter (PM2.5, PM10), gases (CO2, NO2, ozone), and ambient conditions (temperature, humidity). Each reading must be checked against regulatory thresholds to compute an Air Quality Index. When the AQI exceeds safe levels, alerts go out to environmental teams and the monitoring dashboard. Regardless of alert status, a compliance report must be generated for regulatory review.

If a sensor reading fails mid-collection, you need to know which pollutants were already captured so you can retry without duplicating data. If threshold checking reveals a breach, the alert and report steps both need that information. Without orchestration, you'd build a monolithic monitoring script that mixes sensor polling, threshold logic, notification dispatch, and report generation .  making it impossible to swap out your sensor data source, test threshold rules independently, or audit which readings triggered which alerts.

## How This Workflow Solves It

**You just write the environmental monitoring workers. Sensor data collection, threshold checking, alert triggering, and compliance reporting. Conductor handles collection-to-report sequencing, sensor polling retries, and provable regulatory records linking every reading to every alert.**

Each monitoring concern is an independent worker .  collect sensor data, check thresholds, trigger alerts, generate reports. Conductor sequences them, passes pollutant readings and breach counts between steps, retries if a sensor poll times out, and provides a complete audit trail of every reading, threshold check, and alert for regulatory compliance.

### What You Write: Workers

Four workers run the monitoring cycle: CollectDataWorker reads pollutant concentrations from station sensors, CheckThresholdsWorker evaluates AQI against regulatory limits, TriggerAlertWorker dispatches breach notifications, and GenerateReportWorker produces the compliance record.

| Worker | Task | What It Does |
|---|---|---|
| **CheckThresholdsWorker** | `env_check_thresholds` | Compares pollutant readings against regulatory thresholds to compute AQI and identify breaches. |
| **CollectDataWorker** | `env_collect_data` | Collects air quality readings (PM2.5, PM10, CO2, NO2, ozone) from monitoring station sensors. |
| **GenerateReportWorker** | `env_generate_report` | Generates a regulatory compliance report with readings, breaches, and alert history. |
| **TriggerAlertWorker** | `env_trigger_alert` | Sends threshold breach alerts to environmental teams and monitoring dashboards. |

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
env_collect_data
    │
    ▼
env_check_thresholds
    │
    ▼
env_trigger_alert
    │
    ▼
env_generate_report
```

## Example Output

```
=== Example 539: Environmental Monitoring ===

Step 1: Registering task definitions...
  Registered: env_collect_data, env_check_thresholds, env_trigger_alert, env_generate_report

Step 2: Registering workflow 'environmental_monitoring_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [threshold] Processing
  [collect] Processing
  [report] Processing
  [alert] Processing

  Status: COMPLETED
  Output: {breachCount=..., aqi=..., readings=..., pm25=...}

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
java -jar target/environmental-monitoring-1.0.0.jar
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
java -jar target/environmental-monitoring-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow environmental_monitoring_workflow \
  --version 1 \
  --input '{"stationId": "ENV-STN-539-NORTH", "ENV-STN-539-NORTH": "region", "region": "Industrial Zone A", "Industrial Zone A": "monitoringType", "monitoringType": "air_quality", "air_quality": "sample-air-quality"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w environmental_monitoring_workflow -s COMPLETED -c 5
```

## How to Extend

Connect CollectDataWorker to your monitoring station sensors, CheckThresholdsWorker to your regulatory threshold database, and TriggerAlertWorker to your environmental compliance notification system. The workflow definition stays exactly the same.

- **CollectDataWorker** (`env_collect_data`): pull real sensor readings from your monitoring stations via Modbus, OPC-UA, or a sensor API (PurpleAir, AirVisual) to return PM2.5, PM10, CO2, NO2, ozone, temperature, and humidity values
- **CheckThresholdsWorker** (`env_check_thresholds`): compare readings against EPA/WHO regulatory limits, compute the AQI using the standard breakpoint formula, and return breach counts and severity levels
- **TriggerAlertWorker** (`env_trigger_alert`): send real notifications via email, SMS, or push to environmental teams and update your monitoring dashboard (Grafana, custom web UI) when thresholds are breached
- **GenerateReportWorker** (`env_generate_report`): generate regulatory compliance reports in PDF format and submit them to your environmental agency's reporting portal

Replace each worker with a production sensor gateway or alerting system while keeping the same output fields, and the monitoring pipeline runs unmodified.

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
environmental-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/environmentalmonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EnvironmentalMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckThresholdsWorker.java
│       ├── CollectDataWorker.java
│       ├── GenerateReportWorker.java
│       └── TriggerAlertWorker.java
└── src/test/java/environmentalmonitoring/workers/
    ├── CheckThresholdsWorkerTest.java        # 2 tests
    └── CollectDataWorkerTest.java        # 2 tests
```
