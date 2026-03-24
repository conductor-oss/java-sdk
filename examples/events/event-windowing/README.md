# Event Windowing

A real-time analytics system needs to compute metrics over sliding time windows: events per minute, average value per 5-minute window, peak count per hour. The windowing pipeline needs to assign events to windows, detect when a window closes, and emit aggregated results.

## Pipeline

```
[ew_collect_window]
     |
     v
[ew_compute_stats]
     |
     v
[ew_emit_result]
```

**Workflow inputs:** `events`, `windowSizeMs`

## Workers

**CollectWindowWorker** (task: `ew_collect_window`)

Collects incoming events into a fixed time window. Passes through all events that fall within the configured window, tagging them with a window identifier.

- Reads `events`, `windowSizeMs`. Writes `windowEvents`, `windowId`, `windowSizeMs`

**ComputeStatsWorker** (task: `ew_compute_stats`)

Computes aggregate statistics (count, min, max, sum, avg) over the value field of windowed events. Returns consistent values.

- Reads `windowId`. Writes `stats`, `windowId`

**EmitResultWorker** (task: `ew_emit_result`)

Emits the final windowed result, confirming the window was successfully processed and published.

- Reads `windowId`. Writes `emitted`, `windowId`

---

**27 tests** | Workflow: `event_windowing` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
