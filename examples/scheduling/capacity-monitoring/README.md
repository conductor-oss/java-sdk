# Capacity Monitoring

A cluster needs capacity forecasting. The pipeline measures current resource utilization, forecasts capacity for the specified number of days ahead, and alerts if any resource (e.g., disk) will run out within the forecast window.

## Workflow

```
cap_measure_resources ──> cap_forecast ──> cap_alert
```

Workflow `capacity_monitoring_418` accepts `cluster` and `forecastDays`. Times out after `60` seconds.

## Workers

**MeasureResourcesWorker** (`cap_measure_resources`) -- measures current resource utilization for the specified cluster.

**ForecastWorker** (`cap_forecast`) -- forecasts capacity for the specified forecast period.

**CapAlertWorker** (`cap_alert`) -- evaluates forecast results. Reports disk capacity alerts with projected days until full.

## Workflow Output

The workflow produces `cpuUsage`, `memoryUsage`, `diskUsage`, `daysUntilDiskFull`, `alertSent` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `capacity_monitoring_418` defines 3 tasks with input parameters `cluster`, `forecastDays` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

3 tests verify resource measurement, capacity forecasting, and alert generation.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
