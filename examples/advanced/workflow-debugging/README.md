# Workflow Debugging

A workflow execution fails at step 4 of 7, but the error message is opaque. The debugging pipeline captures the full execution state at each step, logs input/output data, records timing information, and produces a diagnostic report that pinpoints where and why the failure occurred.

## Pipeline

```
[wfd_instrument]
     |
     v
[wfd_execute]
     |
     v
[wfd_collect_trace]
     |
     v
[wfd_analyze]
     |
     v
[wfd_report]
```

**Workflow inputs:** `workflowName`, `debugLevel`

## Workers

**WfdAnalyzeWorker** (task: `wfd_analyze`)

Analyzes collected trace data for anomalies and performance issues.

- Reads `traceData`. Writes `analysis`

**WfdCollectTraceWorker** (task: `wfd_collect_trace`)

Collects trace data from an instrumented execution.

- Records wall-clock milliseconds
- Writes `traceData`

**WfdExecuteWorker** (task: `wfd_execute`)

Executes the instrumented workflow and captures an execution ID.

- Writes `executionId`, `durationMs`, `taskCount`

**WfdInstrumentWorker** (task: `wfd_instrument`)

Instruments a workflow with debug trace points.

- Writes `instrumentedWorkflow`, `tracePoints`

**WfdReportWorker** (task: `wfd_report`)

Generates a debug report from the analysis results.

- Records wall-clock milliseconds
- Reads `analysis`. Writes `debugReport`

---

**0 tests** | Workflow: `wfd_workflow_debugging` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
