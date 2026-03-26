# Supply Chain Iot

Orchestrates supply chain iot through a multi-stage Conductor workflow.

**Input:** `shipmentId`, `origin`, `destination` | **Timeout:** 60s

## Pipeline

```
sci_track_shipment
    │
sci_monitor_conditions
    │
sci_route_condition [SWITCH]
  ├─ ok: sci_handle_ok
  └─ alert: sci_handle_alert
```

## Workers

**HandleAlertWorker** (`sci_handle_alert`)

Outputs `action`, `alertSent`.

**HandleOkWorker** (`sci_handle_ok`)

Outputs `action`, `logged`.

**MonitorConditionsWorker** (`sci_monitor_conditions`)

Outputs `temperature`, `conditionStatus`.

**TrackShipmentWorker** (`sci_track_shipment`)

Outputs `done`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
