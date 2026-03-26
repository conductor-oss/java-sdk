# Worker Scaling

A workflow engine's task queue depth spikes during business hours and drops to near-zero overnight. The scaling pipeline monitors queue depth, computes the required worker count, scales workers up when the queue grows, and scales down during quiet periods to save resources.

## Pipeline

```
[wks_monitor_queue]
     |
     v
[wks_calculate_needed]
     |
     v
[wks_scale_workers]
     |
     v
[wks_verify_scaling]
```

**Workflow inputs:** `queueName`, `currentWorkers`, `targetLatencyMs`

## Workers

**WksCalculateNeededWorker** (task: `wks_calculate_needed`)

- Applies `math.ceil()`, formats output strings
- Reads `queueDepth`, `avgProcessingMs`, `targetLatencyMs`. Writes `desiredWorkers`, `scaleFactor`

**WksMonitorQueueWorker** (task: `wks_monitor_queue`)

- Writes `queueDepth`, `avgProcessingMs`, `inFlightTasks`

**WksScaleWorkersWorker** (task: `wks_scale_workers`)

- Reads `currentWorkers`, `desiredWorkers`. Writes `scalingAction`, `newWorkerCount`, `previousCount`

**WksVerifyScalingWorker** (task: `wks_verify_scaling`)

- Writes `verified`, `healthCheck`

---

**16 tests** | Workflow: `wks_worker_scaling` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
