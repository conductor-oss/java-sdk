# Workflow Optimization

A workflow definition has accumulated unnecessary steps over time. Some tasks run sequentially when they could run in parallel; others fetch data that is never used downstream. The optimization pipeline analyzes the workflow DAG, identifies parallelization opportunities, detects dead tasks, and produces an optimized definition.

## Pipeline

```
[wfo_analyze_execution]
     |
     v
[wfo_identify_waste]
     |
     v
[wfo_parallelize]
     |
     v
[wfo_benchmark]
```

**Workflow inputs:** `workflowName`, `executionHistory`

## Workers

**WfoAnalyzeExecutionWorker** (task: `wfo_analyze_execution`)

- Writes `dependencyGraph`, `taskTimings`, `originalOrder`, `totalDurationMs`

**WfoBenchmarkWorker** (task: `wfo_benchmark`)

- Reads `originalDurationMs`. Writes `benchmarkResult`

**WfoIdentifyWasteWorker** (task: `wfo_identify_waste`)

- Writes `wasteItems`, `independentTasks`, `totalWasteMs`

**WfoParallelizeWorker** (task: `wfo_parallelize`)

- Writes `optimizedPlan`

---

**16 tests** | Workflow: `wfo_workflow_optimization` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
