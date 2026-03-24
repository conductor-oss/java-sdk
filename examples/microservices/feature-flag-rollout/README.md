# Feature Flag Rollout in Java with Conductor

Manages feature flag lifecycle: create flag, staged rollout, monitor impact, and full activation or rollback.

## The Problem

Rolling out a feature flag involves creating the flag, enabling it for a target user segment at a specified percentage, monitoring its impact on key metrics (conversion rate, error rate), and then deciding whether to fully activate or roll back. Each step depends on the previous one. You cannot monitor impact until the flag is enabled for a segment.

Without orchestration, feature flag rollouts are ad-hoc: an engineer creates a flag in LaunchDarkly or a config file, manually checks dashboards after a while, and makes a gut-call about full activation. There is no structured lifecycle, no automatic monitoring window, and no rollback trigger.

## The Solution

**You just write the flag creation, segment targeting, impact monitoring, and rollout workers. Conductor handles staged flag lifecycle execution, durable monitoring windows, and a complete rollout audit trail.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers manage the flag lifecycle: CreateFlagWorker provisions the flag, EnableSegmentWorker targets a user segment, MonitorImpactWorker measures conversion and error rates, and FullRolloutWorker promotes to 100% or rolls back.

| Worker | Task | What It Does |
|---|---|---|
| **CreateFlagWorker** | `ff_create_flag` | Creates a new feature flag in the flag management system and returns its ID. |
| **EnableSegmentWorker** | `ff_enable_segment` | Enables the flag for a target user segment at a specified rollout percentage. |
| **FullRolloutWorker** | `ff_full_rollout` | Fully activates the flag for all users if monitoring shows healthy metrics, or rolls back if not. |
| **MonitorImpactWorker** | `ff_monitor_impact` | Monitors the flag's impact on conversion rate and error rate during a timed window. |

the workflow coordination stays the same.

### The Workflow

```
ff_create_flag
 │
 ▼
ff_enable_segment
 │
 ▼
ff_monitor_impact
 │
 ▼
ff_full_rollout

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
