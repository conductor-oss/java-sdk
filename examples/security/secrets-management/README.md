# Implementing Secrets Management in Java with Conductor: Create, Distribute, Verify Access, and Schedule Rotation

An engineer who left the company six weeks ago still has working API keys. You know this because one of those keys is hardcoded in an environment variable on three production servers, pasted into a `.env` file in a private repo that four people have access to, and shared in a Slack DM from last March. Nobody rotated it when she left because nobody knew where it was used. The database password for the payments service hasn't been rotated in fourteen months, and the only record of who has access is a mental model in the head of a senior dev who's on vacation. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate secrets lifecycle management: create credentials, distribute to authorized consumers only, verify access policies, and schedule automatic rotation, with a full audit trail of every secret touched.

## The Problem

You need to manage secrets across your infrastructure. API keys, database passwords, TLS certificates. Each secret must be created securely, distributed only to authorized services, access controls must be verified (no unauthorized consumers), and rotation must be scheduled before expiry. If distribution fails, services can't start. If access verification is skipped, unauthorized services gain access. If rotation isn't scheduled, secrets expire and cause outages.

Without orchestration, secrets management is a manual process. Someone creates a secret in Vault, copies it to environment variables, hopes the right services have access, and forgets to schedule rotation. Secrets are shared via Slack, stored in plaintext config files, and never rotated.

## The Solution

**You just write the vault integration and rotation logic. Conductor handles sequencing, retries on vault failures, and a full audit trail of every secret created and distributed.**

Each secrets concern is an independent worker. Secret creation, distribution to consumers, access verification, and rotation scheduling. Conductor runs them in sequence: create the secret, distribute to authorized consumers, verify that only the right services have access, then schedule rotation. Every operation is tracked for audit compliance.

### What You Write: Workers

Four workers handle the secrets lifecycle: CreateSecretWorker generates credentials, DistributeWorker pushes them to authorized consumers, VerifyAccessWorker confirms access policies, and ScheduleRotationWorker sets up automatic renewal.

| Worker | Task | What It Does |
|---|---|---|
| **CreateSecretWorker** | `sm_create_secret` | Creates a new secret (API key, database credential, or certificate) and returns a secret ID |
| **DistributeWorker** | `sm_distribute` | Distributes the secret to authorized services and confirms delivery |
| **ScheduleRotationWorker** | `sm_schedule_rotation` | Schedules automatic rotation on a configurable interval (e.g., every 90 days) |
| **VerifyAccessWorker** | `sm_verify_access` | Verifies that access policies are correct and only authorized services can read the secret |

### The Workflow

```
sm_create_secret
 │
 ▼
sm_distribute
 │
 ▼
sm_verify_access
 │
 ▼
sm_schedule_rotation

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
