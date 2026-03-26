# User Registration

Orchestrates user registration through a multi-stage Conductor workflow.

**Input:** `username`, `email` | **Timeout:** 60s

## Pipeline

```
ur_validate
    │
ur_create
    │
ur_confirm
    │
ur_activate
```

## Workers

**ActivateWorker** (`ur_activate`): Activates user account.

Reads `userId`. Outputs `active`, `activatedAt`.

**ConfirmWorker** (`ur_confirm`): Sends confirmation email.

Reads `email`. Outputs `confirmationSent`, `token`.

**CreateWorker** (`ur_create`): Creates the user record.

Reads `username`. Outputs `userId`, `username`, `createdAt`.

**ValidateWorker** (`ur_validate`): Validates username and email format.

```java
boolean valid = username != null && username.length() >= 3
```

Reads `email`, `username`. Outputs `valid`, `checks`.

## Tests

**15 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
