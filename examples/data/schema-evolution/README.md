# Schema Evolution in Java Using Conductor :  Change Detection, Transform Generation, Data Migration, and Validation

A Java Conductor workflow example for schema evolution. comparing a current schema against a target schema to detect changes (added fields, removed fields, type changes, renamed columns), generating the transform operations needed to migrate data from the current schema to the target, applying those transforms to existing data, and validating that the transformed data conforms to the target schema with the expected compatibility level. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Your database schema needs to evolve, a new `phone_number` column is being added, the `address` field is being split into `street`, `city`, and `zip`, and the `status` column is changing from a string to an integer enum. You need to detect exactly what changed between the current and target schemas, generate the right migration transforms (ALTER TABLE for additions, data conversion for type changes, column splitting for restructured fields), apply those transforms to the existing data, and validate that every record in the migrated table conforms to the target schema. If you apply the transforms but skip validation, you might discover weeks later that 2% of records have null values in the new required `zip` field because the address splitting logic didn't handle PO boxes.

Without orchestration, you'd write a single migration script that diffs schemas, generates SQL, runs it, and hopes for the best. If the data transformation fails on row 50,000 of 100,000, the table is left in an inconsistent state with no record of which transforms succeeded. There's no visibility into how many changes were detected, what transform operations were generated, or whether the compatibility level is backward-compatible or breaking. Rolling back means restoring from a backup and hoping the backup is recent.

## The Solution

**You just write the change detection, transform generation, data migration, and schema validation workers. Conductor handles strict ordering so transforms apply only after a valid migration plan exists, retries when databases are temporarily unavailable, and tracking of change counts, transform counts, and compatibility levels at every stage.**

Each stage of the schema evolution pipeline is a simple, independent worker. The change detector compares the current and target schemas field by field, identifying additions, removals, type changes, and renames, and classifying each change's compatibility impact. The transform generator converts those detected changes into concrete transform operations. ADD_COLUMN for new fields, TYPE_CAST for type changes, SPLIT for field restructuring. Producing a migration plan. The transform applier executes the migration plan against the sample data, applying each transform in sequence and tracking how many records were successfully transformed. The schema validator checks every transformed record against the target schema, confirming all required fields are present, types match, and constraints are satisfied, and reports the overall compatibility level (backward, forward, full, or breaking). Conductor executes them in strict sequence, ensures transforms only apply after change detection generates a valid plan, retries if the database is temporarily unavailable, and tracks change counts, transform counts, and validation results at every stage. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage schema evolution: comparing current and target schemas to detect changes, generating concrete transform operations for each change, applying those transforms to existing data, and validating that transformed records conform to the target schema.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyTransformWorker** | `sh_apply_transform` | Applies schema transforms to sample data. |
| **DetectChangesWorker** | `sh_detect_changes` | Detects schema changes between current and target schemas. |
| **GenerateTransformWorker** | `sh_generate_transform` | Generates transform operations from detected schema changes. |
| **ValidateSchemaWorker** | `sh_validate_schema` | Validates transformed data against the target schema. |

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
sh_detect_changes
    │
    ▼
sh_generate_transform
    │
    ▼
sh_apply_transform
    │
    ▼
sh_validate_schema
```

## Example Output

```
=== Schema Evolution Workflow Demo ===

Step 1: Registering task definitions...
  Registered: sh_detect_changes, sh_generate_transform, sh_apply_transform, sh_validate_schema

Step 2: Registering workflow 'schema_evolution'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [apply] Transformed
  [detect] Found
  [generate] Generated
  [validate] Validation

  Status: COMPLETED
  Output: {middle_name=..., phone_number=..., age=..., transformedData=...}

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
java -jar target/schema-evolution-1.0.0.jar
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
java -jar target/schema-evolution-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow schema_evolution \
  --version 1 \
  --input '{"currentSchema": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w schema_evolution -s COMPLETED -c 5
```

## How to Extend

Compare real Avro or Protobuf schemas, generate ALTER TABLE migrations, and validate transformed data against the target schema, the schema evolution workflow runs unchanged.

- **DetectChangesWorker** → compare real schemas: diff Avro schema versions in a Confluent Schema Registry, compare PostgreSQL `information_schema.columns` between environments, or parse Protobuf `.proto` file revisions to detect field additions, removals, and type changes
- **GenerateTransformWorker** → generate real migration operations: Flyway/Liquibase migration scripts for SQL databases, Avro schema evolution rules for Kafka topics, or custom ETL transforms for data warehouse column restructuring
- **ApplyTransformWorker** → execute real migrations: run ALTER TABLE statements via JDBC, apply Avro schema evolution to Kafka consumers, execute Snowflake `ALTER TABLE ... ADD COLUMN` with default value backfill, or run dbt model changes
- **ValidateSchemaWorker** → validate against real schema registries: Confluent Schema Registry compatibility checks (BACKWARD, FORWARD, FULL), JSON Schema validation with ajv, or custom constraint validators that check referential integrity after migration

Adding new change types (column splits, constraint additions) or connecting to real migration tooling does not affect the detect-generate-apply-validate pipeline, as long as each worker outputs the expected change and transform structures.

**Add new stages** by inserting tasks in `workflow.json`, for example, a backup step that snapshots the current data before migration, a dry-run step that applies transforms to a sample before touching production, or a rollback step that reverts the migration if validation fails.

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
schema-evolution/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/schemaevolution/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SchemaEvolutionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyTransformWorker.java
│       ├── DetectChangesWorker.java
│       ├── GenerateTransformWorker.java
│       └── ValidateSchemaWorker.java
└── src/test/java/schemaevolution/workers/
    ├── ApplyTransformWorkerTest.java        # 3 tests
    ├── DetectChangesWorkerTest.java        # 3 tests
    ├── GenerateTransformWorkerTest.java        # 4 tests
    └── ValidateSchemaWorkerTest.java        # 5 tests
```
