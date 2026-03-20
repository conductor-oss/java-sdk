# User Migration in Java Using Conductor :  ETL Pipeline for Cross-Database User Transfer

A Java Conductor workflow example for migrating user records between databases .  extracting from a source system, transforming schemas (e.g., v1 to v2 with new fields like avatar and timezone), loading into the target database, verifying record counts match, and notifying stakeholders of the result. Uses [Conductor](https://github.## The Problem

You need to migrate user data from one database to another .  a legacy system to a new platform, a monolith to microservices, or an old schema to a new one. The migration is a classic ETL pipeline: extract users in batches from the source, transform each record to the new schema (adding fields, renaming columns, converting formats), load the transformed records into the target database, verify that every extracted record made it through, and notify the team whether the migration succeeded or had mismatches.

Without orchestration, you'd write a long procedural script that couples extraction, transformation, and loading into one fragile process. If the load step fails halfway through a batch, you have no idea which records made it. If verification detects a mismatch, there's no audit trail to diagnose where records were lost. Retrying means re-running the entire pipeline from scratch, and there's no visibility into how long each phase took or where bottlenecks lie.

## The Solution

**You just write the extract, transform, load, verify, and notify workers. Conductor handles the ETL pipeline sequencing and batch data flow.**

Each ETL phase .  extract, transform, load, verify, notify ,  is a simple, independent worker. Conductor runs them in sequence, threads the extracted user list into the transform step, passes the transformed data to the loader, feeds the loaded/expected counts into verification, and delivers the final result to the notification step. If any step fails (a database timeout, a schema error), Conductor retries automatically and resumes from exactly where it left off. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

ExtractWorker reads users from the source database, TransformWorker converts schemas, LoadWorker batch-inserts into the target, VerifyMigrationWorker confirms record counts, and NotifyMigrationWorker reports the result.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractWorker** | `umg_extract` | Reads user records in batches from the source database and returns the extracted list with a count |
| **TransformWorker** | `umg_transform` | Converts user records from the old schema to the new one, adding fields like avatar and timezone |
| **LoadWorker** | `umg_load` | Writes the transformed user records into the target database and reports loaded/failed counts |
| **VerifyMigrationWorker** | `umg_verify` | Compares the loaded count against the expected count to confirm all records migrated successfully |
| **NotifyMigrationWorker** | `umg_notify` | Sends a migration status notification to stakeholders with the verification result |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
umg_extract
    │
    ▼
umg_transform
    │
    ▼
umg_load
    │
    ▼
umg_verify
    │
    ▼
umg_notify
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
java -jar target/user-migration-1.0.0.jar
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
java -jar target/user-migration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow umg_user_migration \
  --version 1 \
  --input '{"sourceDb": "test-value", "targetDb": "test-value", "batchSize": 10}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w umg_user_migration -s COMPLETED -c 5
```

## How to Extend

Each worker handles one ETL phase .  connect your source database for extraction, your target system for loading, and your notification service (Slack, email) for status alerts, and the migration workflow stays the same.

- **ExtractWorker** (`umg_extract`): connect to your source database via JDBC/JPA, paginate with cursor-based queries, and handle configurable batch sizes
- **TransformWorker** (`umg_transform`): implement real schema mapping with Jackson or MapStruct, handle field renames, type conversions, and default value population
- **LoadWorker** (`umg_load`): use JDBC batch inserts or JPA bulk save against the target database, with transaction management and partial-failure tracking
- **VerifyMigrationWorker** (`umg_verify`): add checksum validation, spot-check individual records, or compare hash digests beyond simple count matching
- **NotifyMigrationWorker** (`umg_notify`): send Slack messages, email reports, or update a migration dashboard with per-batch statistics and timing

Connect your real source and target databases and the extract-transform-load-verify-notify migration pipeline runs without modification.

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
user-migration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/usermigration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── UserMigrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractWorker.java
│       ├── LoadWorker.java
│       ├── NotifyMigrationWorker.java
│       ├── TransformWorker.java
│       └── VerifyMigrationWorker.java
└── src/test/java/usermigration/workers/
    ├── ExtractWorkerTest.java        # 2 tests
    ├── LoadWorkerTest.java        # 2 tests
    ├── NotifyMigrationWorkerTest.java        # 2 tests
    ├── TransformWorkerTest.java        # 3 tests
    └── VerifyMigrationWorkerTest.java        # 3 tests
```
