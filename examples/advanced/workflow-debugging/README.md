# Workflow Debugging in Java Using Conductor : Instrument, Execute, Trace, Analyze, Report

## When Workflows Fail, You Need More Than a Stack Trace

A 20-step workflow fails at step 14. The error says "null pointer exception." But the real question is: what were the inputs at step 14? What did step 13 output? How long did each step take? Did step 7 produce unexpected output that propagated silently until step 14 crashed? You need distributed tracing across the entire workflow execution. not just the error at the failure point.

Workflow debugging means instrumenting the workflow to capture detailed state at each step, executing with tracing enabled at a configurable debug level, collecting the full execution trace including inputs, outputs, and timing, analyzing the trace to find the root cause (data anomalies, unexpected branches, performance bottlenecks), and producing a human-readable debug report.

## The Solution

**You write the instrumentation and analysis logic. Conductor handles trace collection, retries, and debug session tracking.**

`WfdInstrumentWorker` adds debug hooks to the target workflow at the specified debug level (minimal, standard, verbose). `WfdExecuteWorker` runs the instrumented workflow and captures detailed execution data. `WfdCollectTraceWorker` gathers the distributed execution trace. every task's inputs, outputs, timing, and status. `WfdAnalyzeWorker` examines the trace for anomalies: null values that shouldn't be null, execution times outside expected ranges, unexpected branch selections. `WfdReportWorker` produces the final debug report. Conductor records the full debugging session for replay.

### What You Write: Workers

Five workers form the debug cycle: instrumentation, traced execution, trace collection, anomaly analysis, and report generation, each capturing one layer of diagnostic data.

| Worker | Task | What It Does |
|---|---|---|
| `WfdInstrumentWorker` | `wfd_instrument` | Takes a workflow name and debug level (e.g., INFO), injects trace points (timing hooks at task start/end, data capture at decision branches) into the workflow, and returns the instrumented workflow name with the list of trace points |
| `WfdExecuteWorker` | `wfd_execute` | Runs the instrumented workflow and returns an execution ID, total duration in milliseconds, and the number of tasks executed |
| `WfdCollectTraceWorker` | `wfd_collect_trace` | Gathers trace data from the execution by reading each trace point. captures timestamps and recorded values (timing measurements, branch selections) for every instrumented location |
| `WfdAnalyzeWorker` | `wfd_analyze` | Examines collected trace data for anomalies (e.g., tasks exceeding duration thresholds), identifies performance bottlenecks, and produces a summary of findings |
| `WfdReportWorker` | `wfd_report` | Generates a final debug report containing the workflow name, timestamp, anomaly count, and actionable recommendations (e.g.### The Workflow

```
wfd_instrument
 │
 ▼
wfd_execute
 │
 ▼
wfd_collect_trace
 │
 ▼
wfd_analyze
 │
 ▼
wfd_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
