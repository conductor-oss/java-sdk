# Season Management

Orchestrates season management through a multi-stage Conductor workflow.

**Input:** `seasonNumber`, `theme`, `durationWeeks` | **Timeout:** 60s

## Pipeline

```
smg_create_season
    │
smg_define_rewards
    │
smg_launch
    │
smg_track
    │
smg_close
```

## Workers

**CloseWorker** (`smg_close`)

```java
r.addOutputData("season", Map.of("seasonId", seasonId != null ? seasonId : "S3-2026", "totalPlayers", 25000, "rewardsDistributed", 18000, "status", "ENDED"));
```

Reads `seasonId`. Outputs `season`.

**CreateSeasonWorker** (`smg_create_season`)

Reads `seasonNumber`, `theme`. Outputs `seasonId`, `created`.

**DefineRewardsWorker** (`smg_define_rewards`)

Reads `seasonId`. Outputs `rewardTiers`.

**LaunchWorker** (`smg_launch`)

Reads `seasonId`. Outputs `launched`, `launchDate`.

**TrackWorker** (`smg_track`)

Reads `seasonId`. Outputs `stats`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
