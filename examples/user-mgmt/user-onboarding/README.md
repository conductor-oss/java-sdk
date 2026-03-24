# User Onboarding

Orchestrates user onboarding through a multi-stage Conductor workflow.

**Input:** `username`, `email`, `fullName`, `plan` | **Timeout:** 60s

## Pipeline

```
uo_create_account
    │
uo_verify_email
    │
uo_set_preferences
    │
uo_welcome
```

## Workers

**CreateAccountWorker** (`uo_create_account`): Creates a new user account. Real validation of username, email, and password strength.

- `EMAIL_PATTERN` = `Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")`
- `USERNAME_PATTERN` = `Pattern.compile("^[A-Za-z0-9_]{3,20}$")`

Reads `email`, `username`. Outputs `userId`, `validUsername`, `validEmail`, `createdAt`.
Returns `FAILED` on validation errors.

**SetPreferencesWorker** (`uo_set_preferences`): Sets user preferences with defaults and validation.

Reads `preferences`. Outputs `preferences`, `preferencesSet`.

**VerifyEmailWorker** (`uo_verify_email`): Sends email verification. Real verification code generation.

Reads `email`. Outputs `verificationSent`, `verificationCode`, `emailBody`, `sentAt`.

**WelcomeWorker** (`uo_welcome`): Sends welcome email. Real template generation.

Reads `userId`, `username`. Outputs `welcomeSent`, `emailSubject`, `emailBody`, `sentAt`.

## Tests

**7 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
