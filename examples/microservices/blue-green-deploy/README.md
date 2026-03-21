# Blue Green Deploy in Java with Conductor

You need to ship v2.5.0 of the payment service. The old deploy process takes the service down for 90 seconds while the new containers start up, and last time, the health check took longer than expected, so the outage stretched to four minutes during peak checkout hours. Your Slack exploded. You could spin up the new version alongside the old one and flip traffic over, but the last time someone tried that manually, they updated the load balancer target group but forgot to run smoke tests on the new environment first. Traffic shifted to containers that couldn't connect to the database. Rolling back meant another manual LB change under pressure, at 2 AM, with the VP of Engineering watching. This workflow automates blue-green deployment: stand up the new environment, run smoke tests, switch traffic atomically, and verify. with a full audit trail so you know exactly what happened and when. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

Deploying a new version of a service must happen with zero downtime. Blue-green deployment achieves this by standing up the new version in an idle (green) environment, running smoke tests against it, switching live traffic from blue to green, and verifying the switch succeeded, all as an atomic, auditable sequence.

Without orchestration, deployment scripts become fragile shell pipelines where a failure at the traffic-switch step can leave both environments in an inconsistent state. There is no automatic rollback, no execution history, and diagnosing a failed deploy means grepping through CI logs.

## The Solution

**You just write the green-environment, traffic-switch, and verification workers. Conductor handles step sequencing, crash-safe resume during the traffic switch, and a complete deployment audit trail.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. Your workers just make the service calls.

### What You Write: Workers

Three workers carry out the deployment: PrepareGreenWorker stands up the new environment, SwitchTrafficWorker atomically redirects live traffic, and VerifyDeploymentWorker checks error rates and latency after the switch.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareGreenWorker** | `bg_prepare_green` | Deploys the new application version to the green environment and reports container count and image tag. |
| **SwitchTrafficWorker** | `bg_switch_traffic` | Atomically shifts live traffic from blue to green by updating DNS or load-balancer rules. |
| **VerifyDeploymentWorker** | `bg_verify_deployment` | Verifies the deployment succeeded by checking error rate, p99 latency, and rollback availability. |

### The Workflow

```
bg_prepare_green
 │
 ▼
bg_test_green
 │
 ▼
bg_switch_traffic
 │
 ▼
bg_verify_deployment

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
