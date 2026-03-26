# Population Health

Population Health: aggregate data, stratify risk, identify care gaps, intervene

**Input:** `cohortId`, `condition`, `reportingPeriod` | **Timeout:** 60s

## Pipeline

```
pop_aggregate_data
    │
pop_stratify_risk
    │
pop_identify_gaps
    │
pop_intervene
```

## Workers

**AggregateDataWorker** (`pop_aggregate_data`)

Reads `cohortId`, `condition`. Outputs `populationData`, `totalPatients`.

**IdentifyGapsWorker** (`pop_identify_gaps`)

Outputs `careGaps`, `totalGaps`.

**InterveneWorker** (`pop_intervene`)

```java
interventions.add(Map.of("gap", "Overdue HbA1c", "action", "Outreach campaign — lab order + appointment", "targetCount", 300));
```

Reads `careGaps`. Outputs `interventions`, `totalPatientsTargeted`.

**StratifyRiskWorker** (`pop_stratify_risk`)

```java
strata.add(Map.of("level", "high", "count", 490, "percentage", 20, "criteria", "HbA1c > 9, complications"));
```

Outputs `riskStrata`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
