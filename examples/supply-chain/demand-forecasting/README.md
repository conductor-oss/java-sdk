# Demand Forecasting in Java with Conductor : Data Collection, Trend Analysis, Forecast Generation, and Procurement Planning

## The Problem

You need to forecast demand for consumer electronics across North America over the next 6 months. This requires pulling historical sales data from your ERP, POS systems, and market intelligence feeds, analyzing the data for seasonal patterns (holiday spikes, back-to-school), growth trends, and market shifts, running forecasting models to predict future demand by SKU and region, and translating those forecasts into procurement plans with order quantities and timing. If the forecast over-predicts, you carry excess inventory; if it under-predicts, you stock out during peak demand.

Without orchestration, the data scientist runs a Jupyter notebook that pulls data manually, the supply planner copies forecast numbers into a spreadsheet, and procurement creates POs based on gut feel plus the spreadsheet. There is no reproducibility. nobody knows which data was used for last quarter's forecast. When the model needs retraining, the entire pipeline is re-run from scratch because there is no checkpoint between data collection and forecasting.

## The Solution

**You just write the forecasting workers. Data collection, trend analysis, demand prediction, and procurement planning. Conductor handles data checkpointing, automatic retries on model failures, and versioned records for forecast reproducibility.**

Each stage of the forecasting pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so clean historical data feeds trend analysis, trend outputs parameterize the forecast model, and forecast results drive procurement planning. If the forecasting model worker fails (out-of-memory, timeout), Conductor retries it using the already-collected and analyzed data, no need to re-pull months of sales history. Every data snapshot, trend analysis, forecast output, and procurement plan is versioned and recorded for audit and model improvement.

### What You Write: Workers

Four workers form the forecasting pipeline: CollectDataWorker pulls historical sales data, AnalyzeTrendsWorker identifies seasonal patterns, ForecastWorker generates demand predictions, and PlanWorker translates forecasts into procurement actions.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeTrendsWorker** | `df_analyze_trends` | Analyzes historical data for seasonal patterns, growth trends, and market shifts. |
| **CollectDataWorker** | `df_collect_data` | Collects historical sales and market data for the product category and region. |
| **ForecastWorker** | `df_forecast` | Generates a demand forecast over the specified horizon using trend analysis results. |
| **PlanWorker** | `df_plan` | Translates demand forecasts into procurement plans with order quantities and timing. |

### The Workflow

```
df_collect_data
 │
 ▼
df_analyze_trends
 │
 ▼
df_forecast
 │
 ▼
df_plan

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
