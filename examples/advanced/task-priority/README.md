# Task Priority

A shared worker pool handles tasks from multiple workflow types. Production-critical tasks must execute before batch analytics tasks, even if the analytics tasks were queued first. The priority system assigns numeric priorities, sorts the task queue, and always dispatches the highest-priority task next.

## Pipeline

```
[tpr_classify_priority]
     |
     v
     <SWITCH>
       |-- high -> [tpr_route_high]
       |-- medium -> [tpr_route_medium]
       |-- low -> [tpr_route_low]
       +-- default -> [tpr_route_medium]
```

**Workflow inputs:** `taskId`, `urgency`, `impact`

## Workers

**TprClassifyPriorityWorker** (task: `tpr_classify_priority`)

- Writes `priority`, `slaMinutes`

**TprRouteHighWorker** (task: `tpr_route_high`)

- Writes `queue`, `acknowledged`, `slaMinutes`

**TprRouteLowWorker** (task: `tpr_route_low`)

- Writes `queue`, `acknowledged`, `slaMinutes`

**TprRouteMediumWorker** (task: `tpr_route_medium`)

- Writes `queue`, `acknowledged`, `slaMinutes`

---

**16 tests** | Workflow: `tpr_task_priority` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
