# Data Warehouse Load in Java Using Conductor :  Staging, Pre-Load Checks, Upsert, Post-Load Validation, and Metadata Update

A Java Conductor workflow example for data warehouse loading: staging incoming records to a temporary table, running pre-load quality checks against the schema, upserting validated records into the target table, running post-load validation to confirm the data landed correctly, and updating warehouse metadata with load statistics. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

Loading data into a warehouse is not just an INSERT statement. You need to stage records in a temporary table so the target table isn't locked during quality checks. You need to run pre-load validation (schema compliance, null checks, referential integrity) before touching production tables. You need to upsert. Inserting new records and updating existing ones based on a key. After loading, you need to verify the target table has the expected record count and the data is queryable. Finally, you need to update warehouse metadata (last load time, row counts, freshness) so downstream dashboards and dbt models know the data is current.

Without orchestration, you'd write a stored procedure or script that does staging, checking, loading, and metadata updates in a single transaction. If the upsert fails halfway, the staging table is left in an ambiguous state. If the metadata update fails after a successful load, downstream tools think the data is stale. There's no visibility into which step failed, how many records were staged vs: loaded, or whether post-load validation passed.

## The Solution

**You just write the staging, pre-load checks, upsert, post-load validation, and metadata update workers. Conductor handles strict stage-check-upsert-verify-update ordering, retries when the warehouse is temporarily unavailable, and precise record count tracking across every loading phase.**

Each stage of the warehouse load is a simple, independent worker. The stager writes records to a temporary staging table. The pre-load checker validates staged records against the target schema. The upserter performs INSERT ... ON CONFLICT UPDATE against the target table. The post-load validator confirms the target table has the expected record count. The metadata updater records the load timestamp, row count, and validation status. Conductor executes them in strict sequence, ensures the upsert only runs after pre-load checks pass, retries if the warehouse is temporarily unavailable, and tracks exactly how many records were staged, validated, upserted, and confirmed. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle the warehouse loading pipeline: staging records to a temporary table, running pre-load quality checks, upserting validated records into the target, verifying post-load record counts, and updating warehouse metadata with load statistics.

| Worker | Task | What It Does |
|---|---|---|
| **PostLoadValidationWorker** | `wh_post_load_validation` | Validates records in the target table after loading. |
| **PreLoadChecksWorker** | `wh_pre_load_checks` | Runs pre-load quality checks on staged records. |
| **StageDataWorker** | `wh_stage_data` | Stages incoming records into a temporary staging table. |
| **UpdateMetadataWorker** | `wh_update_metadata` | Updates warehouse metadata after a successful load. |
| **UpsertTargetWorker** | `wh_upsert_target` | Upserts records from staging into the target table. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
wh_stage_data
    │
    ▼
wh_pre_load_checks
    │
    ▼
wh_upsert_target
    │
    ▼
wh_post_load_validation
    │
    ▼
wh_update_metadata

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
java -jar target/data-warehouse-load-1.0.0.jar

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
java -jar target/data-warehouse-load-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_warehouse_load \
  --version 1 \
  --input '{"records": "sample-records", "targetTable": "production", "schema": "sample-schema"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_warehouse_load -s COMPLETED -c 5

```

## How to Extend

Write to a real staging table via JDBC, execute MERGE statements against Snowflake or Redshift, and update Hive Metastore freshness, the warehouse load workflow runs unchanged.

- **StageDataWorker** → write to a real staging table using JDBC bulk insert, Snowflake `PUT`/`COPY INTO`, or BigQuery load jobs
- **PreLoadChecksWorker** → run schema validation, null checks, foreign key integrity verification, and duplicate detection on the staging table
- **UpsertTargetWorker** → execute real `MERGE` or `INSERT ... ON CONFLICT` statements, or use warehouse-specific upsert patterns (Snowflake `MERGE`, Redshift `DELETE+INSERT`)
- **PostLoadValidationWorker** → compare expected vs: actual row counts in the target, run sample queries to verify data accessibility, check that aggregates make sense
- **UpdateMetadataWorker** → update Hive Metastore, dbt source freshness, or a custom metadata table with load timestamp, row count, and validation status

Pointing the stager at a real staging table or the upserter at PostgreSQL COPY leaves the load workflow unchanged, provided each worker returns the expected row counts and status fields.

**Add new stages** by inserting tasks in `workflow.json`, for example, a rollback step that drops staged data on pre-load failure, a notification step that alerts the analytics team when the load completes, or a dbt trigger that kicks off downstream model refreshes after successful loading.

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
data-warehouse-load/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datawarehouseload/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataWarehouseLoadExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PostLoadValidationWorker.java
│       ├── PreLoadChecksWorker.java
│       ├── StageDataWorker.java
│       ├── UpdateMetadataWorker.java
│       └── UpsertTargetWorker.java
└── src/test/java/datawarehouseload/workers/
    ├── PostLoadValidationWorkerTest.java        # 6 tests
    ├── PreLoadChecksWorkerTest.java        # 6 tests
    ├── StageDataWorkerTest.java        # 6 tests
    ├── UpdateMetadataWorkerTest.java        # 6 tests
    └── UpsertTargetWorkerTest.java        # 6 tests

```
