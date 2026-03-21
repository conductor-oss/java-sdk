# Notification Preferences in Java Using Conductor

## The Problem

A user wants to change their notification settings. Enabling SMS alerts and disabling push notifications. The system needs to load their current preferences (email, SMS, push, Slack), merge the new selections with existing ones, sync the updated channel configuration to all downstream notification services, and send a confirmation that the changes took effect. Each step depends on the previous one's output.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the preference-loading, merging, channel-sync, and confirmation workers. Conductor handles the preference update pipeline and multi-service propagation.**

Each worker handles one user lifecycle step. Conductor manages the onboarding sequence, verification wait states, timeout escalation, and user state tracking.

### What You Write: Workers

LoadPrefsWorker reads current channel settings, UpdatePrefsWorker merges new selections, SyncChannelsWorker configures each notification service, and ConfirmPrefsWorker notifies the user that changes took effect.

| Worker | Task | What It Does |
|---|---|---|
| **ConfirmPrefsWorker** | `np_confirm` | Sends a confirmation notification to the user that their preference changes took effect |
| **LoadPrefsWorker** | `np_load` | Loads the user's current notification preferences across all channels (email, SMS, push, Slack) |
| **SyncChannelsWorker** | `np_sync_channels` | Identifies which channels are now active and syncs the configuration to each notification service |
| **UpdatePrefsWorker** | `np_update` | Merges the new preference selections with existing ones and persists the updated configuration |

Replace with real identity provider and database calls and ### The Workflow

```
np_load
 │
 ▼
np_update
 │
 ▼
np_sync_channels
 │
 ▼
np_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
