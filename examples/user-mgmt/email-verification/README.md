# Email Verification

Orchestrates email verification through a multi-stage Conductor workflow.

**Input:** `email`, `userId` | **Timeout:** 60s

## Pipeline

```
emv_send_code
    │
emv_wait_input
    │
emv_verify
    │
emv_activate
```

## Workers

**ActivateAccountWorker** (`emv_activate`): Activates the user account after email verification.

Reads `userId`. Outputs `activated`, `activatedAt`.

**SendCodeWorker** (`emv_send_code`): Sends a verification code to the user's email.

Reads `email`. Outputs `verificationCode`, `sentAt`, `expiresIn`.

**VerifyCodeWorker** (`emv_verify`): Verifies the submitted code against the expected code.

```java
boolean match = submitted != null && submitted.equals(expected);
```

Reads `expectedCode`, `submittedCode`. Outputs `codeMatch`.

**WaitInputWorker** (`emv_wait_input`): perform  waiting for user to submit their verification code.

Reads `expectedCode`. Outputs `submittedCode`.

## Tests

**14 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
