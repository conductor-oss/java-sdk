# Program Evaluation

Orchestrates program evaluation through a multi-stage Conductor workflow.

**Input:** `programName`, `evaluationPeriod` | **Timeout:** 60s

## Pipeline

```
pev_define_metrics
    │
pev_collect
    │
pev_analyze
    │
pev_benchmark
    │
pev_recommend
```

## Workers

**AnalyzeWorker** (`pev_analyze`)

Outputs `results`.

**BenchmarkWorker** (`pev_benchmark`)

Outputs `benchmarks`.

**CollectWorker** (`pev_collect`)

Reads `period`. Outputs `collected`.

**DefineMetricsWorker** (`pev_define_metrics`)

Reads `programName`. Outputs `metrics`.

**RecommendWorker** (`pev_recommend`)

Outputs `evaluation`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
