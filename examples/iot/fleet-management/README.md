# Fleet Management in Java with Conductor :  Vehicle Tracking, Route Optimization, Dispatch, and Trip Monitoring

A Java Conductor workflow example that orchestrates fleet operations .  tracking vehicle GPS positions and fuel levels, optimizing routes with distance and fuel estimates, dispatching drivers with ETA notifications, monitoring trip progress (speed, fuel consumption, duration), and generating delivery reports with cost breakdowns. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Why Fleet Dispatch Needs Orchestration

Running a delivery or logistics fleet means coordinating a pipeline of decisions for every trip. You query GPS trackers to find available vehicles with their locations and fuel levels. You feed those positions into a route optimizer that assigns the best vehicle and driver, calculates estimated distance, duration, and fuel consumption. You dispatch the assignment, notify the driver, and provide an ETA. You monitor the trip in real time .  tracking actual distance, speed, fuel used, and completion time. Finally, you generate a report comparing actuals against estimates and calculating cost.

Each step depends on the previous one .  you cannot optimize routes without knowing which vehicles are available, and you cannot generate a trip report without actual trip data. If the GPS tracker is temporarily unreachable, you need to retry without re-dispatching a vehicle that is already assigned. Without orchestration, you'd build a monolithic dispatch system that mixes telematics polling, routing algorithms, driver notifications, and reporting ,  making it impossible to swap route optimizers, test dispatch logic independently, or trace why a specific delivery was late.

## How This Workflow Solves It

**You just write the fleet operations workers. Vehicle tracking, route optimization, driver dispatch, trip monitoring, and delivery reporting. Conductor handles vehicle-to-report sequencing, GPS tracker retries, and complete dispatch records for delivery performance analysis.**

Each fleet operation is an independent worker .  track vehicles, optimize routes, dispatch, monitor trip, generate report. Conductor sequences them, passes vehicle availability and route assignments between steps, retries if a GPS tracker poll times out, and keeps a complete audit trail of every dispatch decision, route calculation, and trip outcome.

### What You Write: Workers

Five workers coordinate fleet operations: TrackVehiclesWorker queries GPS positions and fuel levels, OptimizeRoutesWorker calculates efficient assignments, DispatchWorker notifies drivers with ETAs, MonitorTripWorker tracks real-time progress, and GenerateReportWorker compares actuals to estimates.

| Worker | Task | What It Does |
|---|---|---|
| **DispatchWorker** | `flt_dispatch` | Assigns the trip to a vehicle and driver, notifies the driver, and provides an ETA. |
| **GenerateReportWorker** | `flt_generate_report` | Compiles a delivery report comparing actual vs: estimated distance, duration, and fuel with cost calculations. |
| **MonitorTripWorker** | `flt_monitor_trip` | Tracks the trip in real time .  actual distance, speed, fuel consumption, and completion status. |
| **OptimizeRoutesWorker** | `flt_optimize_routes` | Assigns the best vehicle and calculates the optimal route with distance, duration, and fuel estimates. |
| **TrackVehiclesWorker** | `flt_track_vehicles` | Queries GPS trackers to find available vehicles with their locations and fuel levels. |

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
flt_track_vehicles
    │
    ▼
flt_optimize_routes
    │
    ▼
flt_dispatch
    │
    ▼
flt_monitor_trip
    │
    ▼
flt_generate_report
```

## Example Output

```
=== Example 535: Fleet Management ===

Step 1: Registering task definitions...
  Registered: flt_track_vehicles, flt_optimize_routes, flt_dispatch, flt_monitor_trip, flt_generate_report

Step 2: Registering workflow 'fleet_management_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [dispatch] Processing
  [report] Processing
  [monitor] Processing
  [route] Processing
  [track] Processing

  Status: COMPLETED
  Output: {dispatchId=..., dispatchedAt=..., etaMinutes=..., driverNotified=...}

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
java -jar target/fleet-management-1.0.0.jar
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
java -jar target/fleet-management-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow fleet_management_workflow \
  --version 1 \
  --input '{"tripId": "TRIP-535-001", "TRIP-535-001": "origin", "origin": "San Francisco, CA", "San Francisco, CA": "destination", "destination": "San Jose, CA", "San Jose, CA": "fleetId", "fleetId": "FLEET-BAY-AREA", "FLEET-BAY-AREA": "sample-FLEET-BAY-AREA"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w fleet_management_workflow -s COMPLETED -c 5
```

## How to Extend

Connect TrackVehiclesWorker to your GPS telematics platform, OptimizeRoutesWorker to your routing engine (Google Routes, HERE), and DispatchWorker to your driver notification system. The workflow definition stays exactly the same.

- **TrackVehiclesWorker** (`flt_track_vehicles`): query your GPS telematics platform (Samsara, Geotab, Fleet Complete) for real-time vehicle positions, fuel levels, and availability status
- **OptimizeRoutesWorker** (`flt_optimize_routes`): call a routing engine (Google Routes API, OSRM, HERE) to find the optimal vehicle-route assignment with distance, duration, and fuel estimates
- **DispatchWorker** (`flt_dispatch`): push dispatch assignments to your fleet management system and send driver notifications via SMS or a mobile fleet app
- **MonitorTripWorker** (`flt_monitor_trip`): poll telematics data during the trip to track actual distance, average speed, fuel consumption, and completion time
- **GenerateReportWorker** (`flt_generate_report`): compile trip actuals vs: estimates, calculate delivery cost, and store the report in your logistics database or BI system

Wire each worker to your telematics or routing platform while maintaining the same return structure, and the dispatch pipeline runs without modification.

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
fleet-management/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/fleetmanagement/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FleetManagementExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DispatchWorker.java
│       ├── GenerateReportWorker.java
│       ├── MonitorTripWorker.java
│       ├── OptimizeRoutesWorker.java
│       └── TrackVehiclesWorker.java
└── src/test/java/fleetmanagement/workers/
    ├── OptimizeRoutesWorkerTest.java        # 2 tests
    └── TrackVehiclesWorkerTest.java        # 2 tests
```
