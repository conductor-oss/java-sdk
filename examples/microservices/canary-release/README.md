# Canary Release in Java with Conductor

Canary release with progressive traffic increase.

## The Problem

A canary release progressively increases traffic to a new version in stages (e.g., 5% -> 50% -> 100%), monitoring health at each stage before proceeding. If anomalies are detected at any stage, the release is halted and traffic stays on the stable version.

Without orchestration, multi-stage rollouts require chaining CI/CD jobs with manual gates between stages, and there is no durable record of which stage was active when an issue occurred. Restarting a failed rollout from the correct stage requires manual intervention.

## The Solution

**You just write the canary deploy, monitor, traffic-increase, and rollout workers. Conductor handles multi-stage progression, durable pause between stages, and crash-safe recovery mid-rollout.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

This progressive release uses four workers: DeployCanaryWorker starts the canary at an initial traffic percentage, MonitorCanaryWorker watches for anomalies, IncreaseTrafficWorker bumps the percentage, and FullRolloutWorker promotes to 100%.

| Worker | Task | What It Does |
|---|---|---|
| **DeployCanaryWorker** | `cy_deploy_canary` | Deploys the new version at an initial traffic percentage (e.g., 5%). |
| **FullRolloutWorker** | `cy_full_rollout` | Promotes the canary to 100% traffic, completing the release. |
| **IncreaseTrafficWorker** | `cy_increase_traffic` | Increases the canary traffic percentage to the next stage (e.g., 50%) and scales up canary instances. |
| **MonitorCanaryWorker** | `cy_monitor_canary` | Monitors error rate, p99 latency, and anomalies during a configured monitoring window. |

the workflow coordination stays the same.

### The Workflow

```
cy_deploy_canary
 │
 ▼
cy_monitor_canary
 │
 ▼
cy_increase_traffic
 │
 ▼
cy_monitor_canary
 │
 ▼
cy_full_rollout

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
