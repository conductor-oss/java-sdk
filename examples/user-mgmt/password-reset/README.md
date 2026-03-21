# Password Reset Workflow in Java Using Conductor

User clicks "Reset Password." The email takes eight minutes because your SMTP relay is backed up. They click "Reset" again. Now two tokens are live. The first email arrives, they click it, but that token expired after five minutes. Locked out. They try the second link: it works, but the password update succeeds while the confirmation email fails, so they don't know the reset went through and submit a third request. Support gets a ticket from a frustrated user who "can't log in" with a trail of three tokens, two expired, one used, and no audit log of what happened. This example orchestrates the password reset flow with Conductor: account lookup, token validation, credential update, and confirmation notification, each step sequenced, retriable, and fully auditable. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the identity logic, Conductor handles retries, failure routing, durability, and observability.

## The Forgot-Password Flow

A user clicks "Forgot Password" and enters their email. The system needs to look up the account, validate the reset token (checking it was issued for this user and hasn't expired), update the credential store with the new password hash, and send a confirmation email, all in the correct order, with no step skipped. If the token is valid but the password update fails, the user is stuck: the token may be consumed but the password unchanged. If the notification fails, the user doesn't know the reset succeeded and submits another request.

Without orchestration, you'd chain all of this in a single servlet or controller method. Catching exceptions at each step, manually rolling back on failure, and hoping the email service doesn't time out while you're holding a database transaction open. That code becomes brittle, hard to test, and impossible to audit when security reviews ask "show me every reset that happened last month."

## The Solution

**You just write the account-lookup, token-validation, password-update, and confirmation workers. Conductor handles the secure reset sequence and retry logic.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

RequestWorker looks up the account by email, VerifyTokenWorker validates the reset token, ResetWorker updates the password hash, and NotifyWorker sends a confirmation email, each handles one step of the secure reset flow.

| Worker | Task | What It Does |
|---|---|---|
| `RequestWorker` | `pwd_request` | Looks up the user account by email and returns the user ID (`USR-A1B2C3`) and a timestamp |
| `VerifyTokenWorker` | `pwd_verify` | Validates the reset token against the user ID, confirming it is valid with 900 seconds remaining |
| `ResetWorker` | `pwd_reset` | Updates the user's password in the credential store and records the update timestamp |
| `NotifyWorker` | `pwd_notify` | Sends a password-change confirmation email to the user's address |

### The Workflow

```
pwd_request
 |
 v
pwd_verify
 |
 v
pwd_reset
 |
 v
pwd_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
