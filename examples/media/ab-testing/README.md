# Ab Testing

Orchestrates ab testing through a multi-stage Conductor workflow.

**Input:** `testId`, `testName`, `variantA`, `variantB`, `sampleSize` | **Timeout:** 60s

## Pipeline

```
abt_define_variants
    │
abt_assign_users
    │
abt_collect_data
    │
abt_analyze_results
    │
abt_decide_winner
```

## Workers

**AnalyzeResultsWorker** (`abt_analyze_results`)

Reads `pValue`. Outputs `pValue`, `uplift`, `confidence`, `statisticallySignificant`, `effectSize`.

**AssignUsersWorker** (`abt_assign_users`)

Reads `groupASize`. Outputs `groupASize`, `groupBSize`, `assignmentMethod`.

**CollectDataWorker** (`abt_collect_data`)

Reads `metricsA`. Outputs `metricsA`, `clicks`, `impressions`, `conversionRate`.

**DecideWinnerWorker** (`abt_decide_winner`)

```java
String rec = !"inconclusive".equals(winner) ? "Roll out variant " + winner : "No clear winner — extend test";
```

Reads `recommendation`, `winner`. Outputs `recommendation`.

**DefineVariantsWorker** (`abt_define_variants`)

Reads `variantA`, `variants`. Outputs `variants`, `id`, `name`, `trafficSplit`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
