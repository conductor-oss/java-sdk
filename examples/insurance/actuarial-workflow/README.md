# Actuarial Workflow

Orchestrates actuarial workflow through a multi-stage Conductor workflow.

**Input:** `lineOfBusiness`, `analysisYear`, `modelType` | **Timeout:** 60s

## Pipeline

```
act_collect_data
    │
act_model
    │
act_run_analysis
    │
act_analyze
    │
act_report
```

## Workers

**AnalyzeWorker** (`act_analyze`)

Outputs `analysis`.

**CollectDataWorker** (`act_collect_data`)

Reads `lineOfBusiness`. Outputs `dataSet`.

**ModelWorker** (`act_model`)

Outputs `modelId`, `r2`, `variables`.

**ReportWorker** (`act_report`)

Reads `lineOfBusiness`. Outputs `reportId`, `generated`.

**RunAnalysisWorker** (`act_run_analysis`)

Outputs `results`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
