# Claim Check Pattern in Java Using Conductor :  Offload Large Payloads, Pass by Reference

A Java Conductor workflow example for the claim check pattern .  storing a large payload (images, documents, sensor data) in external storage, passing a lightweight reference through the workflow pipeline, then retrieving and processing the full payload only when needed. Uses [Conductor](https://github.

## Keeping Large Payloads Out of Your Message Bus

Workflow tasks exchange data through their inputs and outputs, but passing a 50 MB medical image or a 200 MB CSV dataset directly between tasks bloats every message, slows serialization, and can hit broker size limits. The claim check pattern solves this: store the large payload in blob storage, pass a small reference ID (the "claim check") through the pipeline, and retrieve the full data only in the task that actually needs it.

The challenge is coordinating the store-reference-retrieve lifecycle reliably. If the store succeeds but the reference gets lost, the payload is orphaned. If the retrieve fails, you need to retry without re-storing. And you need to track the payload size and storage location for debugging without actually passing the payload through the orchestrator.

## The Solution

**You write the storage and retrieval logic. Conductor handles the reference passing, retries, and payload lineage.**

`StorePayloadWorker` writes the large payload to external storage and returns a lightweight claim check ID plus the storage location and payload size in bytes. `PassReferenceWorker` forwards just the claim check ID through the pipeline .  no bulky data, just a pointer. `RetrieveWorker` fetches the full payload from storage using the claim check reference. `ProcessWorker` operates on the retrieved data (e.g., computing metric averages). Conductor ensures the chain executes in order, retries any failed retrieve or store operation, and records the claim check ID at every step so you can trace payload lineage.

### What You Write: Workers

Four workers manage the store-reference-retrieve lifecycle: payload storage, reference passing, retrieval, and processing, keeping large data out of the orchestration bus.

| Worker | Task | What It Does |
|---|---|---|
| **PassReferenceWorker** | `clc_pass_reference` | Passes the lightweight claim check reference through the pipeline. |
| **ProcessWorker** | `clc_process` | Processes the retrieved payload and computes metric averages. |
| **RetrieveWorker** | `clc_retrieve` | Retrieves the full payload from storage using the claim check reference. |
| **StorePayloadWorker** | `clc_store_payload` | Stores a large payload in external storage and returns a lightweight claim check reference. |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### The Workflow

```
clc_store_payload
    │
    ▼
clc_pass_reference
    │
    ▼
clc_retrieve
    │
    ▼
clc_process

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
java -jar target/claim-check-1.0.0.jar

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
java -jar target/claim-check-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow clc_claim_check \
  --version 1 \
  --input '{"payload": {"key": "value"}, "storageType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w clc_claim_check -s COMPLETED -c 5

```

## How to Extend

Each worker handles one part of the store-reference-retrieve lifecycle .  replace the simulated blob storage calls with real S3 or Azure Blob APIs and the claim check pipeline runs unchanged.

- **StorePayloadWorker** (`clc_store_payload`): upload to S3 (`s3.putObject()`), Azure Blob Storage, or GCS and return the object key as the claim check ID
- **RetrieveWorker** (`clc_retrieve`): download from S3 (`s3.getObject()`), generate a presigned URL, or stream the payload directly to the processing step
- **ProcessWorker** (`clc_process`): run real processing on the retrieved data: image thumbnail generation with ImageMagick, CSV aggregation with Apache Commons CSV, or PDF text extraction with Apache Tika

The claim check reference contract stays fixed. Swap simulated blob storage for real S3 or Azure Blob and the reference-passing pipeline runs unchanged.

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
claim-check/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/claimcheck/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ClaimCheckExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PassReferenceWorker.java
│       ├── ProcessWorker.java
│       ├── RetrieveWorker.java
│       └── StorePayloadWorker.java
└── src/test/java/claimcheck/workers/
    ├── PassReferenceWorkerTest.java        # 8 tests
    ├── ProcessWorkerTest.java        # 8 tests
    ├── RetrieveWorkerTest.java        # 8 tests
    └── StorePayloadWorkerTest.java        # 8 tests

```
