# Canary Deployment in Java with Conductor

You merge the PR, CI goes green, and you deploy to prod. Two minutes later, 100% of your users are hitting the new code, and the new code has a subtle bug that doubles response latency on the `/checkout` endpoint. By the time your on-call notices the Datadog alert and rolls back, 40,000 users have experienced broken checkouts. The postmortem is always the same: "we should have tested with a small percentage of traffic first." But doing that manually: deploying a canary, shifting 5% of traffic, watching error rates, deciding whether to promote or rollback, is tedious and error-prone, so teams skip it. This workflow automates the entire canary lifecycle: deploy, shift traffic gradually, analyze metrics, and promote or roll back automatically. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

Rolling out a new service version to 100% of traffic at once is risky, a bug can affect all users instantly. Canary deployment mitigates this by deploying the new version alongside the old one, gradually shifting a percentage of traffic to the canary, analyzing error rates and latency, and then promoting or rolling back based on the results.

Without orchestration, the deploy-shift-analyze-decide pipeline is typically a set of loosely coupled CI/CD steps with no unified state. If the analysis step fails, the traffic shift may not be reverted, and there is no single place to see the full canary lifecycle.

## The Solution

**You just write the deploy, traffic-shift, metrics-analysis, and promote-or-rollback workers. Conductor handles staged execution, durable state between deploy and promote, and a full audit of every canary decision.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

Four workers drive the canary lifecycle: DeployCanaryWorker provisions the new version, ShiftTrafficWorker steers a percentage of requests, AnalyzeMetricsWorker evaluates error rates, and PromoteOrRollbackWorker makes the final call.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeMetricsWorker** | `cd_analyze_metrics` | Collects and analyzes error rate and p99 latency from the canary during a monitoring window. |
| **DeployCanaryWorker** | `cd_deploy_canary` | Deploys the new service version as a canary instance alongside the stable version. |
| **PromoteOrRollbackWorker** | `cd_promote_or_rollback` | Compares the canary error rate against a threshold and decides whether to promote to full rollout or roll back. |
| **ShiftTrafficWorker** | `cd_shift_traffic` | Shifts a specified percentage of live traffic to the canary instances. |

### The Workflow

```
cd_deploy_canary
 │
 ▼
cd_shift_traffic
 │
 ▼
cd_analyze_metrics
 │
 ▼
cd_promote_or_rollback

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
