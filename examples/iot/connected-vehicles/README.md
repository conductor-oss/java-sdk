# Connected Vehicles

Orchestrates connected vehicles through a multi-stage Conductor workflow.

**Input:** `vehicleId`, `vin` | **Timeout:** 60s

## Pipeline

```
veh_telemetry
    │
veh_diagnostics
    │
veh_geolocation
    │
veh_status_report
```

## Workers

**DiagnosticsWorker** (`veh_diagnostics`)

Outputs `overallHealth`, `batteryOk`.

**GeolocationWorker** (`veh_geolocation`)

Outputs `location`, `coordinates`.

**StatusReportWorker** (`veh_status_report`)

Outputs `reportGenerated`.

**TelemetryWorker** (`veh_telemetry`)

Outputs `done`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
