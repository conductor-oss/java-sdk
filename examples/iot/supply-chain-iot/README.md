# Supply Chain IoT in Java with Conductor :  Shipment Tracking, Condition Monitoring, and Alert-Based Rerouting

A Java Conductor workflow example that orchestrates supply chain monitoring. tracking shipment location from origin to destination, monitoring in-transit environmental conditions (temperature for cold chain compliance), and routing to different handlers via SWITCH based on condition status: continue the shipment if conditions are normal, or trigger an alert and initiate rerouting if conditions breach thresholds. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Why Supply Chain Monitoring Needs Orchestration

Shipping perishable goods or sensitive materials requires continuous monitoring throughout transit. You track the shipment's GPS position to know where it is. You check the in-transit environmental conditions. temperature inside the container, humidity levels, door-open events. Based on those conditions, you take entirely different actions: if everything is within spec, you log the checkpoint and continue. If the temperature exceeds the cold chain threshold, you trigger an alert, notify the logistics team, and initiate rerouting to a closer destination before the cargo is compromised.

This is a classic conditional routing problem. The same shipment check can lead to "continue" or "reroute + alert," and those paths have completely different downstream effects. Without orchestration, you'd build a monolithic shipment monitor that mixes GPS polling, sensor reading, threshold checking, notification dispatch, and rerouting logic. making it impossible to add new condition types (shock, tilt, light exposure) without rewriting the core monitoring code.

## How This Workflow Solves It

**You just write the supply chain monitoring workers. Shipment tracking, condition monitoring, and conditional routing to OK or alert handlers. Conductor handles SWITCH-based condition routing, GPS tracker retries, and checkpoint records for cold chain compliance documentation.**

Each supply chain concern is an independent worker. track shipment, monitor conditions, handle normal status, handle alerts. Conductor sequences tracking and condition monitoring, then uses a SWITCH task to route to the correct handler based on condition status. If a GPS tracker poll times out, Conductor retries. Adding new condition types means adding a new SWITCH case and worker,  no changes to existing shipment tracking or alert logic.

### What You Write: Workers

Four workers monitor shipments in transit: TrackShipmentWorker tracks GPS position, MonitorConditionsWorker reads temperature and humidity from IoT sensors, HandleOkWorker logs compliant checkpoints, and HandleAlertWorker triggers rerouting when conditions breach thresholds.

| Worker | Task | What It Does |
|---|---|---|
| **HandleAlertWorker** | `sci_handle_alert` | Triggers alerts, notifies the logistics team, and initiates rerouting when conditions breach thresholds. |
| **HandleOkWorker** | `sci_handle_ok` | Logs a successful checkpoint when all in-transit conditions are within acceptable limits. |
| **MonitorConditionsWorker** | `sci_monitor_conditions` | Reads in-transit environmental conditions (temperature, humidity) from shipment IoT sensors. |
| **TrackShipmentWorker** | `sci_track_shipment` | Tracks the shipment's current GPS location between origin and destination. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
sci_track_shipment
    │
    ▼
sci_monitor_conditions
    │
    ▼
SWITCH (sci_switch_ref)
    ├── ok: sci_handle_ok
    ├── alert: sci_handle_alert

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
java -jar target/supply-chain-iot-1.0.0.jar

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
java -jar target/supply-chain-iot-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow supply_chain_iot_demo \
  --version 1 \
  --input '{"shipmentId": "TEST-001", "origin": "sample-origin", "destination": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w supply_chain_iot_demo -s COMPLETED -c 5

```

## How to Extend

Connect TrackShipmentWorker to your GPS tracking provider, MonitorConditionsWorker to your in-transit IoT sensors, and HandleAlertWorker to your logistics rerouting and dispatch system. The workflow definition stays exactly the same.

- **TrackShipmentWorker** (`sci_track_shipment`): query your carrier tracking API (FedEx, UPS, DHL) or GPS tracker platform to get real-time shipment location, ETA, and transit status
- **MonitorConditionsWorker** (`sci_monitor_conditions`): read real sensor data from in-transit IoT devices (Sensitech, Emerson, Tive) for temperature, humidity, shock, and door-open events
- **HandleOkWorker** (`sci_handle_ok`): log the successful checkpoint to your shipment management system (TMS) and update the customer-facing tracking dashboard
- **HandleAlertWorker** (`sci_handle_alert`): trigger real alerts via SMS/email to the logistics team, create an incident in your TMS, and call the carrier API to initiate rerouting to the nearest compliant facility

Wire each worker to your shipment tracking platform or cold chain sensors while maintaining the same output fields, and the conditional routing remains unchanged.

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
supply-chain-iot/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/supplychainiot/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SupplyChainIotExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── HandleAlertWorker.java
│       ├── HandleOkWorker.java
│       ├── MonitorConditionsWorker.java
│       └── TrackShipmentWorker.java
└── src/test/java/supplychainiot/workers/
    ├── MonitorConditionsWorkerTest.java        # 2 tests
    └── TrackShipmentWorkerTest.java        # 2 tests

```
