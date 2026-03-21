# Data Lake Ingestion in Java Using Conductor :  Schema Validation, Date Partitioning, Format Conversion, and Catalog Registration

A Java Conductor workflow example for data lake ingestion: validating incoming records against a schema, partitioning data by date for efficient querying, converting to an optimized storage format (Parquet, ORC, Avro), writing partitioned files to the lake path, and updating the data catalog so downstream consumers can discover the new data. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to land data into your data lake in a way that's queryable, discoverable, and doesn't corrupt existing data. That means validating incoming records against the expected schema (rejecting malformed rows before they pollute the lake), partitioning records by date so queries only scan relevant partitions, converting from raw formats (JSON, CSV) into columnar formats (Parquet, ORC) for efficient analytics, writing the converted files to the correct lake path with atomic semantics, and updating the data catalog (Hive Metastore, AWS Glue, Apache Atlas) so tools like Athena, Presto, and Spark can find the new data.

Without orchestration, you'd write a monolithic Spark job or script that does schema validation, partitioning, conversion, writing, and catalog updates in one pass. If the write fails after converting 90% of the data, you'd restart the entire job. If the catalog update fails after a successful write, the data exists in the lake but is invisible to query engines. There's no visibility into which step failed, how many records were valid, or how many partitions were created.

## The Solution

**You just write the schema validation, date partitioning, format conversion, lake writing, and catalog update workers. Conductor handles strict ordering so the catalog is updated only after successful writes, retries when lake storage is unavailable, and tracking of record counts across validate-partition-convert-write stages.**

Each stage of the ingestion pipeline is a simple, independent worker. The schema validator checks incoming records and filters out malformed rows. The partitioner organizes valid records into date-based partition keys. The format converter transforms each partition into the target columnar format. The lake writer places the converted files at the correct paths with proper naming. The catalog updater registers the new partitions so query engines can access the data. Conductor executes them in strict sequence, ensures the catalog is only updated after a successful write, retries if the lake storage is temporarily unavailable, and tracks exactly how many records were validated, partitioned, and written. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers manage the data lake ingestion pipeline: validating records against a schema, partitioning by date, converting to columnar formats like Parquet, writing to the lake path, and registering partitions in the data catalog.

| Worker | Task | What It Does |
|---|---|---|
| **ConvertFormatWorker** | `li_convert_format` | Convert Format. Computes and returns files |
| **PartitionByDateWorker** | `li_partition_by_date` | Partition By Date. Computes and returns partitions, partition count |
| **UpdateCatalogWorker** | `li_update_catalog` | Updates the catalog |
| **ValidateSchemaWorker** | `li_validate_schema` | Validate Schema. Computes and returns valid records, valid count |
| **WriteToLakeWorker** | `li_write_to_lake` | Write To Lake. Computes and returns files written, total bytes |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
li_validate_schema
    │
    ▼
li_partition_by_date
    │
    ▼
li_convert_format
    │
    ▼
li_write_to_lake
    │
    ▼
li_update_catalog

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
java -jar target/data-lake-ingestion-1.0.0.jar

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
java -jar target/data-lake-ingestion-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_lake_ingestion \
  --version 1 \
  --input '{"records": "sample-records", "lakePath": "sample-lakePath", "format": "json"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_lake_ingestion -s COMPLETED -c 5

```

## How to Extend

Validate against real Avro schemas, convert to Parquet with Snappy compression, write to S3, and register partitions in AWS Glue, the ingestion workflow runs unchanged.

- **ValidateSchemaWorker** → validate against a real Avro schema, JSON Schema, or Protobuf definition; route rejected records to a dead-letter queue
- **PartitionByDateWorker** → implement Hive-style partitioning (`year=2024/month=03/day=14`) or custom partition strategies (by region, by tenant)
- **ConvertFormatWorker** → use Apache Parquet, ORC, or Avro libraries to convert raw JSON/CSV into columnar formats with compression (Snappy, Zstd)
- **WriteToLakeWorker** → write to S3, ADLS, GCS, or HDFS with atomic rename-on-commit to prevent partial writes
- **UpdateCatalogWorker** → register new partitions in AWS Glue Data Catalog, Hive Metastore, or Apache Atlas

Swapping in real Avro schemas or writing to S3 instead of local storage requires no workflow changes, provided each worker outputs the expected partition and file metadata.

**Add new stages** by inserting tasks in `workflow.json`, for example, a deduplication step before writing, a data quality check (null rates, cardinality) after partitioning, or a notification that alerts the data team when ingestion completes.

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
data-lake-ingestion/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datalakeingestion/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataLakeIngestionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConvertFormatWorker.java
│       ├── PartitionByDateWorker.java
│       ├── UpdateCatalogWorker.java
│       ├── ValidateSchemaWorker.java
│       └── WriteToLakeWorker.java

```
