# Multi Factor Auth

Orchestrates multi factor auth through a multi-stage Conductor workflow.

**Input:** `username`, `preferredMethod` | **Timeout:** 60s

## Pipeline

```
mfa_primary_auth
    │
mfa_select_method
    │
mfa_verify_factor
    │
mfa_grant
```

## Workers

**GrantAccessWorker** (`mfa_grant`)

Reads `userId`. Outputs `accessGranted`, `token`, `expiresIn`.

**PrimaryAuthWorker** (`mfa_primary_auth`)

Reads `username`. Outputs `userId`, `primaryPassed`.

**SelectMethodWorker** (`mfa_select_method`)

Reads `preferred`. Outputs `selectedMethod`, `available`.

**VerifyFactorWorker** (`mfa_verify_factor`)

Reads `method`, `userId`. Outputs `verified`, `attempts`.

## Tests

**14 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
