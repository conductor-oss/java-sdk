# Database Migration in Java with Conductor :  Backup, Migrate Schema, Validate, Update Application

Automates database schema migrations using [Conductor](https://github.com/conductor-oss/conductor). This workflow creates a pre-migration backup, applies ALTER statements and schema changes, validates the new schema matches the expected state, and deploys the application with updated schema mappings.

## Schema Changes Without the Fear

You need to add a column to the orders table in production. A wrong ALTER statement could lock the table, corrupt data, or leave the application pointing at a schema that no longer matches its code. The safe path: snapshot the database first, apply the migration, validate that every expected table and column exists, then deploy the app version that knows about the new schema. If validation fails, you have the backup to restore from.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the migration and validation logic. Conductor handles backup-before-migrate sequencing, schema verification gates, and migration audit trails.**

`BackupWorker` creates a point-in-time backup of the database before any schema changes. enabling rollback if the migration fails. `MigrateWorker` applies the schema migration. ALTER TABLE, CREATE INDEX, data transformation,  using the appropriate migration tool and strategy. `ValidateSchemaWorker` verifies the migration succeeded,  checking table structures, column types, constraint integrity, and data consistency. `UpdateAppWorker` deploys or configures the application to use the new schema,  updating connection strings, enabling new features that depend on the schema changes. Conductor sequences these four steps and records the migration outcome for audit.

### What You Write: Workers

Four workers execute the migration safely. Creating a pre-migration backup, applying schema changes, validating the result, and deploying the updated application.

| Worker | Task | What It Does |
|---|---|---|
| **BackupWorker** | `dbm_backup` | Creates a database snapshot before migration. |
| **MigrateWorker** | `dbm_migrate` | Applies database migration ALTER statements. |
| **UpdateAppWorker** | `dbm_update_app` | Deploys the application with updated schema mapping. |
| **ValidateSchemaWorker** | `dbm_validate_schema` | Validates that schema matches expected state after migration. |

Workers implement infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls. the workflow and rollback logic stay the same.

### The Workflow

```
dbm_backup
    │
    ▼
dbm_migrate
    │
    ▼
dbm_validate_schema
    │
    ▼
dbm_update_app

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
java -jar target/database-migration-devops-1.0.0.jar

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
java -jar target/database-migration-devops-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow database_migration_devops_workflow \
  --version 1 \
  --input '{"database": {"key": "value"}, "migrationVersion": "1.0"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w database_migration_devops_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one migration phase. replace the simulated calls with Flyway, Liquibase, or pt-online-schema-change for real schema changes and validation, and the migration workflow runs unchanged.

- **BackupWorker** (`dbm_backup`): take automated backups via pg_dump, RDS snapshots, or Cloud SQL export before every migration, with automatic restoration on migration failure
- **MigrateWorker** (`dbm_migrate`): use Flyway or Liquibase for versioned migrations, pt-online-schema-change for zero-downtime MySQL migrations, or Alembic for Python/SQLAlchemy projects
- **ValidateSchemaWorker** (`dbm_validate_schema`): compare actual schema against expected schema (pg_dump --schema-only diff), run data integrity checks, and verify foreign key constraints
- **UpdateAppWorker** (`dbm_update_app`): update application connection strings, environment variables, or feature flags to point to the new schema version, with health check verification after the switch

Swap in Flyway or Liquibase for real migrations; the backup-migrate-validate pipeline maintains the same contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
database-migration-devops/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/databasemigrationdevops/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DatabaseMigrationDevopsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BackupWorker.java
│       ├── MigrateWorker.java
│       ├── UpdateAppWorker.java
│       └── ValidateSchemaWorker.java
└── src/test/java/databasemigrationdevops/workers/
    ├── BackupWorkerTest.java        # 7 tests
    ├── MigrateWorkerTest.java        # 7 tests
    ├── UpdateAppWorkerTest.java        # 7 tests
    └── ValidateSchemaWorkerTest.java        # 7 tests

```
