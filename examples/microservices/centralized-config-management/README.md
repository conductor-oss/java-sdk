# Centralized Config Management in Java with Conductor

Centralized config management with staged rollout.

## The Problem

Changing a configuration value across a fleet of microservices is error-prone. Each service may read config from a different source, and applying an invalid value can cause cascading failures. Centralized config management validates changes, plans a staged rollout (canary -> 25% -> 100%), applies the config, and verifies all services are running with the updated value.

Without orchestration, config changes are pushed ad-hoc via scripts or manual kubectl commands, with no validation gate and no way to know whether all services actually picked up the new value. Rolling back a bad config change requires another manual push.

## The Solution

**You just write the config validation, rollout planning, and config-apply workers. Conductor handles validation gating, staged execution, and a durable record of every config change across the fleet.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers handle the config lifecycle: CfgValidateWorker checks the key-value pair against schema rules, CfgStageRolloutWorker plans a graduated rollout, CfgApplyWorker pushes the change, and CfgVerifyWorker confirms fleet-wide consistency.

| Worker | Task | What It Does |
|---|---|---|
| **CfgApplyWorker** | `cfg_apply_config` | Applies the validated config to target services following the rollout plan. |
| **CfgStageRolloutWorker** | `cfg_stage_rollout` | Creates a staged rollout plan (canary, 25%, 100%) with intervals between stages. |
| **CfgValidateWorker** | `cfg_validate` | Validates the config key/value pair against schema rules and type constraints. |
| **CfgVerifyWorker** | `cfg_verify` | Verifies all services are running with the updated config and are healthy. |

the workflow coordination stays the same.

### The Workflow

```
cfg_validate
 │
 ▼
cfg_stage_rollout
 │
 ▼
cfg_apply_config
 │
 ▼
cfg_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
