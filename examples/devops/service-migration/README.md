# Service Migration in Java with Conductor

Orchestrates a service migration between environments using [Conductor](https://github.com/conductor-oss/conductor). This workflow assesses dependencies, replicates the service to the target environment, performs the traffic cutover, and validates that all endpoints are responding correctly.

## Moving Services Without Downtime

Your payment-service needs to move from the legacy environment to the new Kubernetes cluster. It has 3 downstream dependencies and 2 data stores. You need to assess what will break, replicate the service, cut over traffic at the right moment, and validate every endpoint still works. A botched migration means payment failures and revenue loss.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the migration logic. Conductor handles dependency assessment, cutover sequencing, and validation gates.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers manage the migration. Assessing dependencies, replicating the service, cutting over traffic, and validating endpoints in the target environment.

| Worker | Task | What It Does |
|---|---|---|
| **AssessWorker** | `sm_assess` | Inventories the service's dependencies and data stores to build a migration plan |
| **CutoverWorker** | `sm_cutover` | Switches live traffic from the source environment to the target environment |
| **ReplicateWorker** | `sm_replicate` | Replicates the service, configuration, and data to the target environment |
| **ValidateWorker** | `sm_validate` | Confirms all endpoints in the target environment are responding correctly after cutover |

the workflow and rollback logic stay the same.

### The Workflow

```
sm_assess
 │
 ▼
sm_replicate
 │
 ▼
sm_cutover
 │
 ▼
sm_validate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
