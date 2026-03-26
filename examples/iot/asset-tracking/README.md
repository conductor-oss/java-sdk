# Asset Tracking

Orchestrates asset tracking through a multi-stage Conductor workflow.

**Input:** `assetId`, `assetType`, `geofenceId` | **Timeout:** 60s

## Pipeline

```
ast_tag_asset
    │
ast_track_location
    │
ast_geofence_check
    │
ast_trigger_alert
    │
ast_update_registry
```

## Workers

**GeofenceCheckWorker** (`ast_geofence_check`): Checks if asset is within geofence. Real point-in-polygon using ray casting algorithm.

```java
minDistFromBoundary = Math.round(minDistFromBoundary * 100.0) / 100.0;
lat < (polygon[j][0] - polygon[i][0]) * (lng - polygon[i][1])
```

Reads `geofenceName`, `latitude`, `longitude`. Outputs `insideGeofence`, `distanceFromBoundary`, `geofenceName`, `checkedAt`.

**TagAssetWorker** (`ast_tag_asset`): Tags an asset with a tracking device. Real tag ID generation and battery check.

```java
int batteryLevel = 90 + (Math.abs(tagId.hashCode()) % 11);
```

Reads `assetId`, `tagType`. Outputs `tagId`, `tagType`, `batteryLevel`, `activatedAt`.

**TrackLocationWorker** (`ast_track_location`): Tracks asset location. Real GPS coordinate processing.

```java
boolean validCoords = lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
double accuracy = validCoords ? 3.0 + Math.random() * 7.0 : 999.0;
```

Reads `latitude`, `longitude`. Outputs `latitude`, `longitude`, `accuracy`, `speed`, `heading`.

**TriggerAlertWorker** (`ast_trigger_alert`): Triggers alerts based on geofence status. Real alert routing logic.

- `distance < 0.1` &rarr; `"geofence_boundary_warning"`

```java
boolean inside = Boolean.TRUE.equals(insideObj);
```

Reads `distanceFromBoundary`, `insideGeofence`. Outputs `alertType`, `notifications`, `alertId`.

**UpdateRegistryWorker** (`ast_update_registry`): Updates the asset registry with latest location and status.

```java
String status = Boolean.TRUE.equals(insideObj) ? "in_zone" : "out_of_zone";
```

Reads `insideGeofence`, `latitude`, `longitude`. Outputs `updated`, `lastKnownLocation`, `status`, `updatedAt`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
