# Rolling Update in Java with Conductor : Analyze, Plan, Execute, Verify

Orchestrates zero-downtime rolling updates by analyzing current state, planning the update strategy, executing the rollout, and verifying all replicas are healthy.

## Zero-Downtime Updates Need Careful Orchestration

Updating 20 instances of a service simultaneously causes a full outage while the new version starts up. A rolling update replaces instances in batches. update 2, verify they're healthy, update the next 2, and so on. If a batch fails health checks, the rollout stops before affecting more instances.

The batch size and health check interval determine the trade-off between speed and safety. Updating 1 at a time is safest but slow (20 rounds). Updating 5 at a time is faster but riskier (a bad version affects 25% of capacity before detection). The plan step should consider current traffic levels, resource headroom, and the service's tolerance for reduced capacity during the update.

## The Solution

**You write the batch update and health check logic. Conductor handles rollout sequencing, batch-by-batch verification, and automatic rollback triggers.**

`AnalyzeWorker` examines the current deployment. instance count, health status, traffic distribution, and resource utilization, to determine the starting state. `PlanWorker` calculates the rollout strategy, batch size, health check wait time between batches, rollback triggers, and success criteria. `ExecuteWorker` performs the rolling update in batches, updating instances, waiting for health checks, and proceeding to the next batch. `VerifyWorker` confirms the full rollout completed, all instances running the new version, health checks passing, and metrics stable. Conductor sequences these steps and records each batch's execution for rollout audit.

### What You Write: Workers

Four workers manage the rolling update. Analyzing current state, planning the batch strategy, executing the rollout, and verifying all replicas are healthy.

| Worker | Task | What It Does |
|---|---|---|
| **Analyze** | `ru_analyze` | Analyzes the current deployment state before a rolling update. |
| **ExecuteUpdate** | `ru_execute` | Executes the rolling update according to the plan. |
| **PlanUpdate** | `ru_plan` | Plans the rolling update strategy (batch size, max unavailable, etc.). |
| **VerifyUpdate** | `ru_verify` | Verifies all replicas are healthy after the rolling update. |

the workflow and rollback logic stay the same.

### The Workflow

```
ru_analyze
 │
 ▼
ru_plan
 │
 ▼
ru_execute
 │
 ▼
ru_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
