# Sequential Tasks in Java with Conductor

Sequential ETL pipeline. extract, transform, load. Three workers process data in order. Uses [Conductor](https://github.

## The Problem

You need to run an ETL pipeline where each phase strictly depends on the previous one: extract raw records from a data source, transform them by adding computed fields (grade classification, normalized scores) in a specified format, then load the transformed records into a destination system. The transform step cannot start until extraction is complete because it needs the raw data. The load step cannot start until transformation is complete because it needs the enriched records. If the load step fails after transforming 1,000 records, you need to resume from the load step. not re-extract and re-transform.

Without orchestration, you'd chain method calls in a single process. `extract()` feeds into `transform()` feeds into `load()`. If `load()` throws an exception after `transform()` completed successfully, you re-run the entire pipeline because the intermediate transformed data was only in memory. Adding retry logic to each step means nested try/catch blocks, and there is no record of what each step produced.

## The Solution

**You just write the extract, transform, and load workers. Conductor handles the sequencing, data passing, and per-step durability.**

This example demonstrates the simplest Conductor pattern. three tasks running in strict sequence. ExtractWorker takes a data source name and returns raw records. TransformWorker receives the raw data (wired via `${seq_extract_ref.output.rawData}`) and a format specification, then adds grade classifications (A/B/C) and normalized scores to each record. LoadWorker receives the transformed records (wired via `${seq_transform_ref.output.transformedData}`) and loads them into the destination, returning a count of loaded records. Conductor persists the output of each step, so if the load fails, you restart from the load step with the already-transformed data intact.

### What You Write: Workers

Three workers form the ETL sequence: ExtractWorker reads raw records from a data source, TransformWorker adds grade classifications and normalized scores, and LoadWorker writes the transformed records to the destination.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractWorker** | `seq_extract` | Extract phase of the ETL pipeline. Takes a data source name and returns hardcoded raw records. |
| **LoadWorker** | `seq_load` | Load phase of the ETL pipeline. Takes transformed data, prints each record, and returns load summary. |
| **TransformWorker** | `seq_transform` | Transform phase of the ETL pipeline. Takes raw data and a format, adds grade (A/B/C) and normalized score. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
seq_extract
    │
    ▼
seq_transform
    │
    ▼
seq_load

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
java -jar target/sequential-tasks-1.0.0.jar

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
java -jar target/sequential-tasks-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sequential_etl \
  --version 1 \
  --input '{"source": "api", "format": "json"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sequential_etl -s COMPLETED -c 5

```

## How to Extend

Connect the extractor to a real database via JDBC, add your transform rules, and load into a destination warehouse, the sequential pipeline workflow runs unchanged.

- **ExtractWorker** (`seq_extract`): query a real data source: read from a database (PostgreSQL, MySQL), pull from an API (Salesforce, HubSpot), parse a CSV/JSON file from S3, or consume messages from Kafka
- **TransformWorker** (`seq_transform`): apply real business transformations: data cleansing, field mapping, normalization, deduplication, enrichment from reference data, or format conversion (CSV to Parquet, JSON to Avro)
- **LoadWorker** (`seq_load`): write to a real destination: insert into a data warehouse (BigQuery, Redshift, Snowflake), update an Elasticsearch index, publish to a Kafka topic, or upsert into an operational database

Connecting to a real database for extraction or changing the transformation rules does not affect the three-task sequential workflow, since Conductor handles the data passing between steps via input templates.

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
sequential-tasks/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sequentialtasks/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SequentialTasksExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractWorker.java
│       ├── LoadWorker.java
│       └── TransformWorker.java
└── src/test/java/sequentialtasks/workers/
    ├── ExtractWorkerTest.java        # 6 tests
    ├── LoadWorkerTest.java        # 5 tests
    └── TransformWorkerTest.java        # 7 tests

```
