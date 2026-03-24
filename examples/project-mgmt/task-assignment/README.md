# Task Assignment

Task assignment: analyze, match skills, assign, notify, track.

**Input:** `taskTitle`, `requiredSkills`, `priority` | **Timeout:** 60s

## Pipeline

```
tas_analyze
    │
tas_match_skills
    │
tas_assign
    │
tas_notify
    │
tas_track
```

## Workers

**AnalyzeWorker** (`tas_analyze`)

Reads `taskTitle`. Outputs `skills`, `complexity`.

**AssignWorker** (`tas_assign`)

Outputs `assignee`, `assigned`.

**MatchSkillsWorker** (`tas_match_skills`)

Outputs `bestMatch`.

**NotifyWorker** (`tas_notify`)

Reads `assignee`. Outputs `notified`, `channel`.

**TrackWorker** (`tas_track`)

Reads `assignee`. Outputs `tracking`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
