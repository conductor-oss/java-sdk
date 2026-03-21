# Leaderboard Update in Java Using Conductor

Updates a game's leaderboard for a season: collecting match scores, validating for integrity, ranking players, updating the public leaderboard, and broadcasting results to connected players. ## The Problem

You need to update a game's leaderboard for a season. The workflow collects scores from all completed matches, validates them for integrity (checking for cheating flags, impossible scores, or data corruption), ranks players by their validated scores, updates the public leaderboard, and broadcasts the updated rankings to all connected players. Posting invalid scores to the leaderboard undermines competitive integrity; stale leaderboards reduce player engagement.

Without orchestration, you'd build a batch leaderboard job that queries match results, filters invalid scores, sorts by rank, updates the leaderboard database, and pushes updates to clients. manually handling ties, pagination for large player bases, and real-time update latency.

## The Solution

**You just write the score collection, integrity validation, ranking, leaderboard update, and broadcast logic. Conductor handles score validation retries, ranking recalculation, and leaderboard audit trails.**

Each leaderboard concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect, validate, rank, update, broadcast), retrying if the leaderboard service is slow, tracking every leaderboard update cycle, and resuming from the last step if the process crashes. ### What You Write: Workers

Score validation, ranking calculation, leaderboard update, and notification workers each handle one aspect of competitive standings.

| Worker | Task | What It Does |
|---|---|---|
| **BroadcastWorker** | `lbu_broadcast` | Broadcasts the updated leaderboard with top-3 players to all connected clients |
| **CollectScoresWorker** | `lbu_collect_scores` | Collects match scores for all players in the game with player IDs and score values |
| **RankWorker** | `lbu_rank` | Computes player rankings from validated scores, assigning rank positions |
| **UpdateWorker** | `lbu_update` | Writes the computed rankings to the leaderboard store for the current season |
| **ValidateWorker** | `lbu_validate` | Validates collected scores for integrity, filtering out flagged or impossible entries |

### The Workflow

```
lbu_collect_scores
 │
 ▼
lbu_validate
 │
 ▼
lbu_rank
 │
 ▼
lbu_update
 │
 ▼
lbu_broadcast

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
