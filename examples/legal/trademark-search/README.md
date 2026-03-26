# Trademark Search

Orchestrates trademark search through a multi-stage Conductor workflow.

**Input:** `trademarkName`, `goodsAndServices` | **Timeout:** 60s

## Pipeline

```
tmk_search
    │
tmk_conflicts
    │
tmk_assess
    │
tmk_recommend
```

## Workers

**AssessWorker** (`tmk_assess`)

Outputs `assessment`, `riskLevel`.

**ConflictsWorker** (`tmk_conflicts`)

Outputs `conflicts`, `conflictCount`.

**RecommendWorker** (`tmk_recommend`)

Reads `trademarkName`. Outputs `recommendation`, `suggestedAlternatives`.

**SearchWorker** (`tmk_search`)

Reads `trademarkName`. Outputs `searchResults`, `totalResults`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
