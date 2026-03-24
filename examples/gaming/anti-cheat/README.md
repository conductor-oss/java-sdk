# Anti Cheat in Java Using Conductor

Detects and acts on cheating in an online game: monitoring player behavior, running anomaly detection, and routing to clean/suspect/cheat outcomes via a SWITCH task.

## The Problem

You need to detect and act on cheating in an online game. The workflow monitors a player's in-game behavior during a match, runs anomaly detection algorithms on metrics like aim accuracy, movement speed, and reaction time, and routes to one of three outcomes: clean (no action), suspect (flag for manual review), or confirmed cheat (ban or penalty). Failing to detect cheats ruins the experience for honest players; false positives unfairly punish legitimate players.

Without orchestration, you'd build a single anti-cheat service that collects telemetry, runs detection heuristics, makes verdicts, and applies penalties. manually tuning detection thresholds, handling appeals for false bans, and logging every decision to defend against community backlash.

## The Solution

**You just write the behavior monitoring, anomaly detection, and clean/suspect/cheat response logic. Conductor handles detection retries, verdict routing, and enforcement audit trails for every flagged session.**

Each anti-cheat concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of monitoring behavior, running anomaly detection, routing via a SWITCH task to the correct outcome (clean, suspect, cheat), applying the action, and tracking every detection decision with full evidence.

### What You Write: Workers

Telemetry collection, anomaly detection, cheat confirmation, and enforcement workers form an anti-cheat pipeline where each stage applies independent analysis.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `ach_act` | Applies the final enforcement action (ban, warning, or clear) based on the detection verdict |
| **CheatWorker** | `ach_cheat` | Confirms cheating and issues a ban for the flagged player |
| **CleanWorker** | `ach_clean` | Records a clean verdict when no cheating is detected |
| **DetectAnomalyWorker** | `ach_detect_anomaly` | Runs anomaly detection algorithms on player metrics and returns a verdict (clean, suspect, or cheat) |
| **MonitorWorker** | `ach_monitor` | Collects in-game telemetry (aim accuracy, movement speed, reaction time) for a player during a match |
| **SuspectWorker** | `ach_suspect` | Flags a player for manual review by the trust and safety team |

### The Workflow

```
ach_monitor
 │
 ▼
ach_detect_anomaly
 │
 ▼
SWITCH (switch_ref)
 ├── clean: ach_clean
 ├── suspect: ach_suspect
 └── default: ach_cheat
 │
 ▼
ach_act

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
