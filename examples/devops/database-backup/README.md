# Database Backup in Java with Conductor: Snapshot, Compress, Upload, Verify

The production disk died on a Tuesday. The team pulled up the backup schedule and discovered the last successful backup was three weeks ago. the nightly cron job had been failing silently since someone changed the database password. The backup before that? Corrupted, because nobody ever tested a restore. Three weeks of customer data, order history, and configuration changes, gone. The CTO learned about it when the recovery estimate came back as "we don't know." This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full backup lifecycle, snapshot, compress, upload to offsite storage, and verify restore integrity, with tracked execution so silent failures become impossible.

## Backups You Can Trust

Your production database holds everything. If it goes down without a verified backup, the business stops. A reliable backup pipeline takes a consistent snapshot, copies it to offsite storage, verifies the backup can actually be restored, and cleans up old backups according to your retention policy. If any step fails silently, you only find out when you need the backup most.

Without orchestration, you'd wire all of this together in a single monolithic class. Managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the snapshot and verification logic. Conductor handles backup sequencing, upload retries, and retention enforcement.**

The backup workers snapshot the database at a consistent point in time, compress the output for efficient storage, upload to off-site storage with proper naming and retention metadata, and verify integrity by testing restore operations. Conductor sequences these steps, retries failed uploads without re-snapshotting, and records the complete backup provenance for disaster recovery planning.

### What You Write: Workers

Six workers implement the full backup lifecycle. Configuration validation, snapshot, integrity verification, upload, retention cleanup, and notification.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **ValidateConfig** | `backup_validate_config` | Validates the backup configuration (database host, port, name, type; storage type, bucket; retention policy). Rejects invalid configs with `FAILED_WITH_TERMINAL_ERROR` before downstream workers run. Returns validated `databaseType`, `databaseHost`, `databaseName`, `storageType`, and retention settings. | Simulated (validation logic is real, but no actual database connection is tested) |
| **TakeSnapshot** | `backup_take_snapshot` | Simulates a database snapshot (pg_dump, mysqldump, mongodump, or redis-cli BGSAVE depending on `databaseType`). Generates a deterministic filename with UTC timestamp, computes a SHA-256 checksum, and calculates a realistic file size (50 MB. 2 GB) based on the database name. | Simulated |
| **VerifyIntegrity** | `backup_verify_integrity` | Runs four integrity checks against the snapshot: SHA-256 checksum verification, file size sanity check, compression header validation, and a simulated trial restore. Fails the task if any check does not pass. | Simulated |
| **UploadToStorage** | `backup_upload_to_storage` | Uploads the verified backup to offsite storage (S3, GCS, Azure Blob, or local). Builds the full storage URI, simulates throughput (50--200 MB/s), and returns the `storageUri`, `etag`, and `versionId`. | Simulated |
| **CleanupOldBackups** | `backup_cleanup_old` | Enforces the retention policy by listing existing backups (deterministically generated), deleting those older than `retentionDays` or exceeding `maxBackups`, and reporting freed storage. | Simulated |
| **SendNotification** | `backup_send_notification` | Sends a backup completion notification summarizing the pipeline results (filename, size, storage location, verification status, cleanup results). Supports Slack, email, and console channels. | Simulated |

Workers simulate database and storage operations with deterministic, realistic outputs so you can observe the full backup pipeline without running real infrastructure. Replace with actual pg_dump, S3 SDK, and Slack webhook calls, the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
Input ->  -> Output
```

## Example Output

```
=== Database Backup Demo: Snapshot, Compress, Upload, Verify ===

Step 1: Registering task definitions...
  Registered: backup_cleanup_old, backup_send_notification, backup_take_snapshot, backup_upload_to_storage, backup_validate_config, backup_verify_integrity

Step 2: Registering workflow 'database_backup'...
  Workflow registered.

Step 3: Starting workers...
  6 workers polling.

Step 4: Starting workflow...
  Workflow ID: 73f2bd16-708e-211a-d8f6-a214e86cab69

