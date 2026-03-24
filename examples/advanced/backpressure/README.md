# Backpressure

A data ingestion pipeline receives events faster than the downstream processor can handle. Without backpressure, the buffer grows without bound until the system runs out of memory. The pipeline needs to monitor queue depth, signal producers to slow down when the buffer exceeds a threshold, and resume normal throughput when the queue drains.

## Pipeline

```
[bkp_monitor_queue]
     |
     v
     <SWITCH>
       |-- ok -> [bkp_handle_ok]
       |-- high -> [bkp_throttle]
       |-- critical -> [bkp_shed_load]
       +-- default -> [bkp_handle_ok]
```

**Workflow inputs:** `queueName`, `thresholdHigh`, `thresholdCritical`

## Workers

**BkpHandleOkWorker** (task: `bkp_handle_ok`)

- Sets `action` = `"proceed_normally"`
- Writes `action`, `throttled`

**BkpMonitorQueueWorker** (task: `bkp_monitor_queue`)

- Reads `thresholdHigh`, `thresholdCritical`. Writes `queueDepth`, `pressureLevel`, `throttlePercent`, `shedPercent`

**BkpShedLoadWorker** (task: `bkp_shed_load`)

- Sets `action` = `"load_shed"`
- Writes `action`, `shedPercent`

**BkpThrottleWorker** (task: `bkp_throttle`)

- Sets `action` = `"throttled"`
- Writes `action`, `throttlePercent`

---

**16 tests** | Workflow: `bkp_backpressure` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
