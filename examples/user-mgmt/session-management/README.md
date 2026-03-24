# Session Management

Orchestrates session management through a multi-stage Conductor workflow.

**Input:** `userId`, `deviceInfo` | **Timeout:** 60s

## Pipeline

```
ses_create
    │
ses_validate
    │
ses_refresh
    │
ses_revoke
```

## Workers

**CreateSessionWorker** (`ses_create`)

Reads `userId`. Outputs `sessionId`, `token`, `expiresIn`.

**RefreshSessionWorker** (`ses_refresh`)

Reads `sessionId`. Outputs `refreshed`, `newExpiresIn`.

**RevokeSessionWorker** (`ses_revoke`)

Reads `sessionId`. Outputs `revoked`, `revokedAt`.

**ValidateSessionWorker** (`ses_validate`)

Reads `sessionId`. Outputs `valid`, `remainingTtl`.

## Tests

**13 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
