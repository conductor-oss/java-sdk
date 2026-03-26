# Clinical Decision

Clinical Decision Support: gather data, apply guidelines, score risk, recommend

**Input:** `patientId`, `condition`, `clinicalContext` | **Timeout:** 60s

## Pipeline

```
cds_gather_data
    │
cds_apply_guidelines
    │
cds_score_risk
    │
cds_recommend
```

## Workers

**ApplyGuidelinesWorker** (`cds_apply_guidelines`)

Reads `condition`. Outputs `guidelineResults`.

**GatherDataWorker** (`cds_gather_data`)

Reads `condition`, `patientId`. Outputs `clinicalData`.

**RecommendWorker** (`cds_recommend`)

```java
recs.add(Map.of("priority", 2, "action", "Optimize blood pressure to < 130/80", "evidence", "Class I, Level A"));
```

Reads `riskScore`. Outputs `recommendations`.

**ScoreRiskWorker** (`cds_score_risk`)

```java
String category = riskScore >= 20 ? "high" : riskScore >= 7.5 ? "moderate" : "low";
```

Outputs `riskScore`, `riskCategory`, `model`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
