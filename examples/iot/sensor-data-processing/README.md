# Sensor Data Processing in Java with Conductor : Collection, Validation, Aggregation, Anomaly Detection, and Alerting

## Why Sensor Data Pipelines Need Orchestration

Processing IoT sensor data at scale is a multi-stage pipeline where data quality and ordering matter. You collect 1,200 readings from 50 sensors in a time window. You validate each reading for data quality. flagging null values, detecting offline sensors, and filtering bad data. You aggregate the validated readings into summary statistics: average temperature, min/max bounds, standard deviation, humidity averages. You analyze those aggregated metrics for patterns and anomalies, is the temperature trend rising? Did any sensor exceed the 85F threshold? If anomalies are detected, you trigger alerts at the appropriate escalation level.

Each stage depends on clean output from the previous one. aggregation on validated data, pattern analysis on aggregated metrics, alerting on detected anomalies. If you aggregate raw data without validation, offline sensors inject zeros that skew your averages. If you skip pattern analysis and alert on raw readings, you get false positives from sensor noise. Without orchestration, you'd build a monolithic data processor that mixes ingestion, validation, statistics, and alerting, making it impossible to tune the anomaly detection threshold without risking the data validation logic.

## How This Workflow Solves It

**You just write the sensor pipeline workers. Data collection, validation, aggregation, anomaly detection, and alert escalation. Conductor handles validated-data-first ordering, sensor group retries, and traced records linking each alert to the readings and anomalies that caused it.**

Each pipeline stage is an independent worker. collect readings, validate data, aggregate metrics, analyze patterns, trigger alerts. Conductor sequences them, passes the validated reading count and cleaned data between stages, retries if a sensor group poll times out, and tracks exactly which readings, metrics, and anomalies led to each alert.

### What You Write: Workers

Five workers process sensor streams: CollectReadingsWorker ingests batches from sensor groups, ValidateDataWorker filters bad data and offline sensors, AggregateReadingsWorker computes summary statistics, AnalyzePatternsWorker detects anomalies, and TriggerAlertsWorker escalates notifications.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateReadingsWorker** | `sen_aggregate_readings` | Aggregates validated sensor readings into summary metrics. |
| **AnalyzePatternsWorker** | `sen_analyze_patterns` | Analyzes aggregated sensor metrics for patterns and anomalies. |
| **CollectReadingsWorker** | `sen_collect_readings` | Collects sensor readings from a sensor group within a time window. |
| **TriggerAlertsWorker** | `sen_trigger_alerts` | Triggers alerts based on detected anomalies. |
| **ValidateDataWorker** | `sen_validate_data` | Validates sensor readings for data quality. |

the workflow and alerting logic stay the same.

### The Workflow

```
Input -> AggregateReadingsWorker -> AnalyzePatternsWorker -> CollectReadingsWorker -> TriggerAlertsWorker -> ValidateDataWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
