# Feature Flags in Java with Conductor

Route execution based on feature flag status. ## The Problem

Feature flags let you route users to different code paths (new feature vs legacy) without redeploying. This workflow checks a flag's status for a specific user, routes execution to either the new feature path or the legacy path based on the result, and logs the flag usage for analytics. If the flag status is unknown, a safe default path is used.

Without orchestration, feature flag checks are scattered across application code with if/else blocks, making it hard to see which flags are active, how many users are on each path, and whether the new feature is performing better than the legacy one.

## The Solution

**You just write the flag-check, new-feature, legacy-path, and usage-logging workers. Conductor handles conditional path routing via SWITCH, per-evaluation retries, and usage tracking for every flag decision.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Five workers handle flag evaluation and routing: CheckFlagWorker evaluates flag status, NewFeatureWorker and LegacyPathWorker implement the two code paths, DefaultPathWorker serves as a safe fallback, and LogUsageWorker records analytics.

| Worker | Task | What It Does |
|---|---|---|
| **CheckFlagWorker** | `ff_check_flag` | Evaluates a feature flag for a specific user and returns the flag status (enabled/disabled) and rollout percentage. |
| **DefaultPathWorker** | `ff_default_path` | Executes a safe default path when the flag status is unknown. |
| **LegacyPathWorker** | `ff_legacy_path` | Executes the legacy code path when the flag is disabled (e.g., renders v1 UI). |
| **LogUsageWorker** | `ff_log_usage` | Logs the flag evaluation result for analytics and A/B test tracking. |
| **NewFeatureWorker** | `ff_new_feature` | Executes the new feature code path when the flag is enabled (e.g., renders v2 UI). |

the workflow coordination stays the same.

### The Workflow

```
ff_check_flag
 │
 ▼
SWITCH (ff_switch_ref)
 ├── enabled: ff_new_feature
 ├── disabled: ff_legacy_path
 └── default: ff_default_path
 │
 ▼
ff_log_usage

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
