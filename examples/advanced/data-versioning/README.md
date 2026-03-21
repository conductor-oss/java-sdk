# Dataset Versioning in Java Using Conductor : Snapshot, Tag, Diff, and Rollback

## Datasets Change, and You Need to Track How

A data pipeline updates your ML training dataset daily. Yesterday's model performed well, but today's retrained model has degraded accuracy. Was it the new data? Which rows changed? Can you roll back to yesterday's version and retrain? Without versioning, you're guessing. there's no snapshot to compare against, no diff to show what changed, and no tag to identify which version produced the good model.

Dataset versioning means capturing a snapshot before each update, tagging it (e.g., `v2.3-daily-2024-01-15`), comparing it against the previous tag to see additions, deletions, and schema changes, and rolling back if the diff crosses a threshold. too many deleted rows, unexpected column changes, or data quality regressions. Coordinating snapshot, tag, store, diff, and rollback as a reliable pipeline is where manual scripts fall apart.

## The Solution

**You write the snapshot and diff logic. Conductor handles the versioning pipeline, retries, and rollback coordination.**

`DvrSnapshotWorker` captures a point-in-time snapshot of the dataset and returns a snapshot ID. `DvrTagWorker` attaches a human-readable tag name (like `v1.0` or `prod-2024-01-15`) to the snapshot. `DvrStoreWorker` persists the snapshot metadata. dataset ID, snapshot ID, and tag, to the version catalog. `DvrDiffWorker` compares the current tag against the previous tag and produces diff statistics (rows added, removed, changed) plus a flag indicating whether rollback is needed. `DvrRollbackWorker` reverts to the previous version if the diff signals a problem. Conductor records every snapshot ID, tag, and diff result so you have a complete version history.

### What You Write: Workers

Five workers manage the version lifecycle: snapshot capture, tag assignment, metadata storage, diff computation, and conditional rollback, each scoped to one versioning concern.

| Worker | Task | What It Does |
|---|---|---|
| **DvrDiffWorker** | `dvr_diff` | Computes a change summary (added/modified/deleted rows) and determines if rollback is needed |
| **DvrRollbackWorker** | `dvr_rollback` | Rolls back the dataset to the previous version if the diff flagged issues |
| **DvrSnapshotWorker** | `dvr_snapshot` | Creates a point-in-time snapshot of the dataset with row count and checksum |
| **DvrStoreWorker** | `dvr_store` | Persists the tagged version to versioned storage (e.g., S3) under the dataset/tag path |
| **DvrTagWorker** | `dvr_tag` | Applies a version tag (e.g.### The Workflow

```
dvr_snapshot
 │
 ▼
dvr_tag
 │
 ▼
dvr_store
 │
 ▼
dvr_diff
 │
 ▼
dvr_rollback

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
