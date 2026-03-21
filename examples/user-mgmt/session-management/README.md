# Session Management in Java Using Conductor

## The Problem

A user logs in and the system must manage their session lifecycle end-to-end. The system needs to create a new session with a JWT token and expiration, validate that the session is still active and has sufficient remaining TTL, refresh the session by extending its expiration before it lapses, and revoke the session when the user logs out or the session is terminated. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the session-creation, validation, refresh, and revocation workers. Conductor handles the session lifecycle sequencing and token data flow.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

CreateSessionWorker generates a JWT with expiration, ValidateSessionWorker checks remaining TTL, RefreshSessionWorker extends the expiration, and RevokeSessionWorker invalidates the token on logout.

| Worker | Task | What It Does |
|---|---|---|
| **CreateSessionWorker** | `ses_create` | Creates a new session for the user, generating a unique session ID, JWT token, and expiration time |
| **RefreshSessionWorker** | `ses_refresh` | Extends the session's expiration by issuing a new TTL, keeping the user logged in |
| **RevokeSessionWorker** | `ses_revoke` | Revokes the session by invalidating the token and recording the revocation timestamp |
| **ValidateSessionWorker** | `ses_validate` | Checks that the session is still valid and returns the remaining TTL |

Replace with real identity provider and database calls and ### The Workflow

```
ses_create
 │
 ▼
ses_validate
 │
 ▼
ses_refresh
 │
 ▼
ses_revoke

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
