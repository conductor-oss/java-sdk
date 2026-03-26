# Content Recommendation

Orchestrates content recommendation through a multi-stage Conductor workflow.

**Input:** `userId`, `contentType`, `maxResults` | **Timeout:** 60s

## Pipeline

```
crm_analyze_history
    │
crm_compute_similarity
    │
crm_rank_results
    │
crm_apply_filters
    │
crm_serve_recommendations
```

## Workers

**AnalyzeHistoryWorker** (`crm_analyze_history`)

Reads `viewedItems`. Outputs `viewedItems`, `likedItems`, `topCategories`, `activityScore`.

**ApplyFiltersWorker** (`crm_apply_filters`)

Reads `filteredItems`. Outputs `filteredItems`, `removedCount`.

**ComputeSimilarityWorker** (`crm_compute_similarity`)

Reads `similarItems`. Outputs `similarItems`, `id`, `score`, `category`.

**RankResultsWorker** (`crm_rank_results`)

Reads `rankedItems`. Outputs `rankedItems`, `rankingStrategy`.

**ServeRecommendationsWorker** (`crm_serve_recommendations`)

Reads `recommendations`. Outputs `recommendations`, `servedCount`, `responseTimeMs`, `trackingId`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
