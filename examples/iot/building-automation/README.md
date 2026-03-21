# Building Automation in Java with Conductor

## Why Building Automation Needs Orchestration

Optimizing a building's HVAC and lighting systems requires a pipeline where each step depends on the previous one. You monitor building systems to get current HVAC status, lighting levels, and occupancy counts for a specific floor. You feed that data into an optimizer that identifies energy savings opportunities. reducing HVAC output in unoccupied zones, dimming lights in daylit areas. You schedule those optimizations into a timed execution plan for the building management system. Finally, you apply the adjustments to the actual HVAC and lighting controllers.

Each step depends on the previous one. the optimizer needs current system states, the scheduler needs optimization recommendations, and the adjuster needs the schedule. If the BMS monitoring poll fails, you do not want stale occupancy data driving optimization decisions. Without orchestration, you'd build a monolithic building controller that mixes sensor polling, optimization algorithms, scheduling logic, and actuator control, making it impossible to swap optimization strategies, test scheduling independently, or track which sensor readings led to which energy savings.

## The Solution

**You just write the building automation workers. System monitoring, energy optimization, schedule creation, and controller adjustment. Conductor handles sensor-to-actuator ordering, BMS polling retries, and recorded optimization decisions for energy savings verification.**

Each worker handles one IoT operation. data ingestion, threshold analysis, device command, or alert dispatch. Conductor manages the telemetry pipeline, device state tracking, and alert escalation.

### What You Write: Workers

Four workers optimize building energy: MonitorSystemsWorker reads HVAC, lighting, and occupancy data, OptimizeWorker identifies savings opportunities, ScheduleWorker creates timed execution plans, and AdjustWorker applies changes to building controllers.

| Worker | Task | What It Does |
|---|---|---|
| **AdjustWorker** | `bld_adjust` | Applies scheduled optimizations to HVAC and lighting controllers on the target floor. |
| **MonitorSystemsWorker** | `bld_monitor_systems` | Monitors HVAC status, lighting levels, and occupancy for a building floor. |
| **OptimizeWorker** | `bld_optimize` | Analyzes HVAC, lighting, and occupancy data to generate energy optimization recommendations and projected savings. |
| **ScheduleWorker** | `bld_schedule` | Schedules optimization recommendations into a timed execution plan for the building management system. |

the workflow and alerting logic stay the same.

### The Workflow

```
bld_monitor_systems
 │
 ▼
bld_optimize
 │
 ▼
bld_schedule
 │
 ▼
bld_adjust

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
