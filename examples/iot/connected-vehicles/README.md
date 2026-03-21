# Connected Vehicles in Java with Conductor

A Java Conductor workflow example that orchestrates connected vehicle monitoring .  collecting vehicle telemetry (engine RPM, fuel level, battery voltage, speed), running on-board diagnostics to assess overall vehicle health, tracking geolocation, and compiling a comprehensive vehicle status report combining health, location, and speed data. Uses [Conductor](https://github.

## Why Connected Vehicle Monitoring Needs Orchestration

Monitoring a connected vehicle requires collecting data from multiple on-board systems and combining it into an actionable status report. You pull telemetry from the vehicle's OBD-II or CAN bus .  engine RPM, fuel level, battery voltage, and speed. You run diagnostics against those readings to assess overall vehicle health (engine performance, electrical system, fuel efficiency). You track the vehicle's geolocation using its GPS module. Finally, you compile health status, location, and speed into a unified status report for fleet operators or the vehicle owner.

Each step depends on output from earlier stages .  diagnostics need telemetry readings, the status report needs both diagnostic results and location data. If the telemetry pull fails due to a cellular connectivity drop, you need to retry without regenerating a stale diagnostic report. Without orchestration, you'd build a monolithic vehicle monitor that mixes CAN bus communication, diagnostic algorithms, GPS polling, and report generation ,  making it impossible to upgrade your diagnostic model, switch telematics providers, or audit which telemetry readings triggered a maintenance alert.

## The Solution

**You just write the vehicle monitoring workers. Telemetry collection, diagnostics analysis, geolocation tracking, and status reporting. Conductor handles telemetry-to-report sequencing, cellular retry logic, and complete records linking telemetry readings to maintenance alerts.**

Each worker handles one IoT operation .  data ingestion, threshold analysis, device command, or alert dispatch. Conductor manages the telemetry pipeline, device state tracking, and alert escalation.

### What You Write: Workers

Four workers monitor connected vehicles: TelemetryWorker collects engine RPM, fuel level, and battery voltage, DiagnosticsWorker assesses vehicle health, GeolocationWorker tracks GPS position, and StatusReportWorker compiles the unified status summary.

| Worker | Task | What It Does |
|---|---|---|
| **DiagnosticsWorker** | `veh_diagnostics` | Analyzes engine RPM, fuel level, and battery voltage to determine overall vehicle health status. |
| **GeolocationWorker** | `veh_geolocation` | Retrieves the vehicle's current GPS location and correlates it with speed data. |
| **StatusReportWorker** | `veh_status_report` | Compiles vehicle health, location, and speed data into a unified status report. |
| **TelemetryWorker** | `veh_telemetry` | Collects real-time vehicle telemetry: engine RPM, fuel level, battery voltage, and speed. |

Workers simulate device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs .  the workflow and alerting logic stay the same.

### The Workflow

```
veh_telemetry
    │
    ▼
veh_diagnostics
    │
    ▼
veh_geolocation
    │
    ▼
veh_status_report

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
java -jar target/connected-vehicles-1.0.0.jar

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
java -jar target/connected-vehicles-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow connected_vehicles_demo \
  --version 1 \
  --input '{"vehicleId": "TEST-001", "vin": "sample-vin"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w connected_vehicles_demo -s COMPLETED -c 5

```

## How to Extend

Connect TelemetryWorker to your vehicle's OBD-II or CAN bus interface, GeolocationWorker to your telematics provider, and DiagnosticsWorker to your fleet health analytics engine. The workflow definition stays exactly the same.

- **TelemetryWorker** (`veh_telemetry`): read real OBD-II data via a connected dongle (Automatic, Mojio, Zubie) or pull CAN bus telemetry from your telematics gateway
- **DiagnosticsWorker** (`veh_diagnostics`): decode real DTC codes, run engine health scoring algorithms, or call a vehicle diagnostics API (Smartcar, Vinli) to assess component health
- **GeolocationWorker** (`veh_geolocation`): query the vehicle's GPS module or telematics platform (Geotab, CalAmp) for real-time position, heading, and speed
- **StatusReportWorker** (`veh_status_report`): write the compiled status report to your fleet management dashboard, send alerts for degraded vehicles, or push data to your connected car mobile app

Integrate each worker with your telematics platform or OBD-II interface while keeping the same return fields, and the monitoring pipeline requires no changes.

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
connected-vehicles/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/connectedvehicles/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ConnectedVehiclesExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DiagnosticsWorker.java
│       ├── GeolocationWorker.java
│       ├── StatusReportWorker.java
│       └── TelemetryWorker.java
└── src/test/java/connectedvehicles/workers/
    ├── DiagnosticsWorkerTest.java        # 2 tests
    └── TelemetryWorkerTest.java        # 2 tests

```
