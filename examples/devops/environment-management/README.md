# Environment Management in Java with Conductor : Create, Configure, Seed Data, Verify

Environment lifecycle orchestration: create, configure, seed data, and verify. ## Developers Need Environments on Demand

"I need a staging environment for my feature branch" shouldn't take a week of DevOps tickets. Ephemeral environments should spin up in minutes: provision infrastructure from a template (Kubernetes namespace, database, message queue), configure the environment with the right service versions and feature flags, seed it with realistic test data, and run health checks to confirm everything is ready.

Environments have a TTL. they're destroyed automatically after the specified hours to control costs. If the configuration step fails (wrong service version, missing secret), the environment is created but unusable, retrying should reconfigure, not recreate. And every environment must be tracked: who created it, which branch, when it was created, and when it was destroyed.

## The Solution

**You write the provisioning and configuration logic. Conductor handles create-configure-seed-verify sequencing, TTL-based teardown, and environment lifecycle tracking.**

`CreateEnvWorker` provisions a new environment from the specified template. Kubernetes namespace, database instance, and supporting services. `ConfigureWorker` sets up service versions, environment variables, feature flags, and secrets. `SeedDataWorker` populates the database with test data appropriate for the environment's purpose. `VerifyWorker` runs health checks against all services in the environment and confirms end-to-end functionality. Conductor records the environment lifecycle and can trigger automatic teardown after the TTL expires.

### What You Write: Workers

Four workers manage the environment lifecycle. Creating infrastructure, configuring services, seeding test data, and verifying health.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureEnv** | `em_configure` | Configures the environment. |
| **CreateEnv** | `em_create_env` | Creates a new environment. |
| **SeedData** | `em_seed_data` | Seeds test data into the environment. |
| **VerifyEnv** | `em_verify` | Verifies the environment is healthy. |

the workflow and rollback logic stay the same.

### The Workflow

```
em_create_env
 │
 ▼
em_configure
 │
 ▼
em_seed_data
 │
 ▼
em_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
