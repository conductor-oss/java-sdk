# Social Login in Java Using Conductor

## The Problem

A user clicks "Sign in with Google" on your login page. The system needs to detect which OAuth provider was selected, validate the OAuth token against the provider's API to retrieve the user's profile, link the social identity to an existing account or create a new one, and issue a session token so the user can access the application. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the provider-detection, token-validation, account-linking, and session-issuance workers. Conductor handles the OAuth login sequence and identity flow.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

DetectProviderWorker identifies Google/GitHub/Facebook, OAuthWorker validates the token and retrieves the profile, LinkAccountWorker connects the social identity, and CreateSessionWorker issues a session token.

| Worker | Task | What It Does |
|---|---|---|
| **CreateSessionWorker** | `slo_session` | Issues a session token for the authenticated user with a configurable expiration |
| **DetectProviderWorker** | `slo_detect_provider` | Identifies the OAuth provider (Google, GitHub, Facebook) and resolves its authentication endpoint |
| **LinkAccountWorker** | `slo_link_account` | Links the social identity to an existing user account or creates a new account, tracking linked providers |
| **OAuthWorker** | `slo_auth` | Validates the OAuth token against the provider's API and retrieves the user's profile (name, avatar) |

Replace with real identity provider and database calls and ### The Workflow

```
slo_detect_provider
 │
 ▼
slo_auth
 │
 ▼
slo_link_account
 │
 ▼
slo_session

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
