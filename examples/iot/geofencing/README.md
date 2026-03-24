# Geofencing

Geofencing workflow that checks device location, evaluates geofence boundaries, and routes alerts via SWITCH based on zone status.

**Input:** `deviceId`, `latitude`, `longitude` | **Timeout:** 60s

## Pipeline

```
geo_check_location
    │
geo_evaluate_boundaries
    │
geo_route_decision [SWITCH]
  ├─ inside: geo_alert_inside
  └─ outside: geo_alert_outside
```

## Workers

**AlertInsideWorker** (`geo_alert_inside`): Handles alert when device is inside the geofence zone.

Reads `deviceId`, `zone`. Outputs `alertType`, `acknowledged`.

**AlertOutsideWorker** (`geo_alert_outside`): Handles alert when device is outside the geofence zone.

Reads `deviceId`, `distance`, `zone`. Outputs `alertType`, `acknowledged`.

**CheckLocationWorker** (`geo_check_location`): Checks device location and normalizes coordinates.

Reads `deviceId`, `latitude`, `longitude`. Outputs `latitude`, `longitude`, `timestamp`.

**EvaluateBoundariesWorker** (`geo_evaluate_boundaries`): Evaluates geofence boundaries against device position.

- `FENCE_LAT` = `37.78`
- `FENCE_LON` = `-122.42`
- `RADIUS` = `0.01`

```java
double dist = Math.sqrt(Math.pow(lat - FENCE_LAT, 2) + Math.pow(lon - FENCE_LON, 2));
boolean inside = dist <= RADIUS;
```

Reads `latitude`, `longitude`. Outputs `zoneStatus`, `zoneName`, `distanceFromBoundary`.

## Tests

**32 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
