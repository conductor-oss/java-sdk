# Workflow Profiling in Java Using Conductor : Instrument, Execute, Measure, Find Bottlenecks, Optimize

## You Can't Optimize What You Don't Measure

Your workflow runs in 30 seconds, but where does the time go? Is it the database query in step 3 (takes 12 seconds) or the API call in step 7 (takes 8 seconds)? Running the workflow once gives you one data point. Running it 100 times and profiling each task across iterations reveals the real bottleneck. maybe step 3 averages 2 seconds but occasionally spikes to 12 seconds, while step 7 is consistently 8 seconds.

Workflow profiling means instrumenting tasks to capture high-resolution timing, running multiple iterations to build statistical profiles, computing per-task percentiles (p50, p95, p99), and identifying which tasks contribute most to end-to-end latency. Without this data, optimization efforts target the wrong tasks.

## The Solution

**You write the measurement and bottleneck analysis logic. Conductor handles multi-iteration execution, retries, and profiling session tracking.**

`WfpInstrumentWorker` adds timing hooks to the target workflow. `WfpExecuteWorker` runs the workflow across the configured number of iterations, collecting timing data for each run. `WfpMeasureTimesWorker` computes per-task statistics. mean, median, p95, p99, across all iterations. `WfpBottleneckWorker` identifies the tasks that dominate the critical path and have the highest latency variance. `WfpOptimizeWorker` generates concrete optimization recommendations (cache this lookup, parallelize these two tasks, increase timeout for this external call). Conductor records the full profiling session.

### What You Write: Workers

Five workers form the profiling pipeline: instrumentation, multi-iteration execution, per-task timing measurement, bottleneck detection, and optimization recommendations, each feeding statistical data to the next.

| Worker | Task | What It Does |
|---|---|---|
| **WfpBottleneckWorker** | `wfp_bottleneck` | Identifies the slowest tasks and ranks them by severity (critical, high) |
| **WfpExecuteWorker** | `wfp_execute` | Runs the instrumented workflow and collects per-iteration execution times |
| **WfpInstrumentWorker** | `wfp_instrument` | Adds profiling hooks (CPU time, wall time, memory usage) to the target workflow |
| **WfpMeasureTimesWorker** | `wfp_measure_times` | Measures average execution times for each task and computes total workflow duration |
| **WfpOptimizeWorker** | `wfp_optimize` | Produces optimization suggestions (e.g.### The Workflow

```
wfp_instrument
 │
 ▼
wfp_execute
 │
 ▼
wfp_measure_times
 │
 ▼
wfp_bottleneck
 │
 ▼
wfp_optimize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
