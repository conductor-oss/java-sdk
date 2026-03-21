# Wearable Health Data Pipeline in Java with Conductor :  Vitals Collection, Processing, Anomaly Detection, and Notifications

A Java Conductor workflow example that orchestrates a wearable health data pipeline. collecting vital signs from wearable devices (heart rate, SpO2, step count, sleep data), processing raw readings into derived metrics (average heart rate, activity summaries), detecting anomalies in vital signs (irregular heartbeat, abnormal SpO2 drops), and notifying users or caregivers when health anomalies are found. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Wearable Health Pipelines Need Orchestration

Processing health data from wearable devices involves a pipeline where each stage builds on the previous one. You sync raw vitals from the wearable. heart rate samples, blood oxygen readings, accelerometer data, sleep stages. You process those raw readings into meaningful metrics: average heart rate over the collection window, resting vs: active heart rate zones, step counts normalized to the user's baseline. You run anomaly detection against the processed data to flag irregular patterns,  sustained elevated heart rate without activity, SpO2 drops below safe thresholds, abnormal sleep fragmentation. If anomalies are detected, you notify the user, their healthcare provider, or emergency contacts depending on severity.

Each stage depends on clean data from the previous one. Anomaly detection on raw, unprocessed samples would produce false positives from sensor noise and motion artifacts. Notifications without anomaly context would be meaningless. Without orchestration, you'd build a monolithic health processor that mixes BLE data sync, signal processing, statistical analysis, and push notification logic. making it impossible to upgrade your anomaly detection model without risking the data processing pipeline.

## How This Workflow Solves It

**You just write the wearable health workers. Vitals collection, data processing, anomaly detection, and caregiver notification. Conductor handles vitals-to-notification sequencing, device sync retries, and a complete audit trail for clinical review of every reading and anomaly.**

Each pipeline stage is an independent worker. collect vitals, process data, detect anomalies, notify. Conductor sequences them, passes raw readings through processing into anomaly detection and notification, retries if a device sync times out, and maintains a complete audit trail of every vital sign reading, derived metric, and anomaly alert for clinical review.

### What You Write: Workers

Four workers process wearable health data: CollectVitalsWorker syncs heart rate, SpO2, and step counts from devices, ProcessDataWorker derives meaningful metrics and baselines, DetectAnomaliesWorker flags irregular vital sign patterns, and NotifyWorker alerts users or caregivers based on severity.

| Worker | Task | What It Does |
|---|---|---|
| **CollectVitalsWorker** | `wer_collect_vitals` | Syncs raw vital signs from the wearable device. heart rate, SpO2, step count, and temperature. |
| **DetectAnomaliesWorker** | `wer_detect_anomalies` | Flags irregular patterns. sustained elevated heart rate, abnormal SpO2 drops, or unusual readings. |
| **NotifyWorker** | `wer_notify` | Sends health anomaly notifications to the user, caregiver, or healthcare provider based on severity. |
| **ProcessDataWorker** | `wer_process_data` | Processes raw readings into derived metrics. average heart rate, activity summaries, and baseline comparisons. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
wer_collect_vitals
    │
    ▼
wer_process_data
    │
    ▼
wer_detect_anomalies
    │
    ▼
wer_notify

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
java -jar target/wearable-data-1.0.0.jar

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
java -jar target/wearable-data-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wearable_data_demo \
  --version 1 \
  --input '{"userId": "TEST-001", "deviceId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wearable_data_demo -s COMPLETED -c 5

```

## How to Extend

Connect CollectVitalsWorker to your wearable device SDK (Apple HealthKit, Google Fit, Garmin), DetectAnomaliesWorker to your clinical analytics engine, and NotifyWorker to your patient and provider notification system. The workflow definition stays exactly the same.

- **CollectVitalsWorker** (`wer_collect_vitals`): sync real vital signs from wearable device APIs (Apple HealthKit, Google Fit, Fitbit Web API, Garmin Health API) or BLE-connected medical devices to collect heart rate, SpO2, activity, and sleep data
- **ProcessDataWorker** (`wer_process_data`): apply signal processing to raw readings: filter motion artifacts, compute rolling averages, classify heart rate zones, and normalize activity metrics against user baselines
- **DetectAnomaliesWorker** (`wer_detect_anomalies`): run anomaly detection models (threshold-based rules, Z-score analysis, or trained ML classifiers) to flag irregular heartbeat patterns, dangerous SpO2 drops, or abnormal sleep fragmentation
- **NotifyWorker** (`wer_notify`): send real notifications via push (FCM/APNS), SMS, or email to the user, caregiver, or healthcare provider with anomaly details and recommended actions

Swap each worker for a production BLE sync or clinical analytics engine while maintaining the same return fields, and the health pipeline needs no modifications.

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
wearable-data/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/wearabledata/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WearableDataExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectVitalsWorker.java
│       ├── DetectAnomaliesWorker.java
│       ├── NotifyWorker.java
│       └── ProcessDataWorker.java
└── src/test/java/wearabledata/workers/
    ├── CollectVitalsWorkerTest.java        # 2 tests
    └── ProcessDataWorkerTest.java        # 2 tests

```
