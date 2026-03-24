# Data Sync in Java Using Conductor : Bidirectional Change Detection, Conflict Resolution, and Consistency Verification

## The Problem

You have data in two systems that need to stay in sync: a CRM and an ERP, a mobile app's local database and the cloud backend, a primary database and a partner system. Changes happen on both sides between sync cycles. You need to detect what changed in each system, identify conflicts where both systems modified the same record, resolve those conflicts using a configurable strategy (latest timestamp wins, source-of-truth priority, manual review), apply the merged updates to both systems, and verify consistency after the sync completes. If the apply step fails after updating system A but before updating system B, the systems are now out of sync, worse than before.

Without orchestration, you'd write a sync script that queries both systems, diffs the results, picks winners for conflicts, and writes updates. If the write to system B fails, system A has already been updated and there's no automatic rollback or retry. There's no record of which conflicts were found, how they were resolved, or whether the final state is consistent. Adding a conflict resolution strategy or a third system means rewriting deeply coupled code.

## The Solution

**You just write the change detection, conflict resolution, update application, and consistency verification workers. Conductor handles strict detect-resolve-apply-verify ordering, retries when either system is temporarily unavailable, and a full audit trail of changes detected, conflicts resolved, and updates applied.**

Each stage of the sync pipeline is a simple, independent worker. The change detector queries both systems and identifies inserts, updates, and deletes since last sync, flagging records modified in both as conflicts. The conflict resolver applies the configured strategy (latest_wins, system_a_priority, merge) to produce a unified set of updates. The applier writes the resolved changes to both systems. The consistency verifier confirms both systems match after sync. Conductor executes them in strict sequence, ensures updates only apply after conflict resolution, retries if a system is temporarily unavailable, and provides a complete audit trail of changes detected, conflicts resolved, and updates applied.

### What You Write: Workers

Four workers manage bidirectional sync: detecting changes in both systems since the last sync, resolving conflicts using a configurable strategy like latest-wins, applying the merged updates to both systems, and verifying post-sync consistency.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyUpdatesWorker** | `sy_apply_updates` | Applies resolved updates to both systems. |
| **DetectChangesWorker** | `sy_detect_changes` | Detects changes in both systems and identifies conflicts. |
| **ResolveConflictsWorker** | `sy_resolve_conflicts` | Resolves conflicts using the specified strategy (e.g, latest_wins). |
| **VerifyConsistencyWorker** | `sy_verify_consistency` | Verifies that both systems are consistent after sync. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
sy_detect_changes
 │
 ▼
sy_resolve_conflicts
 │
 ▼
sy_apply_updates
 │
 ▼
sy_verify_consistency

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
