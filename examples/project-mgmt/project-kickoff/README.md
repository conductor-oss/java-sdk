# Project Kickoff

Project kickoff: define scope, assign team, create plan, kick off.

**Input:** `projectName`, `sponsor`, `budget` | **Timeout:** 60s

## Pipeline

```
pkf_define_scope
    │
pkf_assign_team
    │
pkf_create_plan
    │
pkf_kick_off
```

## Workers

**AssignTeamWorker** (`pkf_assign_team`)

Reads `projectName`. Outputs `team`.

**CreatePlanWorker** (`pkf_create_plan`)

Outputs `plan`.

**DefineScopeWorker** (`pkf_define_scope`)

Reads `projectName`. Outputs `scope`.

**KickOffWorker** (`pkf_kick_off`)

Reads `projectName`. Outputs `project`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
