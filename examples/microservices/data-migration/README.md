# Data Migration in Java with Conductor

Data migration with backup, transform, migrate, validate, and cutover. ## The Problem

Migrating data between databases requires a strict sequence: back up the source, transform the data to match the target schema, load the transformed data, validate that source and target match, and then cut over. Each step is long-running and must not be repeated on a restart.

Without orchestration, data migrations are run as monolithic scripts that restart from scratch on any failure. Re-backing up and re-transforming gigabytes of data. There is no visibility into progress, and partial failures (e.g., 90% of records migrated) require manual investigation to find the gap.

## The Solution

**You just write the backup, transform, migrate, validate, and cutover workers. Conductor handles long-running step durability, crash-safe resume without re-processing data, and per-step progress tracking.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Five workers cover the full migration lifecycle: BackupWorker snapshots the source, TransformWorker reshapes data for the target schema, MigrateWorker bulk-loads records, ValidateWorker verifies parity, and CutoverWorker switches live traffic.

| Worker | Task | What It Does |
|---|---|---|
| **BackupWorker** | `dm_backup` | Creates a full backup of the source database and returns a backup ID and size. |
| **CutoverWorker** | `dm_cutover` | Switches application traffic to the new database after validation passes. |
| **MigrateWorker** | `dm_migrate` | Loads the transformed data into the target database, reporting record count and duration. |
| **TransformWorker** | `dm_transform` | Transforms the backed-up data from the source schema (v1) to the target schema (v2). |
| **ValidateWorker** | `dm_validate` | Compares source and target databases to verify 100% data match. |

the workflow coordination stays the same.

### The Workflow

```
dm_backup
 │
 ▼
dm_transform
 │
 ▼
dm_migrate
 │
 ▼
dm_validate
 │
 ▼
dm_cutover

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
