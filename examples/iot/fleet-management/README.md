# Fleet Management

Orchestrates fleet management through a multi-stage Conductor workflow.

**Input:** `tripId`, `origin`, `destination`, `fleetId` | **Timeout:** 60s

## Pipeline

```
flt_track_vehicles
    │
flt_optimize_routes
    │
flt_dispatch
    │
flt_monitor_trip
    │
flt_generate_report
```

## Workers

**DispatchWorker** (`flt_dispatch`)

Reads `dispatchId`. Outputs `dispatchId`, `dispatchedAt`, `etaMinutes`, `driverNotified`.

**GenerateReportWorker** (`flt_generate_report`)

Reads `onTimeDelivery`. Outputs `onTimeDelivery`, `costEstimate`, `reportId`.

**MonitorTripWorker** (`flt_monitor_trip`)

Reads `tripStatus`. Outputs `tripStatus`, `actualDistance`, `actualDuration`, `fuelUsed`, `avgSpeed`.

**OptimizeRoutesWorker** (`flt_optimize_routes`)

Reads `assignedVehicleId`. Outputs `assignedVehicleId`, `driverId`, `routeId`, `estimatedDistance`, `estimatedDuration`.

**TrackVehiclesWorker** (`flt_track_vehicles`)

Reads `availableVehicles`. Outputs `availableVehicles`, `id`, `lat`, `lng`, `fuelLevel`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
