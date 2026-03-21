# Industrial IoT in Java with Conductor :  Machine Telemetry, Predictive Failure Analysis, and Repair Scheduling

A Java Conductor workflow example that orchestrates industrial equipment monitoring. collecting machine telemetry (temperature, vibration, pressure, RPM, oil level, run hours), running predictive failure analysis to identify at-risk components and estimate remaining useful life, and automatically scheduling repair work orders with parts lists and downtime estimates. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Predictive Maintenance Pipelines Need Orchestration

Industrial machines generate a constant stream of telemetry. bearing temperature at 185F, vibration at 4.2 mm/s, oil level at 72%. Turning that raw data into actionable maintenance decisions requires a pipeline. You collect telemetry from the machine's sensors. You feed those readings into a predictive model that estimates failure probability, identifies the likely component (bearing assembly, pump seal, motor winding), and predicts remaining useful life. If the failure probability exceeds a threshold, you schedule a repair work order with the right parts kit, a target date before predicted failure, and an estimated downtime window.

Each step depends on the previous one. the predictive model needs current telemetry, and the repair scheduler needs the model's output to know which parts to order and when to schedule downtime. If the telemetry pull from the PLC fails, you need to retry without sending stale predictions to the scheduler. Without orchestration, you'd build a monolithic condition monitoring system that mixes OPC-UA data collection, ML inference, and CMMS integration,  making it impossible to swap predictive models, test scheduling logic independently, or audit which telemetry readings triggered which work orders.

## How This Workflow Solves It

**You just write the industrial IoT workers. Machine telemetry collection, predictive failure analysis, and repair work order scheduling. Conductor handles telemetry-to-work-order sequencing, PLC connection retries, and complete records linking every work order to the readings that triggered it.**

Each stage of the predictive maintenance pipeline is an independent worker. monitor machines, run predictive analysis, schedule repairs. Conductor sequences them, passes telemetry data to the prediction model and prediction results to the repair scheduler, retries if a PLC connection times out, and keeps a complete audit trail linking every work order back to the specific telemetry readings that triggered it.

### What You Write: Workers

Three workers form the predictive maintenance pipeline: MonitorMachinesWorker collects temperature, vibration, and pressure telemetry, PredictiveAnalysisWorker estimates failure probability and identifies at-risk components, and ScheduleRepairWorker creates work orders with parts lists and downtime windows.

| Worker | Task | What It Does |
|---|---|---|
| **MonitorMachinesWorker** | `iit_monitor_machines` | Collects machine telemetry (temperature, vibration, pressure, RPM, oil level, run hours) and computes an anomaly score. |
| **PredictiveAnalysisWorker** | `iit_predictive_analysis` | Runs predictive failure analysis to estimate failure probability, identify the at-risk component, and assess urgency. |
| **ScheduleRepairWorker** | `iit_schedule_repair` | Creates a repair work order with parts list, target date, and estimated downtime based on prediction results. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
iit_monitor_machines
    │
    ▼
iit_predictive_analysis
    │
    ▼
iit_schedule_repair

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
java -jar target/industrial-iot-1.0.0.jar

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
java -jar target/industrial-iot-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow industrial_iot_workflow \
  --version 1 \
  --input '{"plantId": "TEST-001", "machineId": "TEST-001", "machineType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w industrial_iot_workflow -s COMPLETED -c 5

```

## How to Extend

Connect MonitorMachinesWorker to your PLC/OPC-UA data layer, PredictiveAnalysisWorker to your ML failure prediction model, and ScheduleRepairWorker to your CMMS (SAP PM, Maximo). The workflow definition stays exactly the same.

- **MonitorMachinesWorker** (`iit_monitor_machines`): poll real machine telemetry from your SCADA system or PLC via OPC-UA, Modbus, or an industrial gateway (Kepware, Ignition) to return temperature, vibration, pressure, RPM, oil level, and accumulated run hours
- **PredictiveAnalysisWorker** (`iit_predictive_analysis`): call your ML model endpoint (SageMaker, TensorFlow Serving, custom service) with the telemetry data to get failure probability, predicted failing component, remaining useful life estimate, and model confidence score
- **ScheduleRepairWorker** (`iit_schedule_repair`): create a work order in your CMMS (SAP PM, Maximo, eMaint) with the required parts kit, schedule maintenance before the predicted failure date, and calculate expected downtime

Connect each worker to your PLC data source or CMMS while preserving output fields, and the maintenance pipeline operates without modification.

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
industrial-iot/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/industrialiot/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── IndustrialIotExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MonitorMachinesWorker.java
│       ├── PredictiveAnalysisWorker.java
│       └── ScheduleRepairWorker.java
└── src/test/java/industrialiot/workers/
    ├── MonitorMachinesWorkerTest.java        # 2 tests
    └── PredictiveAnalysisWorkerTest.java        # 2 tests

```
