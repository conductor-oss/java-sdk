# Energy Management

Energy management workflow that monitors consumption, analyzes patterns, optimizes settings, and generates a report.

**Input:** `buildingId`, `period` | **Timeout:** 60s

## Pipeline

```
erg_monitor_consumption
    │
erg_analyze_patterns
    │
erg_optimize
    │
erg_report
```

## Workers

**AnalyzePatternsWorker** (`erg_analyze_patterns`): Analyzes energy usage patterns and identifies peak hours.

Reads `buildingId`. Outputs `patterns`, `peakHours`, `baselineKw`.

**MonitorConsumptionWorker** (`erg_monitor_consumption`): Monitors energy consumption and returns readings with total kWh.

```java
double totalKwh = readings.stream()
```

Reads `buildingId`. Outputs `consumption`, `totalKwh`.

**OptimizeWorker** (`erg_optimize`): Generates optimization recommendations and projected savings.

Reads `buildingId`. Outputs `projectedSavings`, `recommendations`.

**ReportWorker** (`erg_report`): Generates an energy report.

Reads `savings`. Outputs `reportId`, `generated`.

## Tests

**31 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
