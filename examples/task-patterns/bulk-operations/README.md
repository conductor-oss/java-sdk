# Bulk Operations in Java with Conductor

Bulk operations demo. two-step workflow used for bulk start, pause, resume, and terminate. Uses [Conductor](https://github.

## The Problem

You need to manage hundreds or thousands of workflow instances at once. starting a batch of data processing jobs, pausing them while a dependent system is down, resuming them when it recovers, or terminating stale runs. Each batch is identified by a batchId, and each instance runs a two-step pipeline (step1 produces intermediate data, step2 produces the final result). Operating on workflows one at a time through the UI is impractical at scale.

Without Conductor's bulk operations API, you'd build custom scripts that loop through workflow IDs, call start/pause/resume/terminate individually, handle partial failures when some calls succeed and others don't, and track which instances are in which state. That code is fragile, hard to test, and impossible to observe across thousands of concurrent instances.

## The Solution

**You just write the step workers. Conductor handles bulk lifecycle management across all instances.**

This example demonstrates Conductor's bulk operations API for managing workflow instances at scale. The simple two-step workflow serves as the target for bulk start (launching many instances at once), bulk pause (suspending in-flight workflows), bulk resume (continuing paused workflows), and bulk terminate (canceling running workflows). Conductor tracks each instance's state independently, so a partial failure in one batch doesn't affect the others.

### What You Write: Workers

Two step workers form a minimal pipeline that serves as the target for Conductor's bulk lifecycle APIs. Start, pause, resume, and terminate across hundreds of instances at once.

| Worker | Task | What It Does |
|---|---|---|
| **Step1Worker** | `bulk_step1` | First step in the bulk operations workflow. Takes a batchId and returns intermediate data for the batch. |
| **Step2Worker** | `bulk_step2` | Second step in the bulk operations workflow. Takes intermediate data from Step1 and produces the final result. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
bulk_step1
    │
    ▼
bulk_step2

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
java -jar target/bulk-operations-1.0.0.jar

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
java -jar target/bulk-operations-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow bulk_ops_demo \
  --version 1 \
  --input '{"batchId": "TEST-001"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w bulk_ops_demo -s COMPLETED -c 5

```

## How to Extend

Replace the two-step simulation with your real processing logic, the bulk start, pause, resume, and terminate APIs work identically regardless of what the workers do.

- **Step1Worker** (`bulk_step1`): replace with real batch initialization logic (e.g., query a database for records in the batch, validate input data, set up processing context)
- **Step2Worker** (`bulk_step2`): replace with real batch finalization logic (e.g., write results to a data warehouse, send completion notifications, update batch status in your job tracker)

Replacing the step workers with real batch processing logic leaves the bulk operation APIs working identically, as long as each step returns the expected intermediate and final outputs.

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
bulk-operations/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/bulkoperations/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BulkOperationsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── Step1Worker.java
│       └── Step2Worker.java
└── src/test/java/bulkoperations/workers/
    ├── Step1WorkerTest.java        # 6 tests
    └── Step2WorkerTest.java        # 6 tests

```
