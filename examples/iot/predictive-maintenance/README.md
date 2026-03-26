# Predictive Maintenance

Orchestrates predictive maintenance through a multi-stage Conductor workflow.

**Input:** `assetId`, `assetType`, `siteId` | **Timeout:** 60s

## Pipeline

```
pmn_collect_data
    │
pmn_analyze_trends
    │
pmn_predict_failure
    │
pmn_schedule_maintenance
```

## Workers

**AnalyzeTrendsWorker** (`pmn_analyze_trends`)

Reads `trendAnalysis`. Outputs `trendAnalysis`, `temperatureSlope`, `vibrationSlope`, `overallHealth`.

**CollectDataWorker** (`pmn_collect_data`)

Reads `operationalData`. Outputs `operationalData`, `currentTemp`, `vibrationLevel`, `operatingHours`, `lastMaintenanceHours`.

**PredictFailureWorker** (`pmn_predict_failure`)

Reads `predictedFailureDate`. Outputs `predictedFailureDate`, `confidence`, `daysUntilFailure`, `recommendedAction`, `riskLevel`.

**ScheduleMaintenanceWorker** (`pmn_schedule_maintenance`)

Reads `maintenanceDate`. Outputs `maintenanceDate`, `workOrderId`, `estimatedCost`, `partsOrdered`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
