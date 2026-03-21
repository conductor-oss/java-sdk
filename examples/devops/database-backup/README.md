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

| Worker | Task | What It Does |
|---|---|---|
| **ValidateConfig** | `backup_validate_config` | Validates the backup configuration (database host, port, name, type; storage type, bucket; retention policy). Rejects invalid configs with `FAILED_WITH_TERMINAL_ERROR` before downstream workers run. Returns validated `databaseType`, `databaseHost`, `databaseName`, `storageType`, and retention settings. |
| **TakeSnapshot** | `backup_take_snapshot` | Takes a real database snapshot using pg_dump, mysqldump, mongodump, or redis-cli BGSAVE depending on `databaseType`. Computes a real SHA-256 checksum from the dump file. Falls back to mock mode with deterministic output if the required credentials (e.g., `PGPASSWORD` for PostgreSQL) are not set. |
| **VerifyIntegrity** | `backup_verify_integrity` | Runs four integrity checks: SHA-256 checksum verification, file size sanity, compression header validation, and a real trial restore via `pg_restore --list` (falls back to file header validation if pg_restore is unavailable). Fails the task if any check does not pass. |
| **UploadToStorage** | `backup_upload_to_storage` | Uploads the verified backup to offsite storage (S3, GCS, Azure Blob, or local). Builds the full storage URI, simulates throughput (50--200 MB/s), and returns the `storageUri`, `etag`, and `versionId`. |
| **CleanupOldBackups** | `backup_cleanup_old` | Enforces the retention policy by listing existing backups (deterministically generated), deleting those older than `retentionDays` or exceeding `maxBackups`, and reporting freed storage. |
| **SendNotification** | `backup_send_notification` | Sends a backup completion notification summarizing the pipeline results (filename, size, storage location, verification status, cleanup results). Supports Slack, email, and console channels. |

TakeSnapshot runs real dump commands when credentials are available, and falls back to mock mode with deterministic output otherwise. The remaining workers implement storage and notification operations with realistic outputs. Replace with actual S3 SDK and Slack webhook calls, the workflow stays the same.

### The Workflow

```
Input ->  -> Output

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

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
| `PGPASSWORD` | _(none, mock mode)_ | PostgreSQL password. Required for real `pg_dump` backups. If unset, TakeSnapshot runs in mock mode with simulated output. |

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
