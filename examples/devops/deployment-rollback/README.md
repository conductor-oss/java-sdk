# Deployment Rollback in Java with Conductor : Detect Failure, Identify Version, Rollback, Verify

Automates deployment rollback using [Conductor](https://github.com/conductor-oss/conductor). This workflow detects a failing deployment (error rate spikes, health check failures), identifies the last known stable version, rolls back the deployment to that version, and verifies the service is healthy again.

## When a Deploy Goes Wrong

Your checkout-service just deployed version 2.5.0 and the error rate is spiking. Customers are seeing 500 errors. You need to act fast: confirm the deployment is actually failing (not just a transient blip), find the last stable version (2.4.3, deployed 3 days ago), roll back to it, and verify the error rate normalizes. Every minute of delay means lost revenue and frustrated users.

Without orchestration, someone notices the error rate in Grafana, runs `kubectl rollout undo` by hand, forgets which revision to target, picks the wrong one, and the service is now running a version from two weeks ago with a known bug. If the rollback itself fails halfway: pods stuck in CrashLoopBackOff, there's no automated verification, no structured record of what was rolled back or why, and no confidence that the service is actually healthy again.

## The Solution

**You write the failure detection and rollback logic. Conductor handles the detect-identify-rollback-verify sequence and records the complete rollback timeline.**

`DetectFailureWorker` analyzes deployment health signals. error rates, latency percentiles, health check status, to confirm the deployment is failing and quantify the impact. `IdentifyVersionWorker` looks up the deployment history to find the last known-good version and its configuration. `RollbackDeployWorker` executes the rollback, redeploying the previous version with its original configuration. `VerifyRollbackWorker` monitors health signals post-rollback to confirm the service has recovered, checking error rates, latency, and health check status. Conductor sequences these steps and records the complete rollback timeline for incident review.

### What You Write: Workers

Four workers execute the rollback. Detecting deployment failures, identifying the last stable version, rolling back, and verifying recovery.

| Worker | Task | What It Does |
|---|---|---|
| **DetectFailureWorker** | `rb_detect_failure` | Detects deployment failures by checking error rate spikes, health check status, and anomaly signals |
| **IdentifyVersionWorker** | `rb_identify_version` | Looks up the last known stable version and its deployment timestamp from release history |
| **RollbackDeployWorker** | `rb_rollback_deploy` | Rolls back the deployment to the identified stable version |
| **VerifyRollbackWorker** | `rb_verify_rollback` | Verifies the service is healthy after rollback by checking error rates and health endpoints |

the workflow and rollback logic stay the same.

### The Workflow

```
rb_detect_failure
 │
 ▼
rb_identify_version
 │
 ▼
rb_rollback_deploy
 │
 ▼
rb_verify_rollback

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
