# Industrial IoT in Java with Conductor : Machine Telemetry, Predictive Failure Analysis, and Repair Scheduling

## Why Predictive Maintenance Pipelines Need Orchestration

Industrial machines generate a constant stream of telemetry. bearing temperature at 185F, vibration at 4.2 mm/s, oil level at 72%. Turning that raw data into actionable maintenance decisions requires a pipeline. You collect telemetry from the machine's sensors. You feed those readings into a predictive model that estimates failure probability, identifies the likely component (bearing assembly, pump seal, motor winding), and predicts remaining useful life. If the failure probability exceeds a threshold, you schedule a repair work order with the right parts kit, a target date before predicted failure, and an estimated downtime window.

Each step depends on the previous one. the predictive model needs current telemetry, and the repair scheduler needs the model's output to know which parts to order and when to schedule downtime. If the telemetry pull from the PLC fails, you need to retry without sending stale predictions to the scheduler. Without orchestration, you'd build a monolithic condition monitoring system that mixes OPC-UA data collection, ML inference, and CMMS integration, making it impossible to swap predictive models, test scheduling logic independently, or audit which telemetry readings triggered which work orders.

## How This Workflow Solves It

**You just write the industrial IoT workers. Machine telemetry collection, predictive failure analysis, and repair work order scheduling. Conductor handles telemetry-to-work-order sequencing, PLC connection retries, and complete records linking every work order to the readings that triggered it.**

Each stage of the predictive maintenance pipeline is an independent worker. monitor machines, run predictive analysis, schedule repairs. Conductor sequences them, passes telemetry data to the prediction model and prediction results to the repair scheduler, retries if a PLC connection times out, and keeps a complete audit trail linking every work order back to the specific telemetry readings that triggered it.

### What You Write: Workers

Three workers form the predictive maintenance pipeline: MonitorMachinesWorker collects temperature, vibration, and pressure telemetry, PredictiveAnalysisWorker estimates failure probability and identifies at-risk components, and ScheduleRepairWorker creates work orders with parts lists and downtime windows.

| Worker | Task | What It Does |
|---|---|---|
| **MonitorMachinesWorker** | `iit_monitor_machines` | Collects machine telemetry (temperature, vibration, pressure, RPM, oil level, run hours) and computes an anomaly score. |
| **PredictiveAnalysisWorker** | `iit_predictive_analysis` | Runs predictive failure analysis to estimate failure probability, identify the at-risk component, and assess urgency. |
| **ScheduleRepairWorker** | `iit_schedule_repair` | Creates a repair work order with parts list, target date, and estimated downtime based on prediction results. |

the workflow and alerting logic stay the same.

### The Workflow

```
iit_monitor_machines
 │
 ▼
iit_predictive_analysis
 │
 ▼
iit_schedule_repair

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
