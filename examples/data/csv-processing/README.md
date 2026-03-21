# CSV Processing in Java Using Conductor: Parsing, Validation, Transformation, and Clean Output

A partner sends you a 500MB CSV of customer records every Monday. Your API endpoint reads the whole file into memory, validates every row, transforms field formats, and writes clean output. all in a single synchronous request handler. At row 85,000, a salary field contains "N/A" instead of a number, `Double.parseDouble` throws, and the entire upload fails. The partner re-uploads. This time it gets to row 200,000 before your JVM runs out of heap and the API server crashes, taking down every other endpoint with it. You have no idea which rows were valid, which were rejected, or where the process actually failed, just a 502 in the partner's browser and an OOM in your logs. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You receive CSV files from partners, exports, or internal systems and need to turn them into clean, validated, normalized records. That means parsing raw text with configurable delimiters and header detection, rejecting rows with missing names or malformed emails, normalizing fields (trimming whitespace, standardizing case, parsing salary strings into numbers), and producing a final output with counts of what was accepted and rejected. Each step depends on the previous one. You can't validate without parsing first, and you can't transform rows that haven't been validated.

Without orchestration, you'd build a single class that reads the CSV, runs validation inline, transforms in the same pass, and writes output at the end. If the transformation logic throws an exception on an unexpected salary format, the entire file fails. There's no way to see which step rejected a row, no automatic retry if an upstream data source is temporarily unavailable, and adding a new validation rule or transformation means modifying tightly coupled code.

## The Solution

**You just write the CSV parsing, row validation, field transformation, and output generation workers. Conductor handles row-level tracking across parse-validate-transform stages, automatic retries, and visibility into accept/reject counts at each step.**

Each stage of the CSV pipeline is a simple, independent worker. The parser splits raw CSV text into structured rows using the configured delimiter and header mode. The validator checks each row for required fields and format constraints (non-empty name, valid email). The transformer normalizes field values. Title-casing names, lowercasing emails, uppercasing departments, parsing salary strings into doubles. The output generator assembles the clean records and computes summary statistics. Conductor executes them in sequence, passes rows between steps, retries if a step fails, and tracks exactly how many rows were parsed, validated, and transformed. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle the CSV lifecycle: parsing raw text into rows, validating names and emails, normalizing field casing and types, and producing clean output with summary statistics.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateOutputWorker** | `cv_generate_output` | Generates final output with summary statistics from transformed rows. |
| **ParseCsvWorker** | `cv_parse_csv` | Parses a CSV string into structured rows. |
| **TransformFieldsWorker** | `cv_transform_fields` | Transforms validated rows: normalizes names, lowercases emails, uppercases departments, parses salaries to doubles. |
| **ValidateRowsWorker** | `cv_validate_rows` | Validates parsed CSV rows: each row must have a non-empty name and an email containing "@". |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### The Workflow

```
cv_parse_csv
    │
    ▼
cv_validate_rows
    │
    ▼
cv_transform_fields
    │
    ▼
cv_generate_output

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
java -jar target/csv-processing-1.0.0.jar

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
java -jar target/csv-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow csv_processing \
  --version 1 \
  --input '{"csvData": "name,email,dept,salary\nAlice,alice@corp.com,engineering,95000\nBob,bob@corp.com,sales,82000\n,invalid-email,hr,70000\nDiana,diana@corp.com,engineering,105000", "delimiter": ",", "hasHeader": true}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w csv_processing -s COMPLETED -c 5

```

## How to Extend

Swap the parser for Apache Commons CSV reading from S3 or SFTP, add domain-specific validation rules, and the pipeline workflow runs unchanged.

- **ParseCsvWorker** → read CSV from S3, SFTP, or a database CLOB column instead of inline string input; add support for quoted fields and multi-line values using Apache Commons CSV or OpenCSV
- **ValidateRowsWorker** → add domain-specific validation rules (phone number format, zip code lookups, email domain verification via DNS MX check)
- **TransformFieldsWorker** → enrich rows with external data (geocode addresses, resolve company names to CRM IDs, convert currencies)
- **GenerateOutputWorker** → write clean records to a database table, S3 as Parquet, or push to a downstream API; email a summary report with accept/reject counts

The pipeline continues to function as long as each worker outputs the expected row structure, regardless of which CSV library or validation engine is used internally.

**Add new pipeline stages** by inserting a task in `workflow.json`, for example, deduplication before validation, schema inference from headers, or a quarantine step that routes rejected rows to a review queue.

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
csv-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/csvprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CsvProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateOutputWorker.java
│       ├── ParseCsvWorker.java
│       ├── TransformFieldsWorker.java
│       └── ValidateRowsWorker.java
└── src/test/java/csvprocessing/workers/
    ├── GenerateOutputWorkerTest.java        # 8 tests
    ├── ParseCsvWorkerTest.java        # 10 tests
    ├── TransformFieldsWorkerTest.java        # 9 tests
    └── ValidateRowsWorkerTest.java        # 9 tests

```
