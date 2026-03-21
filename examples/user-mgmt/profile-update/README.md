# Profile Update in Java Using Conductor

## The Problem

A user changes their display name and email address in their account settings. The system needs to validate the submitted fields for format and constraints, apply the updates to the user's profile record, sync the changed fields to downstream services (CRM, analytics, email platform), and notify the user that their profile was updated. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the field-validation, profile-update, downstream-sync, and notification workers. Conductor handles the update pipeline and cross-service propagation.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

ValidateFieldsWorker checks format constraints, UpdateProfileWorker applies the changes, SyncProfileWorker propagates to CRM and analytics, and NotifyChangesWorker confirms the update to the user.

| Worker | Task | What It Does |
|---|---|---|
| **NotifyChangesWorker** | `pfu_notify` | Sends the user a notification confirming which profile fields were changed |
| **SyncProfileWorker** | `pfu_sync` | Propagates the updated profile fields to 3 downstream services: CRM, analytics, and email |
| **UpdateProfileWorker** | `pfu_update` | Applies the validated field changes to the user's profile record and timestamps the update |
| **ValidateFieldsWorker** | `pfu_validate` | Validates the submitted profile fields for format and constraints, returning whether all fields passed |

Replace with real identity provider and database calls and ### The Workflow

```
pfu_validate
 │
 ▼
pfu_update
 │
 ▼
pfu_sync
 │
 ▼
pfu_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
