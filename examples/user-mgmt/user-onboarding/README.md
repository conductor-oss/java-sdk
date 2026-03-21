# User Onboarding in Java Using Conductor: Account Creation, Email Verification, Preferences, and Welcome

## The Problem

A user signs up with their name, email, and chosen plan. You need to create their account record, verify the email address before they can use the product, set up sensible defaults for their plan tier, and send a welcome email to get them started. Each step depends on the previous one. Preferences and the welcome email both need the user ID generated during account creation, and you don't want to send a welcome email to an unverified address.

Without orchestration, you'd build a single signup service that calls the user database, fires off a verification email, writes preference rows, and sends the welcome message, all in one method. If the email service is down, the account is created but the user never gets verified. If preferences fail to save, the welcome email goes out anyway pointing to settings that don't exist. Debugging which step failed for a specific user means grepping logs across multiple services.

## The Solution

**You just write the account-creation, email-verification, preferences, and welcome-email workers. Conductor handles the onboarding sequence and user ID threading.**

Each onboarding step: account creation, email verification, preference setup, welcome delivery, is a simple, independent worker. Conductor executes them in the correct sequence, threads the generated user ID from account creation into every downstream step, retries if the email service times out or the database hiccups, and tracks the full onboarding journey for every user. ### What You Write: Workers

CreateAccountWorker generates a user ID, VerifyEmailWorker confirms ownership, SetPreferencesWorker initializes plan-appropriate defaults, and WelcomeWorker sends a personalized getting-started email.

| Worker | Task | What It Does |
|---|---|---|
| **CreateAccountWorker** | `uo_create_account` | Generates a deterministic user ID (USR-{hex} derived from email), persists the account record with email, name, and plan |
| **VerifyEmailWorker** | `uo_verify_email` | Sends an email verification link to the new user's address and confirms delivery |
| **SetPreferencesWorker** | `uo_set_preferences` | Initializes default user preferences: theme (light), language (en), notifications (on), timezone (UTC) |
| **WelcomeWorker** | `uo_welcome` | Sends a personalized welcome email to the verified user with getting-started content |

The demo workers produce realistic, deterministic output shapes so the workflow runs end-to-end with reproducible results. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
uo_create_account
 │
 ▼
uo_verify_email
 │
 ▼
uo_set_preferences
 │
 ▼
uo_welcome

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
