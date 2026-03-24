# Milestone Tracking

Milestone tracking with SWITCH for on_track/at_risk/delayed.

**Input:** `milestoneId`, `projectName` | **Timeout:** 60s

## Pipeline

```
mst_check_progress
    │
mst_evaluate
    │
route_status [SWITCH]
  ├─ on_track: mst_on_track
  ├─ at_risk: mst_at_risk
  └─ default: mst_delayed
    │
mst_act
```

## Workers

**ActWorker** (`mst_act`)

Reads `milestoneId`, `status`. Outputs `action`.

**AtRiskWorker** (`mst_at_risk`)

Reads `milestoneId`. Outputs `action`.

**CheckProgressWorker** (`mst_check_progress`)

Reads `milestoneId`. Outputs `progress`.

**DelayedWorker** (`mst_delayed`)

Reads `milestoneId`. Outputs `action`.

**EvaluateWorker** (`mst_evaluate`)

Outputs `status`, `pctDone`.

**OnTrackWorker** (`mst_on_track`)

Reads `milestoneId`. Outputs `action`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
