# Data Migration in Java Using Conductor :  Extract, Validate, Transform Schema, Load, and Verify

A Java Conductor workflow example for database-to-database data migration. extracting records from a source system in configurable batches, validating record integrity, transforming records from the source schema to the target schema, loading into the target system, and verifying the migration by comparing source and target record counts. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You're migrating data from one system to another, a legacy database to a new platform, an on-prem system to the cloud, or a monolithic database to microservice-owned data stores. The source and target schemas are different, so records need transformation. Some source records may be invalid and shouldn't migrate. After loading, you need to verify that the target contains the right number of records. If the load step fails after inserting 8,000 of 10,000 records, you need to know exactly where it stopped.

Without orchestration, you'd write a migration script that connects to both databases, reads records, transforms them inline, and inserts them one by one. If the target database connection drops mid-migration, you'd restart from the beginning. Potentially creating duplicates or missing records. There's no audit trail showing how many records were extracted, how many passed validation, how many were transformed, and whether the final count matches.

## The Solution

**You just write the extract, validate, transform, load, and verify workers. Conductor handles strict extract-validate-transform-load-verify ordering, retries when the target database is temporarily unavailable, and crash recovery that resumes from the exact migration step that failed.**

Each stage of the migration is a simple, independent worker. The extractor reads records from the source system in configurable batch sizes. The validator checks each record for integrity (required fields, data type constraints) and filters out invalid ones. The transformer maps records from the source schema to the target schema. The loader inserts transformed records into the target system. The verifier compares source and loaded record counts to confirm the migration is complete. Conductor executes them in strict sequence, retries if the target database is temporarily unavailable, and resumes from the exact step where it left off if the process crashes mid-migration. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers cover the full migration lifecycle: extracting records from the source database, validating integrity, transforming from source to target schema, loading into the target system, and verifying record counts match.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractSourceWorker** | `mi_extract_source` | Extracts records from the source database. |
| **LoadTargetWorker** | `mi_load_target` | Loads transformed records into the target system. |
| **TransformSchemaWorker** | `mi_transform_schema` | Transforms records from old schema to new schema. |
| **ValidateSourceWorker** | `mi_validate_source` | Validates extracted records, filtering out invalid ones. |
| **VerifyMigrationWorker** | `mi_verify_migration` | Verifies the migration by comparing source and loaded counts. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
mi_extract_source
    │
    ▼
mi_validate_source
    │
    ▼
mi_transform_schema
    │
    ▼
mi_load_target
    │
    ▼
mi_verify_migration
```

## Example Output

```
=== Data Migration Workflow Demo ===

Step 1: Registering task definitions...
  Registered: mi_extract_source, mi_validate_source, mi_transform_schema, mi_load_target, mi_verify_migration

Step 2: Registering workflow 'data_migration'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [extract] Extracted
  [load] Loaded
  [transform] Transformed
  [validate]
  [verify]

  Status: COMPLETED
  Output: {id=..., name=..., email=..., dept_id=...}

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
  --workflow data_migration \
  --version 1 \
  --input '{"sourceConfig": "sample-sourceConfig", "database": "sample-database", "legacy_hr_db": "sample-legacy-hr-db", "table": "sample-table", "targetConfig": "sample-targetConfig", "new_hr_system": "sample-new-hr-system", "batchSize": 5}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_migration -s COMPLETED -c 5
```

## How to Extend

Connect the extractor to your source database via JDBC, implement real schema mapping in the transformer, and batch-insert into the target with idempotency keys, the migration workflow runs unchanged.

- **ExtractSourceWorker** → connect to a real source database (MySQL, PostgreSQL, Oracle, MongoDB) with JDBC/driver and read records in batches using cursor-based pagination
- **ValidateSourceWorker** → add domain-specific validation rules (foreign key consistency, business rule checks, data type compatibility with the target schema)
- **TransformSchemaWorker** → implement real schema mapping (column renames, type conversions, computed fields, denormalization or normalization transformations)
- **LoadTargetWorker** → batch-insert into the target system (PostgreSQL `COPY`, MySQL `LOAD DATA`, DynamoDB `BatchWriteItem`) with idempotency keys to prevent duplicates on retry
- **VerifyMigrationWorker** → run count comparisons, checksum verification, and sample-based data integrity checks between source and target

Connecting to real databases via JDBC or implementing actual schema mapping logic does not require workflow changes, provided each worker outputs the expected record counts and transformation results.

**Add new stages** by inserting tasks in `workflow.json`, for example, a pre-migration snapshot for rollback, a CDC (change data capture) step to catch records modified during migration, or a cutover validation that compares query results between old and new systems.

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
│       ├── ExtractSourceWorker.java
│       ├── LoadTargetWorker.java
│       ├── TransformSchemaWorker.java
│       ├── ValidateSourceWorker.java
│       └── VerifyMigrationWorker.java
└── src/test/java/datamigration/workers/
    ├── ExtractSourceWorkerTest.java        # 4 tests
    ├── LoadTargetWorkerTest.java        # 3 tests
    ├── TransformSchemaWorkerTest.java        # 5 tests
    ├── ValidateSourceWorkerTest.java        # 4 tests
    └── VerifyMigrationWorkerTest.java        # 4 tests
```
