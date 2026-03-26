# Cross Region

A global SaaS platform runs in three AWS regions. When a user updates their profile in us-east-1, the change must propagate to eu-west-1 and ap-southeast-1. The cross-region pipeline needs to replicate the update, handle regional failures independently, and verify that all regions eventually converge.

## Pipeline

```
[xr_replicate]
     |
     v
[xr_sync]
     |
     v
[xr_verify_consistency]
```

**Workflow inputs:** `primaryRegion`, `replicaRegion`, `datasetId`

## Workers

**XrReplicateWorker** (task: `xr_replicate`)

Initiates data replication from `primaryRegion` to `replicaRegion`. Generates a unique `replicationId` prefixed with `"REP-"` using `System.currentTimeMillis()` for traceability. Reports `bytesTransferred` = 52,428,800 (50 MB) and sets `replicated` = true on success.

- Reads `primaryRegion`, `replicaRegion`. Writes `replicated`, `replicationId`, `bytesTransferred`

**XrSyncWorker** (task: `xr_sync`)

Synchronizes data between regions after replication completes. Computes SHA-256 checksums for both primary and replica (both `"sha256:a1b2c3d4e5f6"`) and measures replication lag. Outputs `primaryChecksum`, `replicaChecksum`, `lagMs` = 45, and `synced` = true.

- Writes `synced`, `primaryChecksum`, `replicaChecksum`, `lagMs`

**XrVerifyConsistencyWorker** (task: `xr_verify_consistency`)

Verifies eventual consistency by comparing `primaryChecksum` against `replicaChecksum` with string equality. Outputs `consistent` = true when checksums match, false when the replica has diverged and needs re-sync.

- Reads `primaryChecksum`, `replicaChecksum`. Writes `consistent`

---

**12 tests** | Workflow: `cross_region_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
