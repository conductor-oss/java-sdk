# Social Login

Orchestrates social login through a multi-stage Conductor workflow.

**Input:** `provider`, `email` | **Timeout:** 60s

## Pipeline

```
slo_detect_provider
    │
slo_auth
    │
slo_link_account
    │
slo_session
```

## Workers

**CreateSessionWorker** (`slo_session`)

Reads `userId`. Outputs `sessionToken`, `expiresIn`.

**DetectProviderWorker** (`slo_detect_provider`)

Reads `provider`. Outputs `providerName`, `authEndpoint`, `supported`.

**LinkAccountWorker** (`slo_link_account`)

Reads `provider`. Outputs `userId`, `isNewUser`, `linkedProviders`.

**OAuthWorker** (`slo_auth`)

Reads `provider`. Outputs `providerUserId`, `profile`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
