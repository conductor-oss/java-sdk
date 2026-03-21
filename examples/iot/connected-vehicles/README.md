# Connected Vehicles in Java with Conductor

## Why Connected Vehicle Monitoring Needs Orchestration

Monitoring a connected vehicle requires collecting data from multiple on-board systems and combining it into an actionable status report. You pull telemetry from the vehicle's OBD-II or CAN bus. engine RPM, fuel level, battery voltage, and speed. You run diagnostics against those readings to assess overall vehicle health (engine performance, electrical system, fuel efficiency). You track the vehicle's geolocation using its GPS module. Finally, you compile health status, location, and speed into a unified status report for fleet operators or the vehicle owner.

Each step depends on output from earlier stages. diagnostics need telemetry readings, the status report needs both diagnostic results and location data. If the telemetry pull fails due to a cellular connectivity drop, you need to retry without regenerating a stale diagnostic report. Without orchestration, you'd build a monolithic vehicle monitor that mixes CAN bus communication, diagnostic algorithms, GPS polling, and report generation, making it impossible to upgrade your diagnostic model, switch telematics providers, or audit which telemetry readings triggered a maintenance alert.

## The Solution

**You just write the vehicle monitoring workers. Telemetry collection, diagnostics analysis, geolocation tracking, and status reporting. Conductor handles telemetry-to-report sequencing, cellular retry logic, and complete records linking telemetry readings to maintenance alerts.**

Each worker handles one IoT operation. data ingestion, threshold analysis, device command, or alert dispatch. Conductor manages the telemetry pipeline, device state tracking, and alert escalation.

### What You Write: Workers

Four workers monitor connected vehicles: TelemetryWorker collects engine RPM, fuel level, and battery voltage, DiagnosticsWorker assesses vehicle health, GeolocationWorker tracks GPS position, and StatusReportWorker compiles the unified status summary.

| Worker | Task | What It Does |
|---|---|---|
| **DiagnosticsWorker** | `veh_diagnostics` | Analyzes engine RPM, fuel level, and battery voltage to determine overall vehicle health status. |
| **GeolocationWorker** | `veh_geolocation` | Retrieves the vehicle's current GPS location and correlates it with speed data. |
| **StatusReportWorker** | `veh_status_report` | Compiles vehicle health, location, and speed data into a unified status report. |
| **TelemetryWorker** | `veh_telemetry` | Collects real-time vehicle telemetry: engine RPM, fuel level, battery voltage, and speed. |

the workflow and alerting logic stay the same.

### The Workflow

```
veh_telemetry
 │
 ▼
veh_diagnostics
 │
 ▼
veh_geolocation
 │
 ▼
veh_status_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
