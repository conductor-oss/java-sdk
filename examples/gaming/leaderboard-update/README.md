# Leaderboard Update

Orchestrates leaderboard update through a multi-stage Conductor workflow.

**Input:** `gameId`, `season` | **Timeout:** 60s

## Pipeline

```
lbu_collect_scores
    │
lbu_validate
    │
lbu_rank
    │
lbu_update
    │
lbu_broadcast
```

## Workers

**BroadcastWorker** (`lbu_broadcast`)

```java
result.addOutputData("leaderboard", Map.of("id", lbId != null ? lbId : "LB-742", "top3", List.of("P-101","P-105","P-042"), "status", "LIVE"));
```

Reads `leaderboardId`. Outputs `leaderboard`.

**CollectScoresWorker** (`lbu_collect_scores`)

Reads `gameId`. Outputs `scores`.

**RankWorker** (`lbu_rank`)

Outputs `rankings`.

**UpdateWorker** (`lbu_update`)

Reads `season`. Outputs `leaderboardId`, `updated`.

**ValidateWorker** (`lbu_validate`)

Reads `scores`. Outputs `validScores`, `rejected`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