[backup_validate_config] Validating backup configuration...
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
[backup_take_snapshot] Taking standard snapshot of 'default' on dbConfig.get("host")-value...
  Tool: weather_api
  Output: default_timestamp-valueextension-value
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
[backup_verify_integrity] Verifying backup: default_timestamp-valueextension-value
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
[backup_upload_to_storage] Uploading default_timestamp-valueextension-value to s3...
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
[backup_cleanup_old] Enforcing retention policy for 'default'...
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
[backup_send_notification] Sending backup notification...
  Channel: console
  Recipients: 
  Status: completed
  Message:
    Operation completed successfully
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: CHECKSUMVALIDPASSFAI-001
  File size: 2048. 2048
  Compression: COMPRESSIONVALIDPASS-001. CompressionDetail-value
  Trial restore: RESTOREVALIDPASSFAIL-001. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: PASS
  File size: PASS. 2048
  Compression: PASS. CompressionDetail-value
  Trial restore: PASS. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: w-value
  Configuration INVALID. 3 error(s)
  ERROR: e-value
  Tool: weather_api
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: 2048
  Checksum (SHA-256): [check1, check2]...
  Duration: 2000ms
  Checksum: PASS
  File size: PASS. 2048
  Compression: PASS. CompressionDetail-value
  Trial restore: PASS. RestoreDetail-value
  Result: True/3 checks passed
  Destination: /output/result.json
  Size: 2026-03-16
  Throughput: 2026-03-16/s
  Duration: 1200ms
  ETag: abc123def456
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: 2026-03-16
  Channel: console
  Recipients: 
  Status: completed
  Message:
  Configuration valid
  WARNING: ...
  Configuration INVALID. 3 error(s)
  ERROR: ...
  Tool: ...
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: ...
  Checksum (SHA-256): ......
  Duration: 2000ms
  Checksum: PASS
  File size: PASS. ...
  Compression: PASS. ...
  Trial restore: PASS. ...
  Result: .../3 checks passed
  Destination: ...
  Size: ...
  Throughput: .../s
  Duration: ...ms
  ETag: ...
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: ...
  Channel: console
  Recipients: ...
  Status: ...
  Message:
  Configuration valid
  WARNING: ...
  Configuration INVALID. 3 error(s)
  ERROR: ...
  Tool: ...
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Size: ...
  Checksum (SHA-256): ......
  Duration: 2000ms
  Checksum: PASS
  File size: PASS. ...
  Compression: PASS. ...
  Trial restore: PASS. ...
  Result: .../3 checks passed
  Destination: ...
  Size: ...
  Throughput: .../s
  Duration: ...ms
  ETag: ...
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Freed: ...
  Channel: console
  Recipients: ...
  Status: ...
  Message:
  Configuration valid
  WARNING: <w)>
  Configuration INVALID. 3 error(s)
  ERROR: <e)>
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Duration: 2000ms
  Checksum: PASS
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Channel: console
  Configuration valid
  WARNING: <w)>
  Configuration INVALID. 3 error(s)
  ERROR: <e)>
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Duration: 2000ms
  Checksum: PASS
  Policy: retain 30 days, max 10 backups
  Found 3 existing backups in storage
  Keeping: 3 backups
  Deleting: 3 backups
  Channel: console
  Message:

  Message:


  Configuration valid
  WARNING: <w)>
  Configuration INVALID. 0 error(s)
  ERROR: <e)>
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}
  Checksum: PASS
  Found 0 existing backups in storage
  Keeping: 0 backups
  Deleting: 0 backups
  Message:

  Status: COMPLETED
  Output: {databaseName=default, filename=default_timestamp-valueextension-value, sizeBytes=2048, sizeFormatted=2048, storageUri=/output/result.json, verified=true, deletedCount=3, freedFormatted=2026-03-16, notificationStatus=completed}

Result: PASSED
```
## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/database-backup-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/database-backup-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow database_backup \
  --version 1 \
  --input '{"database": {"type": "postgresql", "host": "db-primary.internal", "port": 5432, "name": "orders_production", "user": "backup_agent"}, "storage": {"type": "s3", "bucket": "acme-db-backups", "path": "postgresql/orders", "region": "us-east-1"}, "retention": {"days": 30, "maxBackups": 10}, "notification": {"channel": "slack", "recipients": ["dba-team@example.com", "oncall@example.com"]}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w database_backup -s COMPLETED -c 5
```

## How to Extend

Each worker handles one backup stage. Replace the simulated calls with pg_dump, AWS RDS snapshots, or S3 uploads for real database snapshots and offsite storage, and the backup workflow runs unchanged.



Connect to your database and S3 APIs; the backup pipeline continues to work with the same snapshot-upload-verify interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
database-backup/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/databasebackup/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DatabaseBackupExample.java          # Main entry point (supports --workers mode)
│   └── workers/
```
