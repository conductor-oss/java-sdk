# Data Sync

Two microservices maintain overlapping copies of a customer dataset. When a record changes in the source system, the target needs a delta sync: detect what changed, transform the changed records to the target schema, apply the updates, and confirm that the two systems are consistent.

## Pipeline

```
[sy_detect_changes]
     |
     v
[sy_resolve_conflicts]
     |
     v
[sy_apply_updates]
     |
     v
[sy_verify_consistency]
```

**Workflow inputs:** `systemA`, `systemB`, `syncMode`, `conflictStrategy`

## Workers

**ApplyUpdatesWorker** (task: `sy_apply_updates`)

Applies resolved updates to both systems.

- Writes `appliedToA`, `appliedToB`, `totalApplied`

**DetectChangesWorker** (task: `sy_detect_changes`)

Detects changes in both systems and identifies conflicts.

- Writes `changesInA`, `changesInB`, `conflicts`, `changeCountA`, `changeCountB`, `conflictCount`

**ResolveConflictsWorker** (task: `sy_resolve_conflicts`)

Resolves conflicts using the specified strategy (e.g. latest_wins).

- Writes `toApplyA`, `toApplyB`, `resolved`, `resolvedCount`

**VerifyConsistencyWorker** (task: `sy_verify_consistency`)

Verifies that both systems are consistent after sync.

- Reads `appliedToA`, `appliedToB`. Writes `status`, `summary`, `checksumMatch`, `verifiedAt`

---

**17 tests** | Workflow: `data_sync` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
