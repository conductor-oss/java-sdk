# Workflow Profiling

A workflow that used to complete in 30 seconds now takes 5 minutes. The profiling pipeline instruments each task with start/end timestamps, computes per-task durations, identifies the bottleneck task, and generates a flame-graph-style report showing where time is spent.

## Pipeline

```
[wfp_instrument]
     |
     v
[wfp_execute]
     |
     v
[wfp_measure_times]
     |
     v
[wfp_bottleneck]
     |
     v
[wfp_optimize]
```

**Workflow inputs:** `workflowName`, `iterations`

## Workers

**WfpBottleneckWorker** (task: `wfp_bottleneck`)

- Writes `bottlenecks`

**WfpExecuteWorker** (task: `wfp_execute`)

- Writes `executionResults`

**WfpInstrumentWorker** (task: `wfp_instrument`)

- Writes `instrumentedWorkflow`, `profilingHooks`

**WfpMeasureTimesWorker** (task: `wfp_measure_times`)

- Writes `timings`, `totalAvgMs`

**WfpOptimizeWorker** (task: `wfp_optimize`)

- Writes `optimizations`, `expectedSpeedup`

---

**20 tests** | Workflow: `wfp_workflow_profiling` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
