# Legal Case Management

Orchestrates legal case management through a multi-stage Conductor workflow.

**Input:** `clientId`, `caseType`, `description` | **Timeout:** 60s

## Pipeline

```
lcm_intake
    │
lcm_assess
    │
lcm_assign
    │
lcm_track
    │
lcm_close
```

## Workers

**AssessWorker** (`lcm_assess`)

Outputs `complexity`.

**AssignWorker** (`lcm_assign`)

Outputs `attorneyId`.

**CloseWorker** (`lcm_close`)

Outputs `outcome`.

**IntakeWorker** (`lcm_intake`)

Outputs `caseId`.

**TrackWorker** (`lcm_track`)

Outputs `phase`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
