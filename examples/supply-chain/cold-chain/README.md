# Cold Chain Monitoring in Java with Conductor : Temperature Sensing, Threshold Checks, Alert Routing, and Corrective Action

## The Problem

You need to continuously monitor temperature conditions for cold chain shipments. Frozen pharmaceuticals, vaccines, and biologics must stay within a narrow temperature range (e.g., 2-8C) throughout transit. When a sensor reading breaches the threshold, the logistics team needs an immediate alert, and corrective action must be triggered. rerouting to a backup refrigerated warehouse, dispatching a replacement shipment, or flagging the batch for quality review. Regulatory requirements (FDA 21 CFR Part 211, EU GDP) demand a documented chain of custody proving temperatures were maintained.

Without orchestration, you'd poll IoT sensors in a cron job, compare readings in a script, and send alerts via a separate notification service. If the alert service is down when a temperature excursion happens, the breach goes unnoticed and a $500K pharmaceutical shipment is ruined. There is no unified record of when the excursion started, when the alert was sent, or what corrective action was taken.

## The Solution

**You just write the cold chain workers. Sensor reads, threshold checks, alert dispatch, and corrective action triggers. Conductor handles sequencing, conditional routing, retries, and timestamped audit trails for FDA/GDP regulatory compliance.**

Each step in the cold chain monitoring pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so sensor readings are checked against thresholds, then routes to either the OK handler (logging compliance) or the alert handler (triggering corrective action) based on the result. If the alert worker fails, Conductor retries it until the logistics team is notified. Every temperature reading, threshold comparison, and corrective action is recorded with timestamps for regulatory audit trails.

### What You Write: Workers

Five workers cover the cold chain pipeline: MonitorTempWorker reads sensor data, CheckThresholdsWorker evaluates compliance, HandleOkWorker logs normal checkpoints, HandleAlertWorker dispatches excursion notifications, and ActWorker triggers corrective actions.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `cch_act` | Takes corrective action based on the temperature status. rerouting, replacement, or quality review. |
| **CheckThresholdsWorker** | `cch_check_thresholds` | Compares the current temperature against min/max thresholds and returns compliance status. |
| **HandleAlertWorker** | `cch_handle_alert` | Triggers an alert and notifies the logistics team when a temperature excursion is detected. |
| **HandleOkWorker** | `cch_handle_ok` | Logs a compliant checkpoint when temperature is within the acceptable range. |
| **MonitorTempWorker** | `cch_monitor_temp` | Reads the current temperature from the shipment's IoT sensor. |

### The Workflow

```
cch_monitor_temp
 │
 ▼
cch_check_thresholds
 │
 ▼
SWITCH (cch_switch_ref)
 ├── ok: cch_handle_ok
 └── default: cch_handle_alert
 │
 ▼
cch_act

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
