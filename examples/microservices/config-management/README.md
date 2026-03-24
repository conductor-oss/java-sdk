# Config Management in Java with Conductor

Load, validate, deploy, and verify configuration.

## The Problem

Deploying a configuration change across a distributed system requires loading the new config from a source (file, remote store), validating it against a schema, deploying it to all nodes, and verifying consistency. A bad config value pushed without validation can cause service outages across the fleet.

Without orchestration, config deployments are done via ad-hoc scripts that skip validation or push to only some nodes. There is no record of which config version each node is running, and rollbacks require manually reverting and re-pushing the previous config.

## The Solution

**You just write the config load, validation, deployment, and verification workers. Conductor handles ordered execution, crash recovery between deploy and verify, and a full audit trail of every config push.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

The pipeline chains four workers: LoadConfigWorker reads from a config source, ValidateConfigWorker checks it against a schema, DeployConfigWorker pushes it to nodes, and VerifyConfigWorker confirms hash consistency across the fleet.

| Worker | Task | What It Does |
|---|---|---|
| **DeployConfigWorker** | `cf_deploy_config` | Deploys the validated config to nodes in the target environment, returning a deployment ID and node count. |
| **LoadConfigWorker** | `cf_load_config` | Loads configuration from a source (file, remote store) for a target environment, returning the config map and schema version. |
| **ValidateConfigWorker** | `cf_validate_config` | Validates the loaded config against its schema, returning errors and warnings. |
| **VerifyConfigWorker** | `cf_verify_config` | Verifies all nodes are running with the deployed config by checking hash consistency. |

the workflow coordination stays the same.

### The Workflow

```
cf_load_config
 │
 ▼
cf_validate_config
 │
 ▼
cf_deploy_config
 │
 ▼
cf_verify_config

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
