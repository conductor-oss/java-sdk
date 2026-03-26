# Credit Scoring

Credit scoring: collect data, calculate factors, compute score, classify applicant.

**Input:** `applicantId`, `ssn` | **Timeout:** 60s

## Pipeline

```
csc_collect_data
    │
csc_calculate_factors
    │
csc_score
    │
csc_classify
```

## Workers

**CalculateFactorsWorker** (`csc_calculate_factors`): Calculates weighted credit score factors from credit history.

Reads `creditHistory`. Outputs `factors`.

**ClassifyWorker** (`csc_classify`): Classifies an applicant based on credit score.

- `score >= 800` &rarr; `"Exceptional"`
- `score >= 740` &rarr; `"Very Good"`
- `score >= 670` &rarr; `"Good"`
- `score >= 580` &rarr; `"Fair"`

```java
String approvalLikelihood = score >= 670 ? "high" : "low";
```

Reads `score`. Outputs `classification`, `approvalLikelihood`.

**CollectDataWorker** (`csc_collect_data`): Collects credit history data for an applicant.

Reads `applicantId`. Outputs `creditHistory`.

**ScoreWorker** (`csc_score`): Computes a weighted credit score from factor data.

```java
weightedScore += (score * weight) / 100.0;
int finalScore = (int) Math.round(300 + (weightedScore / 100.0) * 550);
```

Reads `factors`. Outputs `score`, `model`, `computedAt`.

## Tests

**35 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
