# Bulk User Import in Java Using Conductor

A Java Conductor workflow example demonstrating Bulk User Import. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to import thousands of users from a CSV or JSON file. Parsing the file into individual records, validating each record for required fields, email format, and duplicate detection, batch-inserting the valid records into your user database, and generating a summary report showing how many were imported versus rejected. Each step depends on the previous one's output.

If the parser misreads a column mapping and the validator doesn't catch it, you insert users with swapped first and last names across your entire tenant. If batch insertion fails halfway through but there's no tracking of which records succeeded, you either re-import everything (creating duplicates) or manually reconcile the partial load. Without orchestration, you'd build a monolithic import script that mixes file parsing, field validation, database batch operations, and report generation. Making it impossible to support new file formats, adjust validation rules, or retry a failed batch without re-processing the entire file.

## The Solution

**You just write the file-parsing, record-validation, batch-insertion, and reporting workers. Conductor handles the import pipeline and batch data flow.**

ParseFileWorker reads the uploaded file from the provided URL, detects the format (CSV, JSON, Excel), and extracts individual user records with their field mappings. ValidateRecordsWorker checks every record for required fields, email format validity, and constraint violations: separating 1,238 valid records from 12 invalid ones and flagging the rejection reasons. BatchInsertWorker groups the valid records into batches (13 batches for 1,238 users) and inserts them into the user database, tracking how many were successfully created. ReportWorker compiles the import summary, total parsed, valid, inserted, and failed, and generates a report URL for the admin who initiated the import. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

ParseFileWorker extracts records from CSV/JSON/Excel, ValidateRecordsWorker checks fields and detects duplicates, BatchInsertWorker loads valid users into the database, and ReportWorker compiles the import summary.

| Worker | Task | What It Does |
|---|---|---|
| **BatchInsertWorker** | `bui_batch_insert` | Inserts valid user records into the database in batches, tracking inserted count and average batch time |
| **ParseFileWorker** | `bui_parse_file` | Parses the uploaded file (CSV, JSON, Excel) from the provided URL, extracting individual user records |
| **ReportWorker** | `bui_report` | Generates an import summary report with parsed, valid, and inserted counts, returning a downloadable report URL |
| **ValidateRecordsWorker** | `bui_validate` | Validates each parsed record for required fields and duplicates, separating valid records from invalid ones with rejection reasons |

Workers simulate user lifecycle operations .  account creation, verification, profile setup ,  with realistic outputs. Replace with real identity provider and database calls and the workflow stays the same.

### The Workflow

```
bui_parse_file
    │
    ▼
bui_validate
    │
    ▼
bui_batch_insert
    │
    ▼
bui_report

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
java -jar target/bulk-user-import-1.0.0.jar

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
java -jar target/bulk-user-import-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow bui_bulk_user_import \
  --version 1 \
  --input '{"fileUrl": "https://example.com", "format": "json"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w bui_bulk_user_import -s COMPLETED -c 5

```

## How to Extend

Each worker handles one import step .  connect your file storage for CSV/JSON parsing and your user database for batch insertion, and the bulk-import workflow stays the same.

- **ParseFileWorker** (`bui_parse_file`): download the file from S3 or GCS and parse it using Apache POI (Excel), OpenCSV (CSV), or Jackson (JSON), mapping columns to your user schema
- **ValidateRecordsWorker** (`bui_validate`): check email uniqueness against your user database, validate fields against your schema constraints, and flag duplicates or policy violations with specific rejection reasons
- **BatchInsertWorker** (`bui_batch_insert`): execute batch INSERT statements against your database (PostgreSQL, MySQL) or call your identity provider's bulk API (Auth0, Okta) with configurable batch sizes and transaction boundaries
- **ReportWorker** (`bui_report`): generate the import summary as a downloadable CSV or PDF, store it in your document service, and email the report link to the admin who initiated the import

Swap in real JDBC batch operations and the parse-validate-insert-report import pipeline continues to handle thousands of users without workflow changes.

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
bulk-user-import/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/bulkuserimport/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BulkUserImportExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BatchInsertWorker.java
│       ├── ParseFileWorker.java
│       ├── ReportWorker.java
│       └── ValidateRecordsWorker.java
└── src/test/java/bulkuserimport/workers/
    ├── BatchInsertWorkerTest.java        # 2 tests
    ├── ParseFileWorkerTest.java        # 2 tests
    ├── ReportWorkerTest.java        # 2 tests
    └── ValidateRecordsWorkerTest.java        # 3 tests

```
