# Geofencing in Java with Conductor : Location Tracking, Boundary Evaluation, and Zone-Based Alerts

## Why Geofence Monitoring Needs Orchestration

Geofencing requires a decision pipeline for every location update. You receive raw GPS coordinates from a device, normalize them, and compute the Euclidean distance to the geofence center. Based on whether the device is inside or outside the defined radius, you route to entirely different alert handlers. an entry alert when a device enters a restricted zone, an exit alert when it leaves a monitored area. The alert type, the notification recipients, and the follow-up actions all depend on the zone status.

This is a natural fit for conditional routing. Without orchestration, you'd write a location processor that mixes coordinate math, boundary logic, and alert dispatch in one class, using if/else chains to decide which notification to send. When you need to add a new zone type (approaching, dwelling, speeding-within-zone), you'd have to modify the core processor. Conductor's SWITCH task handles the routing declaratively. add a new case in the workflow JSON, write a new worker, and the existing code is untouched.

## How This Workflow Solves It

**You just write the geofencing workers. Location normalization, boundary evaluation, and zone-specific alert handlers. Conductor handles declarative SWITCH-based zone routing, GPS poll retries, and location event records for geofence analytics.**

Each geofencing concern is an independent worker. check location, evaluate boundaries, alert on entry, alert on exit. Conductor sequences location normalization and boundary evaluation, then uses a SWITCH task to route to the correct alert handler based on zone status. If a GPS poll times out, Conductor retries. If you add new zone states, you add a new SWITCH case and a new worker, no existing code changes.

### What You Write: Workers

Four workers process each location update: CheckLocationWorker normalizes GPS coordinates, EvaluateBoundariesWorker computes distance to the fence perimeter, AlertInsideWorker handles zone entry events, and AlertOutsideWorker handles zone exit events via SWITCH routing.

| Worker | Task | What It Does |
|---|---|---|
| **AlertInsideWorker** | `geo_alert_inside` | Handles alert when device is inside the geofence zone. |
| **AlertOutsideWorker** | `geo_alert_outside` | Handles alert when device is outside the geofence zone. |
| **CheckLocationWorker** | `geo_check_location` | Checks device location and normalizes coordinates. |
| **EvaluateBoundariesWorker** | `geo_evaluate_boundaries` | Evaluates geofence boundaries against device position. |

the workflow and alerting logic stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
