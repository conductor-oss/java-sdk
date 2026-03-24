# Cost Monitoring

A cloud account needs budget tracking. The pipeline collects billing data for the specified period, analyzes spending trends, and alerts when the budget percentage crosses the threshold.

## Workflow

```
cos_collect_billing ──> cos_analyze_trends ──> cos_alert_anomalies
```

Workflow `cost_monitoring_419` accepts `accountId`, `billingPeriod`, and `budgetLimit`. Times out after `60` seconds.

## Workers

**CollectBillingWorker** (`cos_collect_billing`) -- collects billing data for the specified account and period.

**AnalyzeTrendsWorker** (`cos_analyze_trends`) -- analyzes spending trends across the billing data.

**CosAlertWorker** (`cos_alert_anomalies`) -- calculates budget utilization percentage and triggers alerts when thresholds are exceeded.

## Workflow Output

The workflow produces `totalSpend`, `trend`, `percentOfBudget`, `alertSent` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 3 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `cost_monitoring_419` defines 3 tasks with input parameters `accountId`, `billingPeriod`, `budgetLimit` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify billing collection, trend analysis, and budget alert generation.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
