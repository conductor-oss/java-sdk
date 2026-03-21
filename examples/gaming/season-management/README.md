# Season Management in Java Using Conductor

Manages a competitive season lifecycle: creating the season with a theme, defining reward tiers and battle pass structure, launching to all players, tracking progress, and closing with final reward distribution. ## The Problem

You need to manage a competitive season lifecycle in your game. The workflow creates a new season with a theme and duration, defines the reward tiers and battle pass structure, launches the season to all players, tracks progress and engagement throughout, and closes the season with final reward distribution. Launching without properly defined rewards means players have nothing to earn; not closing properly means lingering rewards and confused players.

Without orchestration, you'd manage seasons through a mix of database scripts, admin panel updates, deployment pipelines, and scheduled jobs. manually coordinating the launch across all platforms (iOS, Android, PC, console), tracking season progress, and ensuring rewards are distributed correctly when the season ends.

## The Solution

**You just write the season creation, reward tier definition, player launch, progress tracking, and final reward distribution logic. Conductor handles reward distribution retries, progress tracking, and season lifecycle audit trails.**

Each season concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (create, define rewards, launch, track, close), retrying if a deployment fails, tracking every season's lifecycle, and resuming from the last step if the process crashes. ### What You Write: Workers

Season creation, reward track setup, progress tracking, and season closure workers handle competitive seasons as discrete lifecycle phases.

| Worker | Task | What It Does |
|---|---|---|
| **CloseWorker** | `smg_close` | Closes the season, distributes final rewards to participants, and returns total player and reward stats |
| **CreateSeasonWorker** | `smg_create_season` | Creates a new season with the given number and theme, and assigns a season ID |
| **DefineRewardsWorker** | `smg_define_rewards` | Defines the reward tiers and unlockable items for the season pass |
| **LaunchWorker** | `smg_launch` | Launches the season to all players and records the launch date |
| **TrackWorker** | `smg_track` | Tracks season progress including active players, average level, top level, and pass holders |

### The Workflow

```
smg_create_season
 │
 ▼
smg_define_rewards
 │
 ▼
smg_launch
 │
 ▼
smg_track
 │
 ▼
smg_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
