# Demand Forecasting

Demand forecasting: collect data, analyze trends, forecast, and plan.

**Input:** `productCategory`, `horizon`, `region` | **Timeout:** 60s

## Pipeline

```
df_collect_data
    │
df_analyze_trends
    │
df_forecast
    │
df_plan
```

## Workers

**AnalyzeTrendsWorker** (`df_analyze_trends`)

Outputs `trends`, `direction`.

**CollectDataWorker** (`df_collect_data`)

Reads `productCategory`, `region`. Outputs `historicalData`, `dataPointCount`.

**ForecastWorker** (`df_forecast`)

Reads `horizon`. Outputs `forecast`.

**PlanWorker** (`df_plan`)

Reads `forecast`. Outputs `plan`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
