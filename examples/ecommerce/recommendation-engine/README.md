# Recommendation Engine

Orchestrates recommendation engine through a multi-stage Conductor workflow.

**Input:** `userId`, `context` | **Timeout:** 60s

## Pipeline

```
rec_collect_behavior
    │
rec_compute_similarity
    │
rec_rank_candidates
    │
rec_personalize
```

## Workers

**CollectBehaviorWorker** (`rec_collect_behavior`): Gathers user browsing behavior: viewed products, purchased products,.

Reads `context`, `userId`. Outputs `viewedProducts`, `purchasedProducts`, `browsingCategories`, `sessionCount`.

**ComputeSimilarityWorker** (`rec_compute_similarity`): Computes similarity scores between user behavior and candidate products.

Reads `userId`. Outputs `similarProducts`.

**PersonalizeWorker** (`rec_personalize`): Personalizes the top-ranked products for the user's context,.

```java
int limit = Math.min(ranked.size(), 3);
```

Reads `context`, `rankedProducts`. Outputs `recommendations`, `context`.

**RankCandidatesWorker** (`rec_rank_candidates`): Ranks candidate products by similarity score and assigns a rank number.

```java
for (int i = 0; i < sorted.size(); i++) {
```

Reads `similarProducts`. Outputs `rankedProducts`, `totalCandidates`.

## Tests

**32 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
