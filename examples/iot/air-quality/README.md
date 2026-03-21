# Air Quality in Java with Conductor

A Java Conductor workflow example that orchestrates air quality monitoring. collecting pollutant readings (PM2.5, PM10, ozone, CO) from monitoring stations, evaluating concentrations against air quality standards to compute an AQI category, and routing to different response handlers via SWITCH based on whether conditions are good, moderate, or poor. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Air Quality Monitoring Needs Orchestration

Monitoring air quality requires a pipeline that collects pollutant data, evaluates it against standards, and takes different actions depending on the result. You collect readings from a monitoring station. PM2.5, PM10, ozone, and CO concentrations for a given region. You check those readings against air quality standards to compute an AQI score and categorize conditions as good, moderate, or poor. Based on the category, you route to entirely different response handlers: log a routine checkpoint for good air, issue a sensitive-groups advisory for moderate conditions, or broadcast a public health warning for poor air quality.

This is a natural fit for conditional routing. The same set of readings can lead to routine logging, targeted advisories, or emergency health warnings. each with different notification recipients and follow-up actions. Without orchestration, you'd build a monolithic air quality processor that mixes sensor polling, AQI calculation, and multi-tier notification dispatch in one class, using if/else chains to decide which alert to send. Adding new response tiers (hazardous, very unhealthy) would require modifying the core processor.

## The Solution

**You just write the air quality workers. Pollutant collection, AQI evaluation, and tier-specific response handlers. Conductor handles AQI-based conditional routing, sensor polling retries, and complete records linking each reading to its response action.**

Each worker handles one IoT operation. data ingestion, threshold analysis, device command, or alert dispatch. Conductor manages the telemetry pipeline, device state tracking, and alert escalation.

### What You Write: Workers

Five workers monitor air quality: CollectReadingsWorker gathers pollutant concentrations, CheckStandardsWorker computes AQI scores, and three response workers. ActionGoodWorker, ActionModerateWorker, and ActionPoorWorker. Handle tier-specific actions via SWITCH routing.

| Worker | Task | What It Does |
|---|---|---|
| **ActionGoodWorker** | `aq_action_good` | Logs a routine checkpoint when AQI indicates good air quality. |
| **ActionModerateWorker** | `aq_action_moderate` | Issues a health advisory for sensitive groups when AQI is moderate. |
| **ActionPoorWorker** | `aq_action_poor` | Broadcasts a public health warning when AQI indicates poor air quality. |
| **CheckStandardsWorker** | `aq_check_standards` | Evaluates PM2.5, PM10, ozone, and CO concentrations against air quality standards to compute an AQI score and category. |
| **CollectReadingsWorker** | `aq_collect_readings` | Collects pollutant readings (PM2.5, PM10, ozone, CO) from a monitoring station. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
aq_collect_readings
    │
    ▼
aq_check_standards
    │
    ▼
SWITCH (aq_switch_ref)
    ├── good: aq_action_good
    ├── moderate: aq_action_moderate
    ├── poor: aq_action_poor

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
java -jar target/air-quality-1.0.0.jar

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
java -jar target/air-quality-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow air_quality_demo \
  --version 1 \
  --input '{"stationId": "TEST-001", "region": "us-east-1"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w air_quality_demo -s COMPLETED -c 5

```

## How to Extend

Connect CollectReadingsWorker to your monitoring station sensors, CheckStandardsWorker to your regulatory AQI calculation engine, and the action workers to your public alert broadcast system. The workflow definition stays exactly the same.

- **CollectReadingsWorker** (`aq_collect_readings`): pull real pollutant data from air quality sensors (PurpleAir, AirVisual) or government APIs (EPA AirNow, OpenAQ) for PM2.5, PM10, ozone, and CO readings
- **CheckStandardsWorker** (`aq_check_standards`): compute the real AQI using EPA breakpoint tables, compare against NAAQS or WHO guidelines, and return the official category and health messaging
- **ActionGoodWorker** (`aq_action_good`): log the routine checkpoint to your environmental monitoring dashboard and update the public-facing air quality display
- **ActionModerateWorker** (`aq_action_moderate`): send targeted advisories to sensitive populations (schools, hospitals, elderly care facilities) via SMS, email, or push notifications
- **ActionPoorWorker** (`aq_action_poor`): broadcast emergency health warnings through public alert systems, notify local health departments, and trigger HVAC filtration mode in connected buildings

Wire each worker to real monitoring station APIs or notification systems while preserving output fields, and the SWITCH-based routing stays intact.

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
air-quality/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/airquality/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AirQualityExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActionGoodWorker.java
│       ├── ActionModerateWorker.java
│       ├── ActionPoorWorker.java
│       ├── CheckStandardsWorker.java
│       └── CollectReadingsWorker.java
└── src/test/java/airquality/workers/
    ├── CheckStandardsWorkerTest.java        # 2 tests
    └── CollectReadingsWorkerTest.java        # 2 tests

```
