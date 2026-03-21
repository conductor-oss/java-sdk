# Data Archival in Java Using Conductor :  Stale Record Detection, Cold Storage Transfer, and Verified Purge

A Java Conductor workflow example for data archival. identifying records that exceed a configurable retention period, snapshotting them, transferring the snapshot to cold storage, verifying archive integrity via checksum, and purging the stale records from hot storage only after verification passes. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

Your hot storage (production database, Elasticsearch cluster, fast SSD-backed store) is growing without bound. Old records that haven't been accessed in months are consuming expensive resources and slowing down queries. You need to move stale data to cheaper cold storage while guaranteeing nothing is lost. That means identifying which records exceed your retention policy, creating a consistent snapshot, transferring the snapshot to cold storage (S3 Glacier, Azure Archive, tape), verifying the archive is intact by comparing checksums and record counts, and only then purging the originals from hot storage. The order is critical: if you purge before verifying the archive, data is gone forever.

Without orchestration, you'd write a cron job that queries for old records, copies them to cold storage, and deletes from hot storage in a single script. If the transfer to S3 fails halfway through, some records are archived and some aren't; but the purge step might still run. If the process crashes after archiving but before purging, you'd re-archive the same records on the next run without knowing the previous archive succeeded. There's no audit trail of what was archived, when, or whether the integrity check passed.

## The Solution

**You just write the stale-record detection, snapshot, cold-storage transfer, verification, and purge workers. Conductor handles strict ordering so purges never run before verification, retries when cold storage transfers time out, and a full audit trail of every archival step.**

Each stage of the archival pipeline is a simple, independent worker. The stale record identifier queries for records older than the configured retention period. The snapshot worker creates a consistent point-in-time copy of those records. The transfer worker moves the snapshot to the configured cold storage path with checksumming. The verifier confirms the archive is intact by comparing record counts and checksums. The purge worker deletes stale records from hot storage; but only if verification passed. Conductor executes them in strict sequence, ensures the purge never runs before verification, retries if a cold storage transfer times out, and resumes from the exact step where it left off if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

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
java -jar target/data-archival-1.0.0.jar

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
java -jar target/data-archival-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_archival \
  --version 1 \
  --input '{"records": "sample-records", "retentionDays": "sample-retentionDays", "coldStoragePath": "sample-coldStoragePath"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_archival -s COMPLETED -c 5

```

## How to Extend

Connect the transfer worker to S3 Glacier or Azure Archive Blob, add real checksum verification, and the snapshot-verify-purge workflow runs unchanged.

- **IdentifyStaleWorker** → query your production database for records older than `retentionDays` using `last_accessed_at` or `created_at` timestamps
- **SnapshotRecordsWorker** → create a real point-in-time snapshot (PostgreSQL `pg_dump` with `--data-only`, MySQL `mysqldump`, or DynamoDB export)
- **TransferToColdWorker** → upload to S3 Glacier, Azure Archive Blob, or Google Coldline with multipart upload and checksum verification
- **VerifyArchiveWorker** → download the archive header and compare checksums (SHA-256) and record counts against the original snapshot
- **PurgeHotWorker** → execute batch `DELETE` statements or TTL-based expiration against the hot storage, guarded by the verification result

Pointing the transfer worker at a different cold storage backend requires no workflow modifications, as long as it returns the expected archive path and checksum fields.

**Add new stages** by inserting tasks in `workflow.json`, for example, a notification step that emails the DBA team before purging, a compliance-hold check that prevents archival of records under legal hold, or a compression step that gzips the snapshot before cold storage transfer.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
data-archival/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataarchival/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataArchivalExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── IdentifyStaleWorker.java
│       ├── PurgeHotWorker.java
│       ├── SnapshotRecordsWorker.java
│       ├── TransferToColdWorker.java
│       └── VerifyArchiveWorker.java
└── src/test/java/dataarchival/workers/
    ├── IdentifyStaleWorkerTest.java        # 6 tests
    ├── PurgeHotWorkerTest.java        # 6 tests
    ├── SnapshotRecordsWorkerTest.java        # 6 tests
    ├── TransferToColdWorkerTest.java        # 6 tests
    └── VerifyArchiveWorkerTest.java        # 6 tests

```
