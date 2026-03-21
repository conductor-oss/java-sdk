# ETL Basics in Java Using Conductor: Extract, Transform, Validate, Load, and Confirm

Your company's data lives in 3 databases, 2 third-party APIs, and a shared Google Drive folder that someone updates manually on Fridays. The analyst's "ETL pipeline" is a Python script on their laptop that connects to each source, munges the data with pandas, and writes to the warehouse. It works great: until the VPN disconnects at row 50,000, or the analyst goes on vacation and nobody else can run it, or the Google Drive CSV changes column order and the script silently loads first names into the email field. There's no retry, no validation, and no record of what was loaded when. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate a durable ETL pipeline, extract, transform, validate, load, and confirm, as independent workers with automatic retries and per-stage tracking.

## The Problem

You need to move data from a source system to a destination, but the data needs cleaning along the way. Names have extra whitespace. Emails are inconsistently cased. Amounts are stored as strings that need parsing to numbers. Some records are incomplete and shouldn't be loaded at all. You need a pipeline that extracts, transforms, validates, loads, and confirms, with each step depending on the previous one's output. If the load fails after transforming 10,000 records, you shouldn't have to re-extract and re-transform from scratch.

Without orchestration, you'd write a single ETL method that connects to the source, reads records, transforms inline, validates inline, writes to the destination, and hopes nothing crashes. If the destination database is briefly unavailable, the entire pipeline fails with no retry. There's no visibility into how many records were extracted vs: transformed vs: validated vs: loaded, and debugging a data quality issue means adding println statements throughout coupled code.

## The Solution

**You just write the extract, transform, validate, load, and confirm workers. Conductor handles the extract-transform-validate-load-confirm sequence, retries when the destination is unavailable, and per-stage record count tracking.**

Each stage of the ETL pipeline is a simple, independent worker. The extractor reads records from the source. The transformer cleans and normalizes fields (trimming, lowercasing, type conversion). The validator filters out incomplete or invalid records. The loader writes clean records to the destination. The confirmer verifies the load completed successfully. Conductor executes them in sequence, passes records between stages, retries if the destination is unavailable, and tracks exactly how many records survived each stage. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers form the classic ETL pipeline: extracting records from a source, transforming them (trimming names, lowercasing emails, parsing amounts), validating output quality, loading into the destination, and confirming successful completion.

| Worker | Task | What It Does |
|---|---|---|
| `ExtractDataWorker` | `el_extract_data` | Reads 4 customer records from the source, each with untrimmed names, uppercase emails, and string amounts |
| `TransformDataWorker` | `el_transform_data` | Cleans records: trims whitespace from names, lowercases emails, parses amount strings to doubles |
| `ValidateOutputWorker` | `el_validate_output` | Filters out records with empty name, empty email, or amount <= 0 (e.g., Charlie with amount 0.00) |
| `LoadDataWorker` | `el_load_data` | Writes the validated records to the destination and returns a loaded count |
| `ConfirmLoadWorker` | `el_confirm_load` | Confirms the load completed by returning status `ETL_COMPLETE` with the final loaded count |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### The Workflow

```
el_extract_data
    │
    ▼
el_transform_data
    │
    ▼
el_validate_output
    │
    ▼
el_load_data
    │
    ▼
el_confirm_load

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
java -jar target/etl-basics-1.0.0.jar

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
java -jar target/etl-basics-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow etl_basics_wf \
  --version 1 \
  --input '{"jsonData": "[{\"id\":1,\"name\":\"Alice\",\"email\":\"ALICE@EXAMPLE.COM\"},{\"id\":2,\"name\":\"Bob\",\"email\":\"bob@test.org\"}]", "destination": "analytics-warehouse", "rules": {"trimNames": true, "lowercaseEmails": true}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w etl_basics_wf -s COMPLETED -c 5

```

## How to Extend

Connect the extractor to a real PostgreSQL or Salesforce source, add custom transform rules, and load into your data warehouse, the ETL workflow runs unchanged.

- **`ExtractDataWorker`**: Connect to a real source (PostgreSQL, MySQL, REST API, S3 CSV, Salesforce export) via JDBC or HTTP.

- **`TransformDataWorker`**: Add custom transformation rules (currency conversion, date format normalization, field derivation, lookup enrichment).

- **`ValidateOutputWorker`**: Implement domain-specific validation (business rule checks, schema conformance, referential integrity).

- **`LoadDataWorker`**: Write to a real destination (data warehouse, database, API endpoint, message queue) with batch insert for performance.

- **`ConfirmLoadWorker`**: Run count verification between source and destination, send a Slack notification, or trigger downstream pipelines.

Connecting the extractor to a real database or replacing the transformer with production cleaning logic does not require workflow changes, as long as each worker returns records in the expected shape.

**Add new stages** by inserting tasks in `workflow.json`, for example, a deduplication step before loading, a data quality score computation, or an archival step that backs up the source data after successful load.

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
etl-basics/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/etlbasics/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EtlBasicsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConfirmLoadWorker.java
│       ├── ExtractDataWorker.java
│       ├── LoadDataWorker.java
│       ├── TransformDataWorker.java
│       └── ValidateOutputWorker.java
└── src/test/java/etlbasics/workers/
    ├── ConfirmLoadWorkerTest.java
    ├── ExtractDataWorkerTest.java
    ├── LoadDataWorkerTest.java
    ├── TransformDataWorkerTest.java
    └── ValidateOutputWorkerTest.java

```
