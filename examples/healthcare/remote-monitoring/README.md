# Remote Patient Monitoring in Java Using Conductor :  Vital Signs Collection, Trend Analysis, Alert Routing, and Clinical Action

A Java Conductor workflow example for remote patient monitoring (RPM). collecting vital signs from connected devices, analyzing trends against the patient's baseline, routing to normal or alert pathways via SWITCH, and triggering the appropriate clinical action. Uses [Conductor](https://github.

## The Problem

You need to monitor patients remotely using connected medical devices. Vital signs. blood pressure, heart rate, blood glucose, weight, oxygen saturation,  are transmitted from the patient's home device. Those readings must be analyzed against the patient's individual baseline and clinical thresholds to detect concerning trends (rising blood pressure over 3 days, weight gain suggesting fluid retention in CHF patients, glucose spikes in diabetics). Based on the analysis, the system must route to different clinical actions,  log the reading as normal, or trigger an alert that notifies the care team and may schedule an intervention. A missed alert on a deteriorating trend can result in an avoidable hospitalization or emergency visit.

Without orchestration, you'd build a monolithic monitoring service that polls device data, runs the trend analysis, branches with if/else into normal or alert paths, and sends notifications. If the device data platform is temporarily unavailable, you'd need retry logic. If the system crashes after detecting an alert but before notifying the care team, the patient's deterioration goes unaddressed. CMS RPM billing codes (99453, 99454, 99457) require documentation of every monitoring interaction.

## The Solution

**You just write the RPM workers. Vital signs collection, trend analysis, and conditional routing to normal logging or clinical alert actions. Conductor handles conditional SWITCH routing between normal and alert paths, automatic retries when the device platform is down, and complete monitoring records for RPM billing.**

Each stage of the monitoring cycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of collecting vitals before analyzing trends, routing to the correct clinical action (normal or alert) via SWITCH based on trend analysis, retrying if the device platform is temporarily unavailable, and maintaining a complete record of every monitoring cycle for RPM billing and clinical documentation. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the RPM cycle: CollectVitalsWorker retrieves device readings, AnalyzeTrendsWorker evaluates against baselines, NormalActionWorker logs compliant readings, and AlertActionWorker triggers clinical notifications when trends are concerning.

| Worker | Task | What It Does |
|---|---|---|
| **CollectVitalsWorker** | `rpm_collect_vitals` | Retrieves the latest vital signs (BP, HR, SpO2, glucose, weight) from the patient's connected device |
| **AnalyzeTrendsWorker** | `rpm_analyze_trends` | Analyzes readings against the patient's baseline and clinical thresholds, returns "normal" or "alert" |
| **NormalActionWorker** | `rpm_normal_action` | Logs the reading as within normal limits and updates the patient's monitoring dashboard |
| **AlertActionWorker** | `rpm_alert_action` | Triggers a clinical alert. notifies the care team, flags for intervention, and may schedule a follow-up visit |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

### The Workflow

```
rpm_collect_vitals
    │
    ▼
rpm_analyze_trends
    │
    ▼
SWITCH (rpm_switch_ref)
    ├── normal: rpm_normal_action
    ├── alert: rpm_alert_action
    └── default: rpm_alert_action

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
java -jar target/remote-monitoring-1.0.0.jar

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
java -jar target/remote-monitoring-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow remote_monitoring_workflow \
  --version 1 \
  --input '{"patientId": "TEST-001", "deviceId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w remote_monitoring_workflow -s COMPLETED -c 5

```

## How to Extend

Connect CollectVitalsWorker to your device data platform (Validic, Biobeat), AnalyzeTrendsWorker to your clinical analytics engine, and AlertActionWorker to your care team notification and intervention system. The workflow definition stays exactly the same.

- **CollectVitalsWorker** → integrate with your device data aggregation platform (Validic, Glooko, Biobeat) or BLE/cellular-connected devices
- **AnalyzeTrendsWorker** → implement clinical trend algorithms (rolling averages, rate of change, deviation from baseline) with patient-specific thresholds
- **AlertActionWorker** → send real alerts to care team via secure messaging, EHR inbox, or pager; auto-schedule telehealth follow-ups
- **NormalActionWorker** → log readings to the patient's chart via FHIR Observation resources and update their RPM dashboard
- Add a **BillingWorker** to track monitoring time for CMS RPM billing codes (99453, 99454, 99457, 99458)
- Add a **DeviceCheckWorker** to verify device connectivity and battery status, alerting if the patient has not transmitted readings

Swap in your device data platform and clinical alerting system while keeping the same output fields, and the monitoring workflow: including SWITCH routing, continues without modification.

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
remote-monitoring/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/remotemonitoring/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RemoteMonitoringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AlertActionWorker.java
│       ├── AnalyzeTrendsWorker.java
│       ├── CollectVitalsWorker.java
│       └── NormalActionWorker.java
└── src/test/java/remotemonitoring/workers/
    ├── AlertActionWorkerTest.java        # 2 tests
    ├── AnalyzeTrendsWorkerTest.java        # 2 tests
    ├── CollectVitalsWorkerTest.java        # 2 tests
    └── NormalActionWorkerTest.java        # 2 tests

```
