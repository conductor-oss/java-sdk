# Logistics Optimization in Java with Conductor : Demand Analysis, Route Optimization, Vehicle Scheduling, and Fleet Dispatch

## The Problem

You need to optimize logistics for a batch of 40+ orders spread across different ZIP codes. Demand must be analyzed to identify geographic clusters and volume patterns. Routes must be optimized across all delivery points to minimize total mileage. Vehicles must be scheduled based on load capacity, driver hours-of-service, and delivery time windows. The fleet must then be dispatched with each driver receiving their optimized stop list.

Without orchestration, the logistics planner manually groups orders by region, estimates routes on a map, and assigns trucks by intuition. If the route optimizer finds a better solution after vehicles have been scheduled, there is no easy way to propagate the change. When demand spikes unexpectedly, re-running the entire pipeline wastes the demand analysis already computed.

## The Solution

**You just write the logistics workers. Demand analysis, route computation, vehicle scheduling, and fleet dispatch. Conductor handles data flow between stages, optimizer retries on timeout, and recorded route solutions for continuous improvement.**

Each phase of the logistics optimization pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so demand analysis feeds route optimization, optimized routes drive vehicle scheduling, and scheduling results determine dispatch assignments. If the route optimizer times out on a large order set, Conductor retries without re-analyzing demand. Every demand cluster, route solution, schedule assignment, and dispatch confirmation is recorded for cost analysis and continuous improvement.

### What You Write: Workers

Four workers optimize the logistics pipeline: AnalyzeDemandWorker clusters orders by geography, OptimizeRoutesWorker minimizes total mileage, ScheduleWorker assigns vehicles by capacity and hours, and DispatchWorker sends drivers their stop lists.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeDemandWorker** | `lo_analyze_demand` | Analyzes order demand to identify geographic clusters and volume patterns by ZIP code. |
| **DispatchWorker** | `lo_dispatch` | Dispatches the fleet with each driver receiving their optimized stop list. |
| **OptimizeRoutesWorker** | `lo_optimize_routes` | Computes optimal delivery routes across all delivery points to minimize total mileage. |
| **ScheduleWorker** | `lo_schedule` | Schedules vehicles based on load capacity, driver hours-of-service, and time windows. |

### The Workflow

```
lo_analyze_demand
 │
 ▼
lo_optimize_routes
 │
 ▼
lo_schedule
 │
 ▼
lo_dispatch

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
