# Cold Chain Monitoring in Java with Conductor :  Temperature Sensing, Threshold Checks, Alert Routing, and Corrective Action

A Java Conductor workflow example for cold chain monitoring .  reading temperature sensor data for shipments of temperature-sensitive goods (e.g., frozen pharmaceuticals requiring 2-8C), checking readings against configured thresholds, routing to alert or OK handlers based on compliance status, and triggering corrective actions when excursions are detected. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to continuously monitor temperature conditions for cold chain shipments. Frozen pharmaceuticals, vaccines, and biologics must stay within a narrow temperature range (e.g., 2-8C) throughout transit. When a sensor reading breaches the threshold, the logistics team needs an immediate alert, and corrective action must be triggered .  rerouting to a backup refrigerated warehouse, dispatching a replacement shipment, or flagging the batch for quality review. Regulatory requirements (FDA 21 CFR Part 211, EU GDP) demand a documented chain of custody proving temperatures were maintained.

Without orchestration, you'd poll IoT sensors in a cron job, compare readings in a script, and send alerts via a separate notification service. If the alert service is down when a temperature excursion happens, the breach goes unnoticed and a $500K pharmaceutical shipment is ruined. There is no unified record of when the excursion started, when the alert was sent, or what corrective action was taken.

## The Solution

**You just write the cold chain workers. Sensor reads, threshold checks, alert dispatch, and corrective action triggers. Conductor handles sequencing, conditional routing, retries, and timestamped audit trails for FDA/GDP regulatory compliance.**

Each step in the cold chain monitoring pipeline is a simple, independent worker .  a plain Java class that does one thing. Conductor sequences them so sensor readings are checked against thresholds, then routes to either the OK handler (logging compliance) or the alert handler (triggering corrective action) based on the result. If the alert worker fails, Conductor retries it until the logistics team is notified. Every temperature reading, threshold comparison, and corrective action is recorded with timestamps for regulatory audit trails.

### What You Write: Workers

Five workers cover the cold chain pipeline: MonitorTempWorker reads sensor data, CheckThresholdsWorker evaluates compliance, HandleOkWorker logs normal checkpoints, HandleAlertWorker dispatches excursion notifications, and ActWorker triggers corrective actions.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `cch_act` | Takes corrective action based on the temperature status .  rerouting, replacement, or quality review. |
| **CheckThresholdsWorker** | `cch_check_thresholds` | Compares the current temperature against min/max thresholds and returns compliance status. |
| **HandleAlertWorker** | `cch_handle_alert` | Triggers an alert and notifies the logistics team when a temperature excursion is detected. |
| **HandleOkWorker** | `cch_handle_ok` | Logs a compliant checkpoint when temperature is within the acceptable range. |
| **MonitorTempWorker** | `cch_monitor_temp` | Reads the current temperature from the shipment's IoT sensor. |

Workers simulate supply chain operations .  inventory checks, shipment tracking, supplier coordination ,  with realistic outputs. Replace with real ERP and logistics integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
cch_monitor_temp
    │
    ▼
cch_check_thresholds
    │
    ▼
SWITCH (cch_switch_ref)
    ├── ok: cch_handle_ok
    └── default: cch_handle_alert
    │
    ▼
cch_act
```

## Example Output

```
=== Example 670: Cold Chain Monitoring ===

Step 1: Registering task definitions...
  Registered: cch_monitor_temp, cch_check_thresholds, cch_handle_ok, cch_handle_alert, cch_act

Step 2: Registering workflow 'cch_cold_chain'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [act]
  [check]
  [alert]
  [ok]
  [monitor]

  Status: COMPLETED
  Output: {action=..., logged=..., notified=..., currentTemp=...}

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
java -jar target/cold-chain-1.0.0.jar
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
java -jar target/cold-chain-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cch_cold_chain \
  --version 1 \
  --input '{"shipmentId": "COLD-670-001", "COLD-670-001": "product", "product": "Frozen Pharmaceuticals", "Frozen Pharmaceuticals": "minTemp", "minTemp": 2}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cch_cold_chain -s COMPLETED -c 5
```

## How to Extend

Point MonitorTempWorker at your real IoT gateway (AWS IoT Core, Azure IoT Hub) and HandleAlertWorker at your logistics dispatch system to go live. The workflow definition stays exactly the same.

- **MonitorTempWorker** (`cch_monitor_temp`): read real sensor data from IoT platforms (AWS IoT Core, Azure IoT Hub) or cold chain hardware APIs (Sensitech, Emerson GO Real-Time)
- **CheckThresholdsWorker** (`cch_check_thresholds`): compare readings against product-specific thresholds stored in your product master data (e.g., 2-8C for vaccines, -20C for biologics)
- **HandleOkWorker** (`cch_handle_ok`): log compliant readings to your cold chain compliance database for GDP/FDA audit evidence
- **HandleAlertWorker** (`cch_handle_alert`): send excursion alerts via SMS/email to the logistics team and create an incident ticket in ServiceNow or your quality management system
- **ActWorker** (`cch_act`): trigger corrective actions: reroute the shipment to the nearest compliant facility, dispatch a replacement, or flag the batch for quality hold in your WMS

As long as each worker returns the same fields, the workflow definition and SWITCH routing remain untouched.

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
cold-chain/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/coldchain/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ColdChainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActWorker.java
│       ├── CheckThresholdsWorker.java
│       ├── HandleAlertWorker.java
│       ├── HandleOkWorker.java
│       └── MonitorTempWorker.java
└── src/test/java/coldchain/workers/
    ├── ActWorkerTest.java        # 2 tests
    ├── CheckThresholdsWorkerTest.java        # 2 tests
    ├── HandleAlertWorkerTest.java        # 2 tests
    ├── HandleOkWorkerTest.java        # 2 tests
    └── MonitorTempWorkerTest.java        # 2 tests
```
