# Multi Factor Auth in Java Using Conductor

## The Problem

You need to authenticate a user with multi-factor authentication. Validating their username and password as the primary factor, determining which second factor to use (TOTP app, SMS code, hardware key) based on their preference and enrollment, verifying the second factor, and granting access only if both factors succeed. Each step depends on the previous one's output.

If primary auth succeeds but the second factor method selection picks a method the user hasn't enrolled in, the login fails with a confusing error. If factor verification succeeds but the access grant step fails to issue the session token, the user proved their identity but can't get in. Without orchestration, you'd build a monolithic login handler that mixes password hashing, TOTP validation, SMS delivery, and session management. Making it impossible to add new factor types (passkeys, biometrics), enforce step-up authentication for sensitive operations, or track which authentication methods are most reliable for your user base.

## The Solution

**You just write the primary-auth, factor-selection, factor-verification, and access-grant workers. Conductor handles the MFA sequence and factor-method routing.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

PrimaryAuthWorker validates credentials, SelectMethodWorker picks TOTP/SMS/email based on enrollment, VerifyFactorWorker checks the second-factor code, and GrantAccessWorker issues a session token.

| Worker | Task | What It Does |
|---|---|---|
| **GrantAccessWorker** | `mfa_grant` | Issues a session token and grants access to the authenticated user with a configurable expiration |
| **PrimaryAuthWorker** | `mfa_primary_auth` | Validates the user's username and password, returning a user ID on success |
| **SelectMethodWorker** | `mfa_select_method` | Determines the second factor method (TOTP, SMS, or email) based on user preference and available options |
| **VerifyFactorWorker** | `mfa_verify_factor` | Verifies the submitted second-factor code against the selected method, tracking attempt count |

Replace with real identity provider and database calls and ### The Workflow

```
mfa_primary_auth
 │
 ▼
mfa_select_method
 │
 ▼
mfa_verify_factor
 │
 ▼
mfa_grant

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
