# Anti Cheat

Orchestrates anti cheat through a multi-stage Conductor workflow.

**Input:** `playerId`, `matchId` | **Timeout:** 60s

## Pipeline

```
ach_monitor
    │
ach_detect_anomaly
    │
route_verdict [SWITCH]
  ├─ clean: ach_clean
  ├─ suspect: ach_suspect
  └─ default: ach_cheat
    │
ach_act
```

## Workers

**ActWorker** (`ach_act`)

Reads `playerId`, `verdict`. Outputs `action`.

**CheatWorker** (`ach_cheat`)

Reads `playerId`. Outputs `result`.

**CleanWorker** (`ach_clean`)

Reads `playerId`. Outputs `result`.

**DetectAnomalyWorker** (`ach_detect_anomaly`)

```java
String verdict = aimAccuracy > 0.9 ? "cheat" : aimAccuracy > 0.7 ? "suspect" : "clean";
```

Reads `metrics`. Outputs `verdict`, `confidence`, `flags`, `evidence`.

**MonitorWorker** (`ach_monitor`)

Reads `matchId`, `playerId`. Outputs `metrics`.

**SuspectWorker** (`ach_suspect`)

Reads `playerId`. Outputs `result`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
