# Data Versioning

A data pipeline processes datasets that evolve over time. Each processing run must snapshot the input data, tag it with a version, process it, and store the output alongside its version metadata so that any past result can be reproduced exactly.

## Pipeline

```
[dvr_snapshot]
     |
     v
[dvr_tag]
     |
     v
[dvr_store]
     |
     v
[dvr_diff]
     |
     v
[dvr_rollback]
```

**Workflow inputs:** `datasetId`, `tagName`, `previousTag`

## Workers

**DvrDiffWorker** (task: `dvr_diff`)

- Writes `summary`, `diffStats`, `needsRollback`

**DvrRollbackWorker** (task: `dvr_rollback`)

- Reads `needsRollback`. Writes `rolledBack`

**DvrSnapshotWorker** (task: `dvr_snapshot`)

- Records wall-clock milliseconds
- Writes `snapshotId`, `rows`, `checksum`

**DvrStoreWorker** (task: `dvr_store`)

- Writes `stored`, `storagePath`

**DvrTagWorker** (task: `dvr_tag`)

- Writes `tagged`, `tagName`

---

**20 tests** | Workflow: `data_versioning_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
