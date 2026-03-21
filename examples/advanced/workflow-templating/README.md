# ETL Workflow Templating in Java Using Conductor :  Extract, Transform, Load, Verify

A Java Conductor workflow example for ETL workflow templating .  extracting data from a source (database, API, file), transforming it according to configurable rules, loading it into a destination (data warehouse, database), and verifying the load was successful. The template is reusable for any ETL job ,  swap the extract/transform/load workers for different sources and destinations. Uses [Conductor](https://github.

## Every ETL Job Is Extract-Transform-Load, but the Details Differ

Your company runs 50 ETL jobs. Each extracts from a different source (PostgreSQL, Salesforce API, S3 CSV files), transforms differently (currency conversion, deduplication, schema mapping), and loads to a different destination (Snowflake, Redshift, BigQuery). But the structure is always the same: extract, transform, load, verify. Without a template, each ETL job is built from scratch, duplicating retry logic, error handling, and verification.

Workflow templating means defining the ETL structure once (extract-transform-load-verify) and swapping in different worker implementations for different jobs. The template handles the orchestration .  retries, failure routing, observability ,  while each job's workers handle the source-specific extraction, job-specific transformation, and destination-specific loading.

## The Solution

**You write the extract, transform, and load logic. Conductor handles the ETL template, batch retries, and verification tracking.**

`WtmExtractWorker` pulls data from the source system for the specified batch. `WtmTransformWorker` applies the configured transformation rules .  cleaning, mapping, enriching. `WtmLoadWorker` writes the transformed data to the destination. `WtmVerifyWorker` confirms the load succeeded ,  checking row counts, running data quality assertions, and validating that the destination has the expected data. Conductor runs this template for every batch, retries failed extracts or loads, and records the row counts and verification results for every ETL run.

### What You Write: Workers

Four workers implement the reusable ETL template: source extraction, rule-based transformation, destination loading, and row-count verification, each swappable for different source/destination combinations.

| Worker | Task | What It Does |
|---|---|---|
| **WtmExtractWorker** | `wtm_extract` | Extracts records from the configured source and reports the record count |
| **WtmLoadWorker** | `wtm_load` | Loads the transformed records into the target destination |
| **WtmTransformWorker** | `wtm_transform` | Applies the template's transformation rules to the extracted data |
| **WtmVerifyWorker** | `wtm_verify` | Verifies that extracted and loaded record counts match for data integrity |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
wtm_extract
    │
    ▼
wtm_transform
    │
    ▼
wtm_load
    │
    ▼
wtm_verify

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
java -jar target/workflow-templating-1.0.0.jar

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
java -jar target/workflow-templating-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow wtm_etl_postgres_demo \
  --version 1 \
  --input '{"batchId": "TEST-001", "options": "sample-options"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w wtm_etl_postgres_demo -s COMPLETED -c 5

```

## How to Extend

Each worker implements one ETL stage .  replace the simulated extracts and loads with real database connectors or data warehouse APIs and the extract-transform-load-verify template runs unchanged.

- **WtmExtractWorker** (`wtm_extract`): connect to real data sources: JDBC queries against PostgreSQL/MySQL, Salesforce SOQL via the REST API, S3 `getObject()` for CSV/Parquet files, or Kafka consumer for streaming ETL
- **WtmTransformWorker** (`wtm_transform`): apply real transformations: Apache Spark for large-scale transforms, dbt models for SQL-based transformations, or custom Jackson/JOLT mappings for JSON restructuring
- **WtmLoadWorker** (`wtm_load`): write to real destinations: Snowflake `COPY INTO` via JDBC, BigQuery `insertAll()`, Redshift `COPY` from S3 staging, or Elasticsearch bulk index API

The extract-transform-load interface stays fixed. Swap the simulated source for a real PostgreSQL or Salesforce connector and the template runs the same verification step unchanged.

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
workflow-templating/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/workflowtemplating/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WorkflowTemplatingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── WtmExtractWorker.java
│       ├── WtmLoadWorker.java
│       ├── WtmTransformWorker.java
│       └── WtmVerifyWorker.java
└── src/test/java/workflowtemplating/workers/
    ├── WtmExtractWorkerTest.java        # 4 tests
    ├── WtmLoadWorkerTest.java        # 4 tests
    ├── WtmTransformWorkerTest.java        # 4 tests
    └── WtmVerifyWorkerTest.java        # 4 tests

```
