# Environmental Monitoring in Java with Conductor : Air Quality Sensing, Threshold Alerts, and Compliance Reporting

## Why Air Quality Monitoring Needs Orchestration

Environmental monitoring stations collect readings across multiple pollutants. particulate matter (PM2.5, PM10), gases (CO2, NO2, ozone), and ambient conditions (temperature, humidity). Each reading must be checked against regulatory thresholds to compute an Air Quality Index. When the AQI exceeds safe levels, alerts go out to environmental teams and the monitoring dashboard. Regardless of alert status, a compliance report must be generated for regulatory review.

If a sensor reading fails mid-collection, you need to know which pollutants were already captured so you can retry without duplicating data. If threshold checking reveals a breach, the alert and report steps both need that information. Without orchestration, you'd build a monolithic monitoring script that mixes sensor polling, threshold logic, notification dispatch, and report generation. making it impossible to swap out your sensor data source, test threshold rules independently, or audit which readings triggered which alerts.

## How This Workflow Solves It

**You just write the environmental monitoring workers. Sensor data collection, threshold checking, alert triggering, and compliance reporting. Conductor handles collection-to-report sequencing, sensor polling retries, and provable regulatory records linking every reading to every alert.**

Each monitoring concern is an independent worker. collect sensor data, check thresholds, trigger alerts, generate reports. Conductor sequences them, passes pollutant readings and breach counts between steps, retries if a sensor poll times out, and provides a complete audit trail of every reading, threshold check, and alert for regulatory compliance.

### What You Write: Workers

Four workers run the monitoring cycle: CollectDataWorker reads pollutant concentrations from station sensors, CheckThresholdsWorker evaluates AQI against regulatory limits, TriggerAlertWorker dispatches breach notifications, and GenerateReportWorker produces the compliance record.

| Worker | Task | What It Does |
|---|---|---|
| **CheckThresholdsWorker** | `env_check_thresholds` | Compares pollutant readings against regulatory thresholds to compute AQI and identify breaches. |
| **CollectDataWorker** | `env_collect_data` | Collects air quality readings (PM2.5, PM10, CO2, NO2, ozone) from monitoring station sensors. |
| **GenerateReportWorker** | `env_generate_report` | Generates a regulatory compliance report with readings, breaches, and alert history. |
| **TriggerAlertWorker** | `env_trigger_alert` | Sends threshold breach alerts to environmental teams and monitoring dashboards. |

the workflow and alerting logic stay the same.

### The Workflow

```
env_collect_data
 │
 ▼
env_check_thresholds
 │
 ▼
env_trigger_alert
 │
 ▼
env_generate_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
