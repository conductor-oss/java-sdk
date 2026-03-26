# Rolling Update with Batch Execution and Automatic Rollback

Updating all replicas at once risks a full outage if the new version has a bug. This
workflow analyzes the current deployment, plans a rolling strategy (1 replica at a time,
20% max unavailable), executes the update in batches, and verifies all replicas are healthy
-- with automatic rollback if any batch fails.

## Workflow

```
service, newVersion
       |
       v
+--------------+     +---------------+     +------------------+     +-----------------+
| ru_analyze   | --> | ru_plan       | --> | ru_execute       | --> | ru_verify       |
+--------------+     +---------------+     +------------------+     +-----------------+
  ANALYZE-1342        strategy: rolling     batchesCompleted         allHealthy=true
  currentReplicas     batchSize: 1          = totalBatches           rollbackOccurred
  currentVersion      maxUnavailable: 20%   rollbackTriggered        = false
  healthyReplicas     rollbackOnFailure     = false
```

## Workers

**Analyze** -- Takes `service` and `newVersion`. Returns `analyzeId: "ANALYZE-1342"` with
`currentReplicas`, `currentVersion`, and `healthyReplicas`.

**PlanUpdate** -- Plans the rolling strategy: `batchSize: 1`,
`maxUnavailable: max(1, replicas/5)`, `totalBatches` equal to replica count, and
`rollbackOnFailure: true`.

**ExecuteUpdate** -- Executes the rolling update across all batches. Returns
`batchesCompleted` equal to `totalBatches`, `rollbackTriggered: false`, and an `updatedAt`
timestamp.

**VerifyUpdate** -- Confirms all replicas are healthy. `allHealthy` is true when no rollback
was triggered. Returns `rollbackOccurred` flag and a `completedAt` timestamp.

## Tests

33 unit tests cover deployment analysis, update planning, batch execution, and health
verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
