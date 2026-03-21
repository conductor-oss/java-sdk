# External Payload in Java with Conductor

External payload storage. generate a summary and storage reference instead of returning large data, then process the summary. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to pass large data between workflow tasks. a report with millions of rows, a dataset for ML training, or a full database export; but Conductor's task output has a size limit. Storing megabytes of raw data directly in task output would bloat the workflow execution record, slow down the Conductor server, and eventually hit payload size limits. The downstream task only needs a summary and a pointer to the full data, not the data itself.

Without orchestration, you'd write the large payload to S3 or a file share, pass the reference manually between functions, handle the case where the upload succeeds but the reference never reaches the consumer, and manage cleanup of orphaned files when processing fails. If the process crashes between writing the data and recording the reference, the data is stranded with no way to locate it.

## The Solution

**You just write the payload generation and summary processing workers. Conductor handles the lightweight reference passing without storing bulk data.**

This example demonstrates the external payload storage pattern. keeping large data out of Conductor's task output. The GenerateWorker produces the large dataset but instead of returning it inline, it writes the data to external storage (S3, GCS, or a file system) and returns only a lightweight summary and a `storageRef` pointer. The downstream ProcessWorker receives just the summary to make routing decisions, and can fetch the full data from the storage reference when needed. Conductor tracks the summary and reference without ever storing the bulk payload, keeping workflow executions fast and the server lean.

### What You Write: Workers

Two workers demonstrate the external payload pattern: GenerateWorker produces a large dataset and returns only a lightweight summary with a storage reference, while ProcessWorker consumes that summary for downstream decisions.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateWorker** | `ep_generate` | Generates a summary and storage reference for a large payload. Instead of returning the full data (which could exceed... |
| **ProcessWorker** | `ep_process` | Processes the summary from the generate step. In a real system, this worker could also fetch the full data from the s... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
ep_generate
    │
    ▼
ep_process

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
java -jar target/external-payload-1.0.0.jar

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
java -jar target/external-payload-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow large_payload_demo \
  --version 1 \
  --input '{"dataSize": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w large_payload_demo -s COMPLETED -c 5

```

## How to Extend

Replace the demo data generation with a real large-payload writer to S3 or GCS, and the external payload storage pattern works unchanged.

- **GenerateWorker** (`ep_generate`): run the actual data generation (database export, API bulk fetch, log aggregation), upload the result to S3/GCS/Azure Blob using the AWS/GCP SDK, and return the storage URI, byte count, and row count as the summary
- **ProcessWorker** (`ep_process`): fetch the full data from the storage reference, process it (parse CSV, transform JSON, run analytics), and write results to a data warehouse, Elasticsearch index, or downstream queue

Uploading to real S3 or GCS storage instead of returning demo references does not change the workflow, as long as the summary and storageRef fields are returned in the expected format.

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
external-payload/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/externalpayload/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ExternalPayloadExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateWorker.java
│       └── ProcessWorker.java
└── src/test/java/externalpayload/workers/
    ├── GenerateWorkerTest.java        # 8 tests
    └── ProcessWorkerTest.java        # 6 tests

```
