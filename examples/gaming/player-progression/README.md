# Player Progression in Java Using Conductor

Processes player progression after completing a quest: recording the completion, awarding XP, checking for level-up, unlocking level rewards (items, abilities, titles), and notifying the player.

## The Problem

You need to process a player's progression after completing a quest or challenge. The player completes a task, earns experience points (XP), the system checks whether they have leveled up, unlocks any rewards associated with the new level (items, abilities, titles), and notifies the player of their achievements. Awarding XP without checking for level-up means players miss their rewards; not notifying means they do not feel the satisfaction of progression.

Without orchestration, you'd handle progression in a single game server callback that grants XP, checks level thresholds, queries the reward table, updates the player's inventory, and sends a notification. manually handling concurrent quest completions, ensuring idempotent XP grants, and managing the cascade of unlocks when a player levels up multiple times.

## The Solution

**You just write the quest completion, XP awarding, level-up checking, reward unlocking, and player notification logic. Conductor handles XP calculation retries, level-up sequencing, and progression audit trails.**

Each progression concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (complete task, award XP, check level, unlock rewards, notify), retrying if the player database is slow, tracking every progression event, and resuming from the last step if the process crashes.

### What You Write: Workers

XP calculation, level evaluation, reward unlocking, and profile update workers each manage one dimension of player advancement.

| Worker | Task | What It Does |
|---|---|---|
| **AwardXpWorker** | `ppg_award_xp` | Awards XP to the player based on quest completion and returns the new total XP |
| **CheckLevelWorker** | `ppg_check_level` | Checks whether the player's total XP crosses a level threshold and determines if they leveled up |
| **CompleteTaskWorker** | `ppg_complete_task` | Records the player's quest completion and returns the quest name |
| **NotifyWorker** | `ppg_notify` | Sends a notification to the player with their progression summary and unlocked rewards |
| **UnlockRewardsWorker** | `ppg_unlock_rewards` | Unlocks level-up rewards (e.g., Gold Shield, Fire Spell, Title: Dragon Slayer) if the player leveled up |

### The Workflow

```
ppg_complete_task
 │
 ▼
ppg_award_xp
 │
 ▼
ppg_check_level
 │
 ▼
ppg_unlock_rewards
 │
 ▼
ppg_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
