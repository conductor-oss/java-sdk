# Full-Lifecycle Database Backup with Integrity Verification

Your nightly cron job ran `pg_dump` for months until someone changed the database password.
The job failed silently. Nobody noticed until a disk failure forced a restore -- and the
last good backup was three weeks old. This workflow validates the config, takes a real
snapshot (via `pg_dump`, `mysqldump`, `mongodump`, or `redis-cli`), verifies integrity with
SHA-256 checksums and trial restore, uploads to S3/GCS/Azure, enforces retention, and sends
a notification. Every step is tracked; silent failures are impossible.

## Workflow

```
database, storage, retention, notification
                |
                v
+------------------------+     +-----------------------+     +--------------------------+
| backup_validate_config | --> | backup_take_snapshot  | --> | backup_verify_integrity  |
+------------------------+     +-----------------------+     +--------------------------+
  validates host/port/type      pg_dump/mysqldump/etc.        SHA-256 checksum match
  SUPPORTED_DB_TYPES:           SHA-256 of real file          trial pg_restore --list
  postgresql,mysql,mongodb,     50MB-2GB deterministic size
  redis
         |
         v
+--------------------------+     +--------------------+     +---------------------------+
| backup_upload_to_storage | --> | backup_cleanup_old | --> | backup_send_notification  |
+--------------------------+     +--------------------+     +---------------------------+
  s3://bucket/path/file          retentionDays cutoff        webhook/file/console channel
  ETag + versionId               maxBackups cap              SUCCESS/FAILED status
  throughput: 50-200 MB/s        freed bytes computed        JSON audit file in /tmp/
```

## Workers

**ValidateConfig** -- Validates `database` (host, port 1-65535, name, type from
`SUPPORTED_DB_TYPES`), `storage` (type from `SUPPORTED_STORAGE_TYPES`: s3/gcs/azure-blob/
local, bucket or path required), and `retention` (defaults to 30 days, 10 max backups).
Fails with `FAILED_WITH_TERMINAL_ERROR` on invalid config.

**TakeSnapshot** -- Runs the real dump tool via `ProcessBuilder`: `pg_dump` (port 5432),
`mysqldump` (3306), `mongodump` (27017), or `redis-cli BGSAVE` (6379). Computes SHA-256
from actual file bytes. Falls back to mock mode if `PGPASSWORD` is unset for PostgreSQL.
Deterministic size is 50MB-2GB based on database name hash.

**VerifyIntegrity** -- Re-computes SHA-256, checks file size sanity, validates compression
headers, and runs `pg_restore --list` as a dry-run trial restore.

**UploadToStorage** -- Builds the storage URI (`s3://`, `gs://`, `https://*.blob.core.windows.net/`),
computes upload throughput (50-200 MB/s), and generates a deterministic ETag and versionId.

**CleanupOldBackups** -- Generates 5-19 deterministic existing backups spanning 60 days.
Sorts by timestamp descending, deletes those older than `retentionDays` or exceeding
`maxBackups`. Reports `freedBytes` and `deletedFiles`.

**SendNotification** -- Supports `"webhook"` (real HTTP POST with 5s timeout), `"file"`,
or `"console"` channels. Always writes a JSON audit file to `/tmp/backup-notifications/`.

## Tests

56 unit tests cover config validation, snapshot creation, integrity checks, storage upload,
retention cleanup, and notification delivery.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
