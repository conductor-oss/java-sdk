# GitOps Reconciliation: Detecting and Fixing Cluster Drift

Someone kubectl-edited a deployment in production and now the cluster state does not match
the Git repository. This workflow detects 3 drifted resources, plans the sync (2 updates
and 1 create), applies the changes to bring the cluster back to desired state, and verifies
the reconciliation.

## Workflow

```
repository, targetCluster
           |
           v
+-------------------+     +----------------+     +----------------+     +--------------------+
| go_detect_drift   | --> | go_plan_sync   | --> | go_apply_sync  | --> | go_verify_state    |
+-------------------+     +----------------+     +----------------+     +--------------------+
  DETECT_DRIFT-1361        sync plan:              all resources          cluster matches
  3 drifted resources      2 updates, 1 create    synced                 Git repository
```

## Workers

**DetectDriftWorker** -- Takes `repository` and `targetCluster` inputs (defaults to
`"unknown-repo"` / `"unknown-cluster"` if null). Detects 3 drifted resources and returns
`detect_driftId: "DETECT_DRIFT-1361"` with `driftedResources: 3`.

**PlanSyncWorker** -- Plans the synchronization: `updates: 2`, `creates: 1`. Returns
`plan_sync: true`.

**ApplySyncWorker** -- Applies the sync plan, bringing all resources to desired state.
Returns `apply_sync: true`.

**VerifyStateWorker** -- Confirms the cluster state matches the Git repository. Returns
`verify_state: true` and `completedAt: "2026-03-14T00:00:00Z"`.

## Tests

29 unit tests cover drift detection, sync planning, sync application, and state
verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
