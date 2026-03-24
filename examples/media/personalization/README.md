# Personalization

Orchestrates personalization through a multi-stage Conductor workflow.

**Input:** `userId`, `sessionId`, `pageContext` | **Timeout:** 60s

## Pipeline

```
per_collect_profile
    │
per_segment_user
    │
per_select_content
    │
per_rank_content
    │
per_serve_content
```

## Workers

**CollectProfileWorker** (`per_collect_profile`)

Reads `interests`. Outputs `interests`, `demographics`, `region`, `accountAge`.

**RankContentWorker** (`per_rank_content`)

Reads `rankedItems`. Outputs `rankedItems`, `rankingModel`.

**SegmentUserWorker** (`per_segment_user`)

Reads `segment`. Outputs `segment`, `subSegments`, `confidence`.

**SelectContentWorker** (`per_select_content`)

Reads `candidates`. Outputs `candidates`, `id`, `title`, `score`.

**ServeContentWorker** (`per_serve_content`)

Reads `servedCount`. Outputs `servedCount`, `responseTimeMs`, `cacheHit`, `experimentId`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
