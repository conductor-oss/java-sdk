# Alerting Pipeline

A metric crosses its threshold. The pipeline detects the anomaly by comparing `currentValue` against `threshold`, evaluates alerting rules based on severity, either suppresses the alert (with a reason) or fires it via the configured channel.

## Workflow

```
alt_detect_anomaly ──> alt_evaluate_rules ──> SWITCH
                                                ├── suppress ──> alt_suppress_alert
                                                └── fire ──> alt_send_alert
```

Workflow `alerting_pipeline_413` accepts `metricName`, `currentValue`, and `threshold`. Times out after `60` seconds.

## Workers

**DetectAnomalyWorker** (`alt_detect_anomaly`) -- compares the current value against the threshold and flags anomalies.

**EvaluateRulesWorker** (`alt_evaluate_rules`) -- evaluates alerting rules based on the metric name and severity level.

**SuppressAlertWorker** (`alt_suppress_alert`) -- suppresses the alert and reports the suppression reason.

**SendAlertWorker** (`alt_send_alert`) -- fires the alert at the configured severity via the specified channel.

## Workflow Output

The workflow produces `severity`, `action`, `metricName` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `alerting_pipeline_413` defines 3 tasks with input parameters `metricName`, `currentValue`, `threshold` and a timeout of `60` seconds.

## Tests

3 tests verify anomaly detection, rule evaluation, and the suppress/fire routing decision.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
