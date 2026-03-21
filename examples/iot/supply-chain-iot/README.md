# Supply Chain IoT in Java with Conductor : Shipment Tracking, Condition Monitoring, and Alert-Based Rerouting

## Why Supply Chain Monitoring Needs Orchestration

Shipping perishable goods or sensitive materials requires continuous monitoring throughout transit. You track the shipment's GPS position to know where it is. You check the in-transit environmental conditions. temperature inside the container, humidity levels, door-open events. Based on those conditions, you take entirely different actions: if everything is within spec, you log the checkpoint and continue. If the temperature exceeds the cold chain threshold, you trigger an alert, notify the logistics team, and initiate rerouting to a closer destination before the cargo is compromised.

This is a classic conditional routing problem. The same shipment check can lead to "continue" or "reroute + alert," and those paths have completely different downstream effects. Without orchestration, you'd build a monolithic shipment monitor that mixes GPS polling, sensor reading, threshold checking, notification dispatch, and rerouting logic. making it impossible to add new condition types (shock, tilt, light exposure) without rewriting the core monitoring code.

## How This Workflow Solves It

**You just write the supply chain monitoring workers. Shipment tracking, condition monitoring, and conditional routing to OK or alert handlers. Conductor handles SWITCH-based condition routing, GPS tracker retries, and checkpoint records for cold chain compliance documentation.**

Each supply chain concern is an independent worker. track shipment, monitor conditions, handle normal status, handle alerts. Conductor sequences tracking and condition monitoring, then uses a SWITCH task to route to the correct handler based on condition status. If a GPS tracker poll times out, Conductor retries. Adding new condition types means adding a new SWITCH case and worker, no changes to existing shipment tracking or alert logic.

### What You Write: Workers

Four workers monitor shipments in transit: TrackShipmentWorker tracks GPS position, MonitorConditionsWorker reads temperature and humidity from IoT sensors, HandleOkWorker logs compliant checkpoints, and HandleAlertWorker triggers rerouting when conditions breach thresholds.

| Worker | Task | What It Does |
|---|---|---|
| **HandleAlertWorker** | `sci_handle_alert` | Triggers alerts, notifies the logistics team, and initiates rerouting when conditions breach thresholds. |
| **HandleOkWorker** | `sci_handle_ok` | Logs a successful checkpoint when all in-transit conditions are within acceptable limits. |
| **MonitorConditionsWorker** | `sci_monitor_conditions` | Reads in-transit environmental conditions (temperature, humidity) from shipment IoT sensors. |
| **TrackShipmentWorker** | `sci_track_shipment` | Tracks the shipment's current GPS location between origin and destination. |

the workflow and alerting logic stay the same.

### The Workflow

```
sci_track_shipment
 │
 ▼
sci_monitor_conditions
 │
 ▼
SWITCH (sci_switch_ref)
 ├── ok: sci_handle_ok
 ├── alert: sci_handle_alert

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
