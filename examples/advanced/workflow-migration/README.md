# Workflow Migration in Java Using Conductor :  Export from Legacy, Transform, Import to New, Verify

A Java Conductor workflow example for workflow migration. exporting workflow definitions and execution history from a legacy system, transforming them into the target format, importing them into the new orchestration platform, and verifying that the migrated workflows produce the same results. Uses [Conductor](https://github.

## Migrating from Legacy Orchestration Without Losing History

You're moving from Airflow to Conductor (or Jenkins to Conductor, or a custom cron-based system). The legacy system has 200 workflow definitions, execution history that auditors need, and active runs that can't be interrupted. Migration means exporting the old definitions and history, transforming them into the target format (different task naming, different input/output schemas), importing them into the new system, and verifying that the migrated workflows produce identical results.

Doing this manually per workflow is tedious and error-prone. Automating it as a pipeline lets you batch-migrate workflows, catch transformation errors early, and verify each migration before decommissioning the old system.

## The Solution

**You write the export and transformation logic. Conductor handles the migration pipeline, retries, and verification tracking.**

`WmExportOldWorker` extracts workflow definitions and execution history from the source system. `WmTransformWorker` converts the exported data into the target system's format. mapping task names, translating input/output schemas, and adjusting configuration. `WmImportNewWorker` loads the transformed definitions into the new orchestration platform. `WmVerifyWorker` runs test executions on the migrated workflows and compares outputs against the legacy system's results. Conductor records the export, transformation, import, and verification for each migrated workflow.

### What You Write: Workers

Four workers handle the migration pipeline: legacy system export, schema transformation, new-platform import, and output verification, each targeting one phase of the system cutover.

| Worker | Task | What It Does |
|---|---|---|
| **WmExportOldWorker** | `wm_export_old` | Exports the legacy workflow definition (tasks, format) from the old system |
| **WmImportNewWorker** | `wm_import_new` | Imports the transformed workflow definition into the new Conductor instance |
| **WmTransformWorker** | `wm_transform` | Converts the exported legacy format into the new Conductor workflow schema |
| **WmVerifyWorker** | `wm_verify` | Compares original and imported task counts to confirm migration completeness |

Workers implement the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations. the pattern and Conductor orchestration stay the same.

### The Workflow

```
wm_export_old
    │
    ▼
wm_transform
    │
    ▼
wm_import_new
    │
    ▼
wm_verify

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
java -jar target/workflow-migration-1.0.0.jar

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
java -jar target/workflow-migration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow workflow_migration_demo \
  --version 1 \
  --input '{"sourceSystem": "api", "targetSystem": "production", "workflowName": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w workflow_migration_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one migration phase. replace the simulated legacy exports with real Airflow or Jenkins API calls and the export-transform-import-verify pipeline runs unchanged.

- **WmExportOldWorker** (`wm_export_old`): export from real systems: Airflow `dag export` API, Jenkins job config XML, or Conductor's own `workflow/metadata` API for cross-cluster migration
- **WmTransformWorker** (`wm_transform`): implement real format transformation: JOLT for JSON-to-JSON mapping, custom translators for Airflow DAG Python to Conductor JSON, or XSLT for XML-based legacy systems
- **WmVerifyWorker** (`wm_verify`): run real verification: execute the migrated workflow with test inputs and diff the outputs against recorded legacy results using JSON diff tools

The export and transform contract stays fixed. Swap the simulated legacy connector for a real Airflow or Jenkins API client and the import-verify pipeline runs unchanged.

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
workflow-migration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowmigration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowMigrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WmExportOldWorker.java
│       ├── WmImportNewWorker.java
│       ├── WmTransformWorker.java
│       └── WmVerifyWorker.java
└── src/test/java/workflowmigration/workers/
    ├── WmExportOldWorkerTest.java        # 4 tests
    ├── WmImportNewWorkerTest.java        # 4 tests
    ├── WmTransformWorkerTest.java        # 4 tests
    └── WmVerifyWorkerTest.java        # 4 tests

```
