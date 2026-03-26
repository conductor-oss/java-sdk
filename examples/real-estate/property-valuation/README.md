# Property Valuation

Orchestrates property valuation through a multi-stage Conductor workflow.

**Input:** `propertyId`, `address` | **Timeout:** 60s

## Pipeline

```
pvl_collect_comps
    │
pvl_analyze
    │
pvl_appraise
    │
pvl_report
```

## Workers

**AnalyzeWorker** (`pvl_analyze`)

Outputs `analysis`.

**AppraiseWorker** (`pvl_appraise`)

Outputs `appraisal`.

**CollectCompsWorker** (`pvl_collect_comps`)

Reads `address`. Outputs `comps`.

**ReportWorker** (`pvl_report`)

Outputs `reportId`, `generated`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
