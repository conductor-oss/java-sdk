# Asset Tracking in Java with Conductor

A shipping container carrying $180,000 in electronics left the port of Long Beach 6 hours ago, and the last GPS ping was at 10:14 AM. It's now 4 PM. Is the tracker's battery dead? Did the container pass through a cellular dead zone on I-15? Or did someone peel off the GPS unit and divert the truck? You can't tell, because your tracking system only records pings when they arrive. it has no concept of a ping that should have arrived but didn't. By the time someone manually notices the gap, the container could be in another state. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate a complete asset tracking pipeline, tagging, location polling, geofence checking, alert triggering, and registry updates, as independent workers.

## Why Asset Tracking Needs Orchestration

Tracking high-value assets across a facility or supply chain requires a pipeline where each step builds on the previous one. You tag the asset with a tracking device to get a unique tag ID. You poll the tracker for current GPS coordinates. You check those coordinates against a geofence boundary to determine whether the asset is inside the permitted zone and how far it is from the boundary. Based on the geofence result, you trigger an alert if the asset has left the authorized area. Finally, you update the central asset registry with the latest position, geofence status, and any alert information.

Each step depends on output from the previous one. Location tracking needs the tag ID, geofence checking needs coordinates, alerting needs the geofence result. If a GPS poll times out, you need to retry without re-tagging the asset. Without orchestration, you'd build a monolithic tracking system that mixes device communication, geospatial calculations, alert dispatch, and registry updates, making it impossible to swap GPS providers, change geofence shapes, or audit which location update triggered which alert.

## The Solution

**You just write the asset tracking workers. Tag registration, GPS polling, geofence evaluation, alert triggering, and registry updates. Conductor handles tag-to-alert sequencing, GPS retry logic, and a complete location history for every tracked asset.**

Each worker handles one IoT operation. Data ingestion, threshold analysis, device command, or alert dispatch. Conductor manages the telemetry pipeline, device state tracking, and alert escalation.

### What You Write: Workers

Five workers form the tracking pipeline: TagAssetWorker registers IoT identifiers, TrackLocationWorker polls GPS coordinates, GeofenceCheckWorker evaluates boundary compliance, TriggerAlertWorker dispatches violations, and UpdateRegistryWorker records current position and status.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **GeofenceCheckWorker** | `ast_geofence_check` | Checks whether the asset's GPS coordinates fall inside or outside the defined geofence boundary and computes distance from the boundary. | Simulated |
| **TagAssetWorker** | `ast_tag_asset` | Registers an IoT tracking tag for the asset and returns the assigned tag ID. | Simulated |
| **TrackLocationWorker** | `ast_track_location` | Polls the tracking device for the asset's current GPS latitude and longitude. | Simulated |
| **TriggerAlertWorker** | `ast_trigger_alert` | Evaluates geofence status and triggers an alert if the asset is outside the authorized zone. | Simulated |
| **UpdateRegistryWorker** | `ast_update_registry` | Updates the central asset registry with current location, geofence status, and alert information. | Simulated |

Workers simulate device telemetry and control operations with realistic sensor data. Replace with real MQTT/CoAP clients and device APIs, the workflow and alerting logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ast_tag_asset
    │
    ▼
ast_track_location
    │
    ▼
ast_geofence_check
    │
    ▼
ast_trigger_alert
    │
    ▼
ast_update_registry
```

## Example Output

```
=== Example 540: Asset Tracking ===

Step 1: Registering task definitions...
  Registered: ast_tag_asset, ast_track_location, ast_geofence_check, ast_trigger_alert, ast_update_registry

Step 2: Registering workflow 'asset_tracking_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f12f7f7c-68c4-b214-3a2d-80d2071da395

  [tag] Processing 2026-03-16
  [track] Processing 2026-03-16
  [geofence] Processing 2026-03-16
  [alert] Processing 2026-03-16
  [update] Processing 2026-03-16


  Status: COMPLETED
  Output: {assetId=ASSET-540-PALLET-001, insideGeofence=true, alertTriggered=true, registryUpdated=true}

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
java -jar target/asset-tracking-1.0.0.jar
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
java -jar target/asset-tracking-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow asset_tracking_workflow \
  --version 1 \
  --input '{"assetId": "ASSET-540-PALLET-001", "assetType": "shipping_pallet", "geofenceId": "GF-WAREHOUSE-A"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w asset_tracking_workflow -s COMPLETED -c 5
```

## How to Extend

Connect TagAssetWorker to your IoT device registry, TrackLocationWorker to your GPS tracking platform, and TriggerAlertWorker to your operations notification system. The workflow definition stays exactly the same.

- **TagAssetWorker** (`ast_tag_asset`): register the asset with your IoT tracking platform (Tile, Apple AirTag network, custom BLE tags) or asset management system to get a tag ID
- **TrackLocationWorker** (`ast_track_location`): query real GPS positions from your tracking hardware (CalAmp, Queclink, Sierra Wireless) or a fleet tracking API (Samsara, GPS Trackit)
- **GeofenceCheckWorker** (`ast_geofence_check`): perform real geospatial boundary checks using PostGIS, JTS Topology Suite, or Google Maps Geofencing API with polygon or radius-based fences
- **TriggerAlertWorker** (`ast_trigger_alert`): send real alerts via SMS, email, or push notifications to security or logistics teams when assets leave authorized zones
- **UpdateRegistryWorker** (`ast_update_registry`): write location and status updates to your asset management database, ERP system, or CMDB

Swap each worker for a production GPS provider or alert system while keeping the same output fields, and the tracking workflow needs no changes.

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
asset-tracking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/assettracking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AssetTrackingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GeofenceCheckWorker.java
│       ├── TagAssetWorker.java
│       ├── TrackLocationWorker.java
│       ├── TriggerAlertWorker.java
│       └── UpdateRegistryWorker.java
└── src/test/java/assettracking/workers/
    ├── TagAssetWorkerTest.java        # 2 tests
    └── TrackLocationWorkerTest.java        # 2 tests
```
