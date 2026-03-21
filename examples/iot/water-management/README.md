# Water Management in Java with Conductor :  Level Monitoring, Quality Analysis, Leak Detection, and Alerting

A Java Conductor workflow example that orchestrates water infrastructure monitoring. reading water levels from reservoir and distribution sensors, analyzing water quality parameters (pH, turbidity, chlorine, contaminants), detecting leaks in the distribution network using pressure and flow anomalies, and triggering alerts when issues are found. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## Why Water Infrastructure Monitoring Needs Orchestration

Managing a water distribution network requires a monitoring pipeline where each check feeds into the next. You read water levels across the zone. reservoir capacity, tank fill percentages, distribution pressure. You analyze water quality at multiple points in the network to detect contamination, pH drift, or chlorine depletion. You run leak detection algorithms that correlate pressure drops and flow rate anomalies to identify pipe segments losing water. If any check finds an issue,  low levels, quality violations, or a high-confidence leak,  you trigger alerts to the operations team.

Each stage depends on context from earlier stages. Leak detection needs pressure and flow data alongside level readings. Alerting needs to know whether the issue is a quality violation (which requires a different response team than a leak). Without orchestration, you'd build a monolithic SCADA script that mixes sensor polling, water chemistry analysis, hydraulic modeling, and notification dispatch. making it impossible to upgrade your leak detection algorithm without risking the quality analysis logic, or to audit which sensor reading triggered which alert.

## How This Workflow Solves It

**You just write the water infrastructure workers. Level monitoring, quality analysis, leak detection, and operations alerting. Conductor handles level-to-alert sequencing, remote sensor retries, and audit trails linking every alert to its triggering readings.**

Each monitoring concern is an independent worker. monitor levels, analyze quality, detect leaks, trigger alerts. Conductor sequences them, passes sensor readings and quality results between stages, retries if a remote sensor is temporarily unreachable, and maintains an audit trail linking every alert to the specific readings that triggered it.

### What You Write: Workers

Four workers monitor the water network: MonitorLevelsWorker reads reservoir and distribution pressure, AnalyzeQualityWorker checks pH, turbidity, and chlorine levels, DetectLeaksWorker correlates pressure drops with flow anomalies, and AlertWorker notifies operations when issues are found.

| Worker | Task | What It Does |
|---|---|---|
| **AlertWorker** | `wtr_alert` | Triggers alerts to operations when quality violations or leaks are detected. |
| **AnalyzeQualityWorker** | `wtr_analyze_quality` | Analyzes water quality parameters (pH, turbidity, chlorine, contaminants) against safety standards. |
| **DetectLeaksWorker** | `wtr_detect_leaks` | Detects leaks by correlating pressure drops and flow rate anomalies across the distribution network. |
| **MonitorLevelsWorker** | `wtr_monitor_levels` | Reads water levels, flow rates, and pressure from reservoir and distribution sensors. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
wtr_monitor_levels
    │
    ▼
wtr_analyze_quality
    │
    ▼
wtr_detect_leaks
    │
    ▼
wtr_alert

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
java -jar target/water-management-1.0.0.jar

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
java -jar target/water-management-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow water_management_demo \
  --version 1 \
  --input '{"zoneId": "TEST-001", "sensorGroup": "sample-sensorGroup"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w water_management_demo -s COMPLETED -c 5

```

## How to Extend

Connect MonitorLevelsWorker to your SCADA system sensors, AnalyzeQualityWorker to your water quality testing instruments, and DetectLeaksWorker to your hydraulic pressure analysis platform. The workflow definition stays exactly the same.

- **MonitorLevelsWorker** (`wtr_monitor_levels`): read real water level data from SCADA systems, pressure transducers, or ultrasonic level sensors via Modbus/OPC-UA to return reservoir capacity, tank levels, and distribution pressure
- **AnalyzeQualityWorker** (`wtr_analyze_quality`): pull real water quality measurements from inline analyzers or lab results (pH, turbidity, chlorine residual, contaminant levels) and compare against EPA/WHO drinking water standards
- **DetectLeaksWorker** (`wtr_detect_leaks`): run leak detection algorithms using minimum night flow analysis, pressure transient monitoring, or acoustic sensor data to locate pipe segments with losses and return confidence scores
- **AlertWorker** (`wtr_alert`): send real notifications via SCADA alarm systems, SMS to field crews, or email to water utility operations, with different escalation paths for quality violations vs, infrastructure leaks

Connect each worker to your SCADA system or water quality sensors while preserving output fields, and the monitoring pipeline runs without changes.

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
water-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/watermanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WaterManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AlertWorker.java
│       ├── AnalyzeQualityWorker.java
│       ├── DetectLeaksWorker.java
│       └── MonitorLevelsWorker.java
└── src/test/java/watermanagement/workers/
    ├── AnalyzeQualityWorkerTest.java        # 2 tests
    └── MonitorLevelsWorkerTest.java        # 2 tests

```
