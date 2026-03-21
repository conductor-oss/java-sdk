# Geofencing in Java with Conductor :  Location Tracking, Boundary Evaluation, and Zone-Based Alerts

A Java Conductor workflow example that orchestrates geofence monitoring. normalizing device GPS coordinates, computing distance from geofence boundaries, determining inside/outside zone status, and routing to different alert handlers based on whether the device has entered or exited the fence. Uses [Conductor](https://github.

## Why Geofence Monitoring Needs Orchestration

Geofencing requires a decision pipeline for every location update. You receive raw GPS coordinates from a device, normalize them, and compute the Euclidean distance to the geofence center. Based on whether the device is inside or outside the defined radius, you route to entirely different alert handlers. an entry alert when a device enters a restricted zone, an exit alert when it leaves a monitored area. The alert type, the notification recipients, and the follow-up actions all depend on the zone status.

This is a natural fit for conditional routing. Without orchestration, you'd write a location processor that mixes coordinate math, boundary logic, and alert dispatch in one class, using if/else chains to decide which notification to send. When you need to add a new zone type (approaching, dwelling, speeding-within-zone), you'd have to modify the core processor. Conductor's SWITCH task handles the routing declaratively. add a new case in the workflow JSON, write a new worker, and the existing code is untouched.

## How This Workflow Solves It

**You just write the geofencing workers. Location normalization, boundary evaluation, and zone-specific alert handlers. Conductor handles declarative SWITCH-based zone routing, GPS poll retries, and location event records for geofence analytics.**

Each geofencing concern is an independent worker. check location, evaluate boundaries, alert on entry, alert on exit. Conductor sequences location normalization and boundary evaluation, then uses a SWITCH task to route to the correct alert handler based on zone status. If a GPS poll times out, Conductor retries. If you add new zone states, you add a new SWITCH case and a new worker,  no existing code changes.

### What You Write: Workers

Four workers process each location update: CheckLocationWorker normalizes GPS coordinates, EvaluateBoundariesWorker computes distance to the fence perimeter, AlertInsideWorker handles zone entry events, and AlertOutsideWorker handles zone exit events via SWITCH routing.

| Worker | Task | What It Does |
|---|---|---|
| **AlertInsideWorker** | `geo_alert_inside` | Handles alert when device is inside the geofence zone. |
| **AlertOutsideWorker** | `geo_alert_outside` | Handles alert when device is outside the geofence zone. |
| **CheckLocationWorker** | `geo_check_location` | Checks device location and normalizes coordinates. |
| **EvaluateBoundariesWorker** | `geo_evaluate_boundaries` | Evaluates geofence boundaries against device position. |

Workers implement device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs. the workflow and alerting logic stay the same.

### The Workflow

```
geo_check_location
    │
    ▼
geo_evaluate_boundaries
    │
    ▼
SWITCH (geo_switch_ref)
    ├── inside: geo_alert_inside
    ├── outside: geo_alert_outside

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
java -jar target/geofencing-1.0.0.jar

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
java -jar target/geofencing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow geofencing_demo \
  --version 1 \
  --input '{"deviceId": "TEST-001", "latitude": "sample-latitude", "longitude": "sample-longitude"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w geofencing_demo -s COMPLETED -c 5

```

## How to Extend

Connect CheckLocationWorker to your GPS tracking platform, EvaluateBoundariesWorker to your geospatial service, and the alert workers to your notification and dispatch systems. The workflow definition stays exactly the same.

- **CheckLocationWorker** (`geo_check_location`): query real GPS positions from your device tracker (MQTT topic, device shadow, or telematics API) and normalize coordinates to WGS84
- **EvaluateBoundariesWorker** (`geo_evaluate_boundaries`): replace the simple Euclidean distance check with proper Haversine formula calculations, polygon geofences (JTS Topology Suite), or a spatial database query (PostGIS)
- **AlertInsideWorker** (`geo_alert_inside`): trigger real entry alerts: notify security when a device enters a restricted area, log warehouse arrival times, or activate indoor positioning
- **AlertOutsideWorker** (`geo_alert_outside`): trigger real exit alerts: notify dispatch when a vehicle leaves a depot, alert asset tracking when equipment leaves a job site, or flag compliance violations

Plug each worker into your GPS platform or notification system while keeping the same output fields, and the SWITCH routing continues unchanged.

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
geofencing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/geofencing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GeofencingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AlertInsideWorker.java
│       ├── AlertOutsideWorker.java
│       ├── CheckLocationWorker.java
│       └── EvaluateBoundariesWorker.java
└── src/test/java/geofencing/workers/
    ├── AlertInsideWorkerTest.java        # 8 tests
    ├── AlertOutsideWorkerTest.java        # 8 tests
    ├── CheckLocationWorkerTest.java        # 8 tests
    └── EvaluateBoundariesWorkerTest.java        # 8 tests

```
