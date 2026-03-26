# Task Routing

A multi-tenant system processes tasks that require tenant-specific infrastructure. Tenant A's tasks must run on GPU workers; Tenant B's tasks run on CPU workers; Tenant C's tasks require on-premise execution. The routing layer inspects task metadata and directs each task to the appropriate worker pool.

## Pipeline

```
[trt_analyze_requirements]
     |
     v
[trt_select_pool]
     |
     v
[trt_dispatch]
     |
     v
[trt_verify]
```

**Workflow inputs:** `taskType`, `resourceNeeds`, `region`

## Workers

**TrtAnalyzeRequirementsWorker** (task: `trt_analyze_requirements`)

- Writes `requirements`, `availablePools`

**TrtDispatchWorker** (task: `trt_dispatch`)

- Records wall-clock milliseconds
- Writes `dispatchId`, `pool`, `dispatched`

**TrtSelectPoolWorker** (task: `trt_select_pool`)

- Writes `selectedPool`, `poolLoad`

**TrtVerifyWorker** (task: `trt_verify`)

- Writes `verified`, `executionConfirmed`

---

**16 tests** | Workflow: `trt_task_routing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
