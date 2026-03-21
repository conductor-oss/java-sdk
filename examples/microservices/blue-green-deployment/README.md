# Blue Green Deployment in Java with Conductor

Orchestrates blue-green deployment: deploy to green, validate, switch traffic, and monitor. ## The Problem

A zero-downtime deployment requires deploying the new version to a standby environment, validating it with health checks, switching traffic, and monitoring the new version under real load. Each step depends on the previous one, and a failure at any stage must be caught before live traffic is affected.

Without orchestration, these steps are strung together in CI/CD scripts where a failed health check might not prevent the traffic switch, and there is no durable record of which step succeeded or failed. Rolling back means running a separate, equally fragile script.

## The Solution

**You just write the deploy, validate, traffic-switch, and monitor workers. Conductor handles step ordering, durable state across the multi-step cutover, and automatic rollback visibility.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers manage the rollout lifecycle: DeployGreenWorker provisions the new version, ValidateGreenWorker runs smoke tests, SwitchTrafficWorker flips DNS or load-balancer rules, and MonitorGreenWorker watches real-time metrics.

| Worker | Task | What It Does |
|---|---|---|
| **DeployGreenWorker** | `bg_deploy_green` | Deploys the new version to the green environment. |
| **MonitorGreenWorker** | `bg_monitor_green` | Monitors the green environment after traffic switch. |
| **SwitchTrafficWorker** | `bg_switch_traffic` | Switches traffic from blue to green environment. |
| **ValidateGreenWorker** | `bg_validate_green` | Validates the green environment with smoke tests. |

the workflow coordination stays the same.

### The Workflow

```
bg_deploy_green
 │
 ▼
bg_validate_green
 │
 ▼
bg_switch_traffic
 │
 ▼
bg_monitor_green

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
