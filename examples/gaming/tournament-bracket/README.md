# Tournament Bracket

Orchestrates tournament bracket through a multi-stage Conductor workflow.

**Input:** `tournamentName`, `format`, `maxPlayers` | **Timeout:** 60s

## Pipeline

```
tbk_register
    │
tbk_seed
    │
tbk_create_bracket
    │
tbk_manage_rounds
    │
tbk_finalize
```

## Workers

**CreateBracketWorker** (`tbk_create_bracket`)

Reads `format`. Outputs `bracketId`, `rounds`, `matches`.

**FinalizeWorker** (`tbk_finalize`)

Reads `bracketId`. Outputs `tournament`.

**ManageRoundsWorker** (`tbk_manage_rounds`)

Reads `bracketId`. Outputs `results`.

**RegisterWorker** (`tbk_register`)

Reads `tournamentName`. Outputs `players`, `count`.

**SeedWorker** (`tbk_seed`)

Outputs `seeded`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
