# Data Validation in Java Using Conductor :  Required Fields, Type Checking, Range Validation, and Reporting

A Java Conductor workflow example for multi-layer data validation: loading records, checking that required fields (name, email, age) are present and non-empty, verifying data types (name is String, age is Number), validating value ranges (age between 0-150, email contains "@"), and generating a validation report with error counts per category and the number of records that passed all checks. Uses [Conductor](https://github.

## The Problem

You receive data from external partners, user submissions, or upstream systems, and you need to validate it before it enters your production database. Validation is layered: first check that required fields exist (a record without an email is immediately invalid), then verify data types (age must be a number, not a string), then validate value ranges (age must be 0-150, email must contain "@"). Each layer filters out invalid records, so type checking only runs on records that passed required field checks. You need a report showing exactly how many records failed at each stage and why.

Without orchestration, you'd write a single validation method with nested if/else blocks. There's no visibility into which validation layer rejected a record. If you want to add a new layer (uniqueness check, cross-field validation), you'd modify deeply coupled code. If the process crashes after two of three checks, you'd restart from scratch.

## The Solution

**You just write the required-field, type-checking, range-validation, and reporting workers. Conductor handles sequential validation layer execution, per-layer retry on failure, and precise tracking of how many records were rejected at each stage.**

Each validation layer is a simple, independent worker. The required fields checker filters out records missing mandatory fields. The type checker verifies data types on the surviving records. The range validator checks value constraints. The report generator tallies errors per layer and counts the records that passed all checks. Conductor executes them in sequence, passes only valid records from one layer to the next, retries if a check fails, and tracks exactly how many records were rejected at each stage. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers implement layered validation: loading records, checking required fields are present, verifying data types (name is String, age is Number), validating value ranges (age 0-150, email contains @), and generating an error report by category.

| Worker | Task | What It Does |
|---|---|---|
| **CheckRangesWorker** | `vd_check_ranges` | Checks value ranges: age must be 0-150, email must contain "@". |
| **CheckRequiredWorker** | `vd_check_required` | Checks that required fields (name, email, age) are present and non-empty. |
| **CheckTypesWorker** | `vd_check_types` | Checks that field types are correct: name must be String, age must be Number. |
| **GenerateReportWorker** | `vd_generate_report` | Generates a validation summary report. |
| **LoadRecordsWorker** | `vd_load_records` | Loads records for validation. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
vd_load_records
    │
    ▼
vd_check_required
    │
    ▼
vd_check_types
    │
    ▼
vd_check_ranges
    │
    ▼
vd_generate_report

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
java -jar target/data-validation-1.0.0.jar

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
java -jar target/data-validation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_validation \
  --version 1 \
  --input '{"records": "sample-records", "schema": "sample-schema"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_validation -s COMPLETED -c 5

```

## How to Extend

Validate against JSON Schema or Protobuf definitions, add business rules and cross-field constraints, and the multi-layer validation workflow runs unchanged.

- **LoadRecordsWorker** → read records from a database, API, file upload, or message queue
- **CheckRequiredWorker** → make the required fields configurable via the schema input; support conditional requirements (field X is required only if field Y has value Z)
- **CheckTypesWorker** → validate against JSON Schema, Avro, or Protobuf type definitions; detect type coercion opportunities (string "42" to integer 42)
- **CheckRangesWorker** → add regex pattern validation, enum value checks, date range constraints, and cross-field rules (startDate must be before endDate)
- **GenerateReportWorker** → write validation reports to a dashboard, send failure notifications, or create quarantine tickets for records that need manual review

Adding new validation rules or tightening range constraints inside any worker does not alter the multi-layer pipeline, as long as each outputs its valid/invalid record sets and error counts consistently.

**Add new validation layers** by inserting tasks in `workflow.json`, for example, a uniqueness check, a referential integrity validator (does the referenced foreign key exist?), or a business rule validator (order total must equal sum of line items).

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
data-validation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datavalidation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataValidationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckRangesWorker.java
│       ├── CheckRequiredWorker.java
│       ├── CheckTypesWorker.java
│       ├── GenerateReportWorker.java
│       └── LoadRecordsWorker.java
└── src/test/java/datavalidation/workers/
    ├── CheckRangesWorkerTest.java        # 10 tests
    ├── CheckRequiredWorkerTest.java        # 10 tests
    ├── CheckTypesWorkerTest.java        # 9 tests
    ├── GenerateReportWorkerTest.java        # 9 tests
    └── LoadRecordsWorkerTest.java        # 8 tests

```
