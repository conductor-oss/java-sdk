# Graceful Shutdown

A long-running worker process receives a SIGTERM during a deployment. It is currently mid-task on a 30-second computation. The graceful shutdown pipeline needs to detect the shutdown signal, finish in-progress work, checkpoint partial state, deregister from the task queue, and exit cleanly without losing work.

## Pipeline

```
[gsh_signal]
     |
     v
[gsh_drain_tasks]
     |
     v
[gsh_complete_inflight]
     |
     v
[gsh_checkpoint]
     |
     v
[gsh_stop]
```

**Workflow inputs:** `workerGroup`, `drainTimeoutSec`

## Workers

**GshCheckpointWorker** (task: `gsh_checkpoint`)

- Records wall-clock milliseconds
- Writes `checkpointId`

**GshCompleteInflightWorker** (task: `gsh_complete_inflight`)

- Reads `inFlightTasks`. Writes `completedCount`, `completedTasks`

**GshDrainTasksWorker** (task: `gsh_drain_tasks`)

- Writes `drainedCount`, `inFlightTasks`

**GshSignalWorker** (task: `gsh_signal`)

- Captures `instant.now()` timestamps
- Writes `signalSent`, `timestamp`

**GshStopWorker** (task: `gsh_stop`)

- Writes `stopped`, `cleanShutdown`

---

**20 tests** | Workflow: `gsh_graceful_shutdown` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
