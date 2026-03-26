# Worker Pools

A task processing system needs to manage multiple worker pools for different task types. CPU-intensive tasks go to one pool; I/O-bound tasks go to another. Each pool has configurable concurrency, health monitoring, and independent scaling.

## Pipeline

```
[wpl_categorize_task]
     |
     v
[wpl_assign_pool]
     |
     v
[wpl_execute_task]
     |
     v
[wpl_return_to_pool]
```

**Workflow inputs:** `taskPayload`, `taskCategory`

## Workers

**WplAssignPoolWorker** (task: `wpl_assign_pool`)

- Uses randomization
- Writes `assignedPool`, `workerId`, `poolSize`

**WplCategorizeTaskWorker** (task: `wpl_categorize_task`)

- Writes `category`, `resourceProfile`

**WplExecuteTaskWorker** (task: `wpl_execute_task`)

- Sets `result` = `"task_completed_successfully"`
- Writes `result`, `durationMs`

**WplReturnToPoolWorker** (task: `wpl_return_to_pool`)

- Writes `returned`, `poolAvailability`

---

**16 tests** | Workflow: `wpl_worker_pools` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
