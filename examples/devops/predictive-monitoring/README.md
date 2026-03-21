# Predictive Monitoring in Java with Conductor : Collect History, Train Model, Predict, Alert

Automates predictive monitoring using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects historical metric data, trains a forecasting model (e.g., Prophet), predicts future metric values and breach likelihood, and sends proactive alerts before thresholds are actually crossed.

## Fixing Problems Before They Happen

Your CPU usage has been climbing steadily for 30 days. Will it breach 90% this week? Instead of waiting for an alert to fire at 3 AM, predictive monitoring analyzes 30 days of historical data (43,200 data points at 1-minute granularity), trains a forecasting model, and tells you there is a 72.3% chance of breach with a predicted peak of 88.5% by tomorrow afternoon. You get a warning alert now, while you can still scale up or optimize. Not a critical page when it is already too late.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the forecasting model and alert logic. Conductor handles the data-collection-to-prediction pipeline and tracks every forecast for accuracy validation.**

`CollectHistoryWorker` gathers historical metric data for the monitoring target. time-series values with timestamps spanning the training window. `TrainModelWorker` fits a prediction model to the historical data, learning trends, seasonality, and growth patterns. `PredictWorker` uses the trained model to forecast future metric values over the prediction horizon, with confidence intervals. `AlertWorker` evaluates the predictions against thresholds and sends early warnings if a breach is forecasted, including the predicted breach date and recommended actions. Conductor records every prediction for model accuracy tracking over time.

### What You Write: Workers

Four workers power predictive monitoring. Collecting historical metrics, training a forecasting model, predicting future values, and alerting before thresholds are breached.

| Worker | Task | What It Does |
|---|---|---|
| **CollectHistory** | `pdm_collect_history` | Collects historical metric data points for the specified metric name and time range |
| **PdmAlert** | `pdm_alert` | Evaluates breach likelihood and sends a proactive warning or critical alert if the threshold is likely to be crossed |
| **Predict** | `pdm_predict` | Forecasts future metric values using the trained model, outputting predicted peak, timing, and breach likelihood with confidence intervals |
| **TrainModel** | `pdm_train_model` | Trains a time-series forecasting model (e.g., Prophet) on the collected historical data points |

the workflow and rollback logic stay the same.

### The Workflow

```
Input -> CollectHistory -> PdmAlert -> Predict -> TrainModel -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
