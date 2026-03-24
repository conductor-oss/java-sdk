# Migrating 50,000 Records with Schema Transformation and Validation

Moving data from one database to another without a backup, schema transform, and validation
step risks data loss and schema mismatches. This workflow backs up the source (12 GB),
transforms the schema from v1 to v2, migrates 50,000 records in 120 seconds, validates a
100% source-target match, and performs the cutover.

## Workflow

```
sourceDb, targetDb, migrationName
              |
              v
+--------------+     +----------------+     +---------------+     +----------------+     +----------------+
| dm_backup    | --> | dm_transform   | --> | dm_migrate    | --> | dm_validate    | --> | dm_cutover     |
+--------------+     +----------------+     +---------------+     +----------------+     +----------------+
  backupId: BKP-...   v1 -> v2 schema       50000 records          matchRate: 100%        cutover: true
  sizeGb: 12          /tmp/transformed-      durationSec: 120      valid: true             switched to new
                      data
```

## Workers

**BackupWorker** -- Backs up the source database. Returns `backupId: "BKP-{timestamp}"`,
`sizeGb: 12`.

**TransformWorker** -- Transforms schema from v1 to v2. Writes to
`dataPath: "/tmp/transformed-data"` with `recordCount: 50000`.

**MigrateWorker** -- Migrates 50,000 records to the target. Returns `recordCount: 50000`,
`durationSec: 120`.

**ValidateWorker** -- Compares source and target: `matchRate: 100`, `valid: true`.

**CutoverWorker** -- Switches to the new database. Returns `cutover: true`.

## Tests

10 unit tests cover backup, transformation, migration, validation, and cutover.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
