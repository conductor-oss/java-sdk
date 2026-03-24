# Player Progression

Orchestrates player progression through a multi-stage Conductor workflow.

**Input:** `playerId`, `questId`, `xpEarned` | **Timeout:** 60s

## Pipeline

```
ppg_complete_task
    │
ppg_award_xp
    │
ppg_check_level
    │
ppg_unlock_rewards
    │
ppg_notify
```

## Workers

**AwardXpWorker** (`ppg_award_xp`)

```java
r.addOutputData("totalXp", 4800 + xp); r.addOutputData("awarded", xp);
```

Reads `playerId`, `xpEarned`. Outputs `totalXp`, `awarded`.

**CheckLevelWorker** (`ppg_check_level`)

```java
int newLevel = totalXp / 1000;
```

Reads `totalXp`. Outputs `newLevel`, `leveledUp`, `totalXp`.

**CompleteTaskWorker** (`ppg_complete_task`)

Reads `playerId`, `questId`. Outputs `completed`, `questName`.

**NotifyWorker** (`ppg_notify`)

Reads `playerId`, `rewards`. Outputs `progression`.

**UnlockRewardsWorker** (`ppg_unlock_rewards`)

```java
boolean leveledUp = Boolean.TRUE.equals(lu) || "true".equals(String.valueOf(lu));
```

Reads `leveledUp`. Outputs `rewards`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
