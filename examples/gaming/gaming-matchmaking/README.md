# Gaming Matchmaking

Orchestrates gaming matchmaking through a multi-stage Conductor workflow.

**Input:** `playerId`, `gameMode`, `region` | **Timeout:** 60s

## Pipeline

```
gmm_search_players
    │
gmm_rate_skill
    │
gmm_match
    │
gmm_create_lobby
    │
gmm_start
```

## Workers

**CreateLobbyWorker** (`gmm_create_lobby`)

Reads `matchId`. Outputs `lobbyId`, `playerCount`, `server`.

**MatchWorker** (`gmm_match`)

```java
result.addOutputData("players", List.of(playerId != null ? playerId : "P-042", "P-101", "P-103", "P-105"));
```

Reads `playerId`. Outputs `matchId`, `players`.

**RateSkillWorker** (`gmm_rate_skill`)

Outputs `ranked`.

**SearchPlayersWorker** (`gmm_search_players`)

Reads `gameMode`, `region`. Outputs `candidates`, `count`.

**StartWorker** (`gmm_start`)

```java
result.addOutputData("game", Map.of("lobbyId", lobbyId != null ? lobbyId : "LOBBY-3301", "status", "IN_PROGRESS", "map", "Arena-7"));
```

Reads `lobbyId`. Outputs `game`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
