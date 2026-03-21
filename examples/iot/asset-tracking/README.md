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

| Worker | Task | What It Does |
|---|---|---|
| **GeofenceCheckWorker** | `ast_geofence_check` | Checks whether the asset's GPS coordinates fall inside or outside the defined geofence boundary and computes distance from the boundary. |
| **TagAssetWorker** | `ast_tag_asset` | Registers an IoT tracking tag for the asset and returns the assigned tag ID. |
| **TrackLocationWorker** | `ast_track_location` | Polls the tracking device for the asset's current GPS latitude and longitude. |
| **TriggerAlertWorker** | `ast_trigger_alert` | Evaluates geofence status and triggers an alert if the asset is outside the authorized zone. |
| **UpdateRegistryWorker** | `ast_update_registry` | Updates the central asset registry with current location, geofence status, and alert information. |

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
