# Anomaly Detection

A time-series metric needs statistical anomaly detection. The pipeline collects data points for the specified lookback period, computes a baseline (mean and standard deviation), detects deviations, classifies the anomaly type, and optionally sends an alert.

## Workflow

```
anom_collect_data ──> anom_compute_baseline ──> anom_detect ──> anom_classify ──> anom_alert
```

Workflow `anomaly_detection_414` accepts `metricName`, `lookbackHours`, and `sensitivity`. Times out after `60` seconds.

## Workers

**CollectDataWorker** (`anom_collect_data`) -- collects data points for the metric over the lookback period.

**ComputeBaselineWorker** (`anom_compute_baseline`) -- computes the statistical baseline from collected data.

**DetectWorker** (`anom_detect`) -- compares current values against the baseline. Reports `"stdDev is zero"` when data is constant.

**ClassifyWorker** (`anom_classify`) -- classifies the anomaly type based on the metric name.

**AlertWorker** (`anom_alert`) -- sends or skips the alert based on detection results.

## Workflow Output

The workflow produces `isAnomaly`, `classification`, `severity`, `alerted` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 5 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `anomaly_detection_414` defines 5 tasks with input parameters `metricName`, `lookbackHours`, `sensitivity` and a timeout of `60` seconds.

## Tests

6 tests verify data collection, baseline computation, anomaly detection, classification, and alert dispatch.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
