# Data Archival in Java Using Conductor : Stale Record Detection, Cold Storage Transfer, and Verified Purge

## The Problem

Your hot storage (production database, Elasticsearch cluster, fast SSD-backed store) is growing without bound. Old records that haven't been accessed in months are consuming expensive resources and slowing down queries. You need to move stale data to cheaper cold storage while guaranteeing nothing is lost. That means identifying which records exceed your retention policy, creating a consistent snapshot, transferring the snapshot to cold storage (S3 Glacier, Azure Archive, tape), verifying the archive is intact by comparing checksums and record counts, and only then purging the originals from hot storage. The order is critical: if you purge before verifying the archive, data is gone forever.

Without orchestration, you'd write a cron job that queries for old records, copies them to cold storage, and deletes from hot storage in a single script. If the transfer to S3 fails halfway through, some records are archived and some aren't; but the purge step might still run. If the process crashes after archiving but before purging, you'd re-archive the same records on the next run without knowing the previous archive succeeded. There's no audit trail of what was archived, when, or whether the integrity check passed.

## The Solution

**You just write the stale-record detection, snapshot, cold-storage transfer, verification, and purge workers. Conductor handles strict ordering so purges never run before verification, retries when cold storage transfers time out, and a full audit trail of every archival step.**

Each stage of the archival pipeline is a simple, independent worker. The stale record identifier queries for records older than the configured retention period. The snapshot worker creates a consistent point-in-time copy of those records. The transfer worker moves the snapshot to the configured cold storage path with checksumming. The verifier confirms the archive is intact by comparing record counts and checksums. The purge worker deletes stale records from hot storage; but only if verification passed. Conductor executes them in strict sequence, ensures the purge never runs before verification, retries if a cold storage transfer times out, and resumes from the exact step where it left off if the process crashes. ### What You Write: Workers

Five workers implement the archival safety chain: identifying stale records, creating snapshots, transferring to cold storage, verifying archive integrity via checksums, and purging originals only after verification passes.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyStaleWorker** | `arc_identify_stale` | Identifies stale records based on retention days. |
| **PurgeHotWorker** | `arc_purge_hot` | Purges stale records from hot storage if archive is verified. |
| **SnapshotRecordsWorker** | `arc_snapshot_records` | Creates a snapshot of stale records. |
| **TransferToColdWorker** | `arc_transfer_to_cold` | Transfers snapshot to cold storage. |
| **VerifyArchiveWorker** | `arc_verify_archive` | Verifies the archive integrity. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
arc_identify_stale
 │
 ▼
arc_snapshot_records
 │
 ▼
arc_transfer_to_cold
 │
 ▼
arc_verify_archive
 │
 ▼
arc_purge_hot

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
