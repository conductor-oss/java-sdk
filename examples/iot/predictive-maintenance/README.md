# Predictive Maintenance in Java with Conductor : Sensor Data Collection, Trend Analysis, Failure Prediction, and Maintenance Scheduling

## Why Predictive Maintenance Needs Orchestration

Preventing unplanned downtime requires turning raw sensor data into maintenance decisions. You collect current operational data. bearing temperature at 178F, vibration at 3.8 mm/s, 18,500 operating hours since install, 2,500 hours since last maintenance. You analyze trends by computing temperature and vibration slopes over time to derive an overall health score. You feed those trends into a failure prediction model that estimates when the asset will fail (e.g., "May 25, compressor valve, 82% confidence, medium risk"). If maintenance is warranted, you schedule a work order two weeks before the predicted failure date, order the replacement parts (compressor valve CV-300), and estimate the cost.

Each step in this pipeline depends on the previous one. trend analysis needs current data, failure prediction needs trends, and scheduling needs the predicted failure date. If the sensor data pull fails, you do not want stale trend data feeding the predictor. Without orchestration, you'd build a monolithic condition monitoring system where data collection, statistical analysis, ML inference, and CMMS integration are tangled together, making it impossible to upgrade your prediction model, test scheduling logic independently, or trace which sensor readings led to a specific work order.

## How This Workflow Solves It

**You just write the maintenance workers. Sensor data collection, degradation trend analysis, failure prediction, and work order scheduling. Conductor handles sensor-to-work-order sequencing, data collection retries, and traceable records linking predictions to maintenance actions.**

Each maintenance stage is an independent worker. collect data, analyze trends, predict failure, schedule maintenance. Conductor sequences them, passes operational metrics through trend analysis into the prediction model and scheduling logic, retries if a sensor poll times out, and maintains a complete audit trail linking every work order to the specific readings and predictions that triggered it.

### What You Write: Workers

Four workers drive the maintenance cycle: CollectDataWorker gathers sensor readings and baselines, AnalyzeTrendsWorker computes degradation slopes and health scores, PredictFailureWorker estimates failure dates with confidence levels, and ScheduleMaintenanceWorker creates work orders and triggers parts procurement.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeTrendsWorker** | `pmn_analyze_trends` | Computes degradation slopes, health scores, and trend analysis from operational and historical data. |
| **CollectDataWorker** | `pmn_collect_data` | Collects current operational data (temperature, vibration, operating hours) and historical baselines from sensors. |
| **PredictFailureWorker** | `pmn_predict_failure` | Predicts failure date, identifies the likely failing component, and recommends a repair action with confidence score. |
| **ScheduleMaintenanceWorker** | `pmn_schedule_maintenance` | Creates a maintenance work order, triggers parts procurement, and schedules the repair window before predicted failure. |

the workflow and alerting logic stay the same.

### The Workflow

```
pmn_collect_data
 │
 ▼
pmn_analyze_trends
 │
 ▼
pmn_predict_failure
 │
 ▼
pmn_schedule_maintenance

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
