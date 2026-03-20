# Data Migration in Java with Conductor

Data migration with backup, transform, migrate, validate, and cutover. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

Migrating data between databases requires a strict sequence: back up the source, transform the data to match the target schema, load the transformed data, validate that source and target match, and then cut over. Each step is long-running and must not be repeated on a restart.

Without orchestration, data migrations are run as monolithic scripts that restart from scratch on any failure. Re-backing up and re-transforming gigabytes of data. There is no visibility into progress, and partial failures (e.g., 90% of records migrated) require manual investigation to find the gap.

## The Solution

**You just write the backup, transform, migrate, validate, and cutover workers. Conductor handles long-running step durability, crash-safe resume without re-processing data, and per-step progress tracking.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Five workers cover the full migration lifecycle: BackupWorker snapshots the source, TransformWorker reshapes data for the target schema, MigrateWorker bulk-loads records, ValidateWorker verifies parity, and CutoverWorker switches live traffic.

| Worker | Task | What It Does |
|---|---|---|
| **BackupWorker** | `dm_backup` | Creates a full backup of the source database and returns a backup ID and size. |
| **CutoverWorker** | `dm_cutover` | Switches application traffic to the new database after validation passes. |
| **MigrateWorker** | `dm_migrate` | Loads the transformed data into the target database, reporting record count and duration. |
| **TransformWorker** | `dm_transform` | Transforms the backed-up data from the source schema (v1) to the target schema (v2). |
| **ValidateWorker** | `dm_validate` | Compares source and target databases to verify 100% data match. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

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
java -jar target/data-migration-1.0.0.jar
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
java -jar target/data-migration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_migration_workflow \
  --version 1 \
  --input '{"sourceDb": "test-value", "targetDb": "test-value", "migrationName": "test"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_migration_workflow -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real database tools (pg_dump, AWS DMS, mysqlimport) and DNS cutover logic, the backup-transform-migrate-validate-cutover workflow stays exactly the same.

- **BackupWorker** (`dm_backup`): trigger a real database snapshot (pg_dump, mysqldump, AWS RDS snapshot)
- **CutoverWorker** (`dm_cutover`): update your application's connection string or DNS CNAME to point to the new database
- **MigrateWorker** (`dm_migrate`): use a bulk-loading tool (COPY, mysqlimport, AWS DMS) to insert records into the target database

Swapping pg_dump for AWS DMS behind the backup worker does not change the five-step migration pipeline.

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
data-migration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datamigration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataMigrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BackupWorker.java
│       ├── CutoverWorker.java
│       ├── MigrateWorker.java
│       ├── TransformWorker.java
│       └── ValidateWorker.java
└── src/test/java/datamigration/workers/
    ├── BackupWorkerTest.java        # 2 tests
    ├── CutoverWorkerTest.java        # 2 tests
    ├── MigrateWorkerTest.java        # 2 tests
    ├── TransformWorkerTest.java        # 2 tests
    └── ValidateWorkerTest.java        # 2 tests
```
