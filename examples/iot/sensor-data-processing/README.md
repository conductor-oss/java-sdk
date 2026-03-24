# Sensor Data Processing

Sensor data processing: collect readings, validate, aggregate, analyze patterns, trigger alerts

**Input:** `batchId`, `sensorGroupId`, `timeWindowMinutes` | **Timeout:** 1800s

## Pipeline

```
sen_collect_readings
    │
sen_validate_data
    │
sen_aggregate_readings
    │
sen_analyze_patterns
    │
sen_trigger_alerts
```

## Workers

**AggregateReadingsWorker** (`sen_aggregate_readings`): Aggregates validated sensor readings into summary metrics.

Reads `validReadings`. Outputs `aggregatedMetrics`, `timeRange`.

**AnalyzePatternsWorker** (`sen_analyze_patterns`): Analyzes aggregated sensor metrics for patterns and anomalies.

```java
boolean hasAnomaly = maxTemp > 85;
```

Reads `aggregatedMetrics`. Outputs `trend`, `anomalies`, `forecastNext1h`.

**CollectReadingsWorker** (`sen_collect_readings`): Collects sensor readings from a sensor group within a time window.

Reads `sensorGroupId`. Outputs `readingCount`, `readings`, `sensorCount`.

**TriggerAlertsWorker** (`sen_trigger_alerts`): Triggers alerts based on detected anomalies.

Reads `anomalies`, `trend`. Outputs `alertsTriggered`, `notifications`, `escalationLevel`.

**ValidateDataWorker** (`sen_validate_data`): Validates sensor readings for data quality.

```java
int validCount = (int) Math.round(count * 0.98);
```

Reads `readingCount`, `readings`. Outputs `validCount`, `invalidCount`, `validatedData`, `issues`.

## Tests

**41 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
