# Multi-Region Deploy in Java with Conductor : Deploy Primary, Verify, Deploy Secondary, Verify Global

Automates multi-region deployments using [Conductor](https://github.com/conductor-oss/conductor). This workflow deploys a service to the primary region first, verifies it is healthy, then rolls out to secondary regions, and runs a global verification to confirm all regions are serving traffic correctly.

## Deploying Across Regions Safely

You run your payment service in us-east-1, eu-west-1, and ap-southeast-1. A new version needs to go out, but deploying to all three regions simultaneously is risky. If the release has a bug, all regions go down at once. The safe approach: deploy to the primary region first, verify it is healthy, then fan out to the secondary regions, and finally run a global check to confirm every region is serving traffic. If the primary deployment fails health checks, the secondary regions never get the bad version.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the regional deploy and verification logic. Conductor handles primary-first sequencing, health gates before secondary rollout, and cross-region consistency checks.**

`DeployPrimaryWorker` deploys the new version to the primary region with canary or blue-green strategy. `VerifyPrimaryWorker` monitors the primary region for a stabilization period. checking error rates, latency, and business metrics against baselines. `DeploySecondaryWorker` deploys to all remaining regions once the primary is verified healthy. `VerifyGlobalWorker` confirms cross-region consistency, same version running everywhere, inter-region traffic routing correctly, and global health checks passing. Conductor sequences these steps, halting secondary deployment if primary verification fails.

### What You Write: Workers

Four workers manage the multi-region rollout. Deploying to the primary region first, verifying health, then fanning out to secondary regions, and running a global check.

| Worker | Task | What It Does |
|---|---|---|
| **DeployPrimaryWorker** | `mrd_deploy_primary` | Deploys service to the primary region. |
| **DeploySecondaryWorker** | `mrd_deploy_secondary` | Deploys service to secondary regions. |
| **VerifyGlobalWorker** | `mrd_verify_global` | Verifies all regions are healthy and serving traffic. |
| **VerifyPrimaryWorker** | `mrd_verify_primary` | Verifies health of the primary region deployment. |

the workflow and rollback logic stay the same.

### The Workflow

```
mrd_deploy_primary
 │
 ▼
mrd_verify_primary
 │
 ▼
mrd_deploy_secondary
 │
 ▼
mrd_verify_global

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
