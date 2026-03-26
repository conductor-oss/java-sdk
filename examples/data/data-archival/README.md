# Data Archival

A SaaS platform's primary database is growing at 50 GB per month. Records older than a retention threshold need to be identified, compressed, written to cold storage, and then purged from the primary store -- all without blocking active queries or losing audit trail entries.

## Pipeline

```
[arc_identify_stale]
     |
     v
[arc_snapshot_records]
     |
     v
[arc_transfer_to_cold]
     |
     v
[arc_verify_archive]
     |
     v
[arc_purge_hot]
```

**Workflow inputs:** `records`, `retentionDays`, `coldStoragePath`

## Workers

**IdentifyStaleWorker** (task: `arc_identify_stale`)

Identifies stale records based on retention days.

- Captures `instant.now()` timestamps
- Reads `records`, `retentionDays`. Writes `staleRecords`, `staleIds`, `staleCount`, `totalCount`

**PurgeHotWorker** (task: `arc_purge_hot`)

Purges stale records from hot storage if archive is verified.

- Reads `staleRecordIds`, `archiveVerified`. Writes `purgedCount`, `summary`, `skipped`

**SnapshotRecordsWorker** (task: `arc_snapshot_records`)

Creates a snapshot of stale records.

- Captures `instant.now()` timestamps
- Reads `staleRecords`. Writes `snapshot`

**TransferToColdWorker** (task: `arc_transfer_to_cold`)

Transfers snapshot to cold storage.

- Truncates strings to first 16 character(s), generates uuids, records wall-clock milliseconds
- Reads `snapshot`. Writes `archivePath`, `transferredCount`, `checksum`, `sizeBytes`

**VerifyArchiveWorker** (task: `arc_verify_archive`)

Verifies the archive integrity.

- Reads `checksum`, `expectedCount`. Writes `verified`, `archivePath`

---

**30 tests** | Workflow: `data_archival` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
