# Batch Processing in Java Using Conductor: Chunked Record Processing with DO_WHILE Loops

Your nightly ETL job processes 10 million rows from the transactions database. At row 8.7 million, the destination warehouse hiccups, a brief network partition, maybe 3 seconds. The job crashes. There's no checkpoint, so it restarts from row 1. Every night. Some nights it finishes by 6 AM; other nights the same transient failure at row 6M or 9M means the warehouse team arrives to stale data and a failed job notification. Nobody trusts the "last updated" timestamp anymore. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate batch processing with DO_WHILE loops that checkpoint progress, so a failure at batch 47 retries batch 47, not the entire job.

## The Problem

You have thousands (or millions) of records to process. Database rows to transform, files to convert, API calls to make, and you can't do them all at once. You need to split the work into manageable batches, process each batch sequentially, track progress across iterations, and produce a final summary. If batch 47 out of 200 fails, you need to retry just that batch, not start over from scratch.

Without orchestration, you'd write a `for` loop with manual batch slicing, wrap it in try/catch for retries, build your own progress tracking, and hope the process doesn't crash at batch 199 of 200. There's no visibility into which batch is running, no automatic recovery if the JVM dies mid-processing, and changing the batch size or adding a validation step means rewriting the loop logic.

## The Solution

**You just write the batch preparation, per-batch processing, and summarization workers. Conductor handles DO_WHILE iteration, per-batch retry on failure, and crash recovery that resumes from the exact batch where processing stopped.**

Each concern is a simple, independent worker. The batch preparation worker splits records into chunks based on the configured batch size. The processing worker handles a single batch. It receives a batch index, computes the record range, and processes those records. The summarizer aggregates results across all batches. Conductor's `DO_WHILE` loop drives the iteration, automatically advancing the batch index, retrying any batch that fails, and resuming from the exact batch where it left off if the process crashes. You get all of that, without writing a single line of loop management code.

### What You Write: Workers

Three workers divide batch processing into distinct responsibilities: splitting records into chunks, processing one chunk per loop iteration, and summarizing the results across all batches.

| Worker | Task | What It Does |
|---|---|---|
| `PrepareBatchesWorker` | `bp_prepare_batches` | Splits input records into chunks based on `batchSize`, computing total records (10) and total batches (4) |
| `ProcessBatchWorker` | `bp_process_batch` | Processes one batch per DO_WHILE iteration. Computes record range (e.g., records 1-3) and returns count |
| `SummarizeWorker` | `bp_summarize` | Aggregates results across all batches into a summary like "10 records in 4 batches" |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### The Workflow

```
bp_prepare_batches
    │
    ▼
DO_WHILE
    └── bp_process_batch
    │
    ▼
bp_summarize

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
java -jar target/batch-processing-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

### Sample Output

```
=== Batch Processing Demo ===

Step 1: Registering task definitions...
  Registered: bp_prepare_batches, bp_process_batch, bp_summarize

Step 2: Registering workflow 'batch_processing'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...

  [prepare] 10 records -> 4 batches of 3
  [batch] Iteration 1: processing records 1-3 (3 records)
  [batch] Iteration 2: processing records 4-6 (3 records)
  [batch] Iteration 3: processing records 7-9 (3 records)
  [batch] Iteration 4: processing records 10-10 (1 records)
  [summary] Batch processing complete: 10 records in 4 batches

  Workflow ID: 3fa85f64-5542-4562-b3fc-2c963f66afa6

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {totalRecords=10, totalBatches=4, iterationsCompleted=4, summary=Batch processing complete: 10 records in 4 batches}

Result: PASSED

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/batch-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow batch_processing \
  --version 1 \
  --input '{"records": [{"id": 1, "data": "rec-1"}, {"id": 2, "data": "rec-2"}, {"id": 3, "data": "rec-3"}, {"id": 4, "data": "rec-4"}, {"id": 5, "data": "rec-5"}], "batchSize": 2}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w batch_processing -s COMPLETED -c 5

```

## How to Extend

Point the batch preparation worker at your real database or S3 bucket, replace the processing worker with your per-record logic, and the DO_WHILE-driven workflow runs unchanged.

- **`PrepareBatchesWorker`**: Query your database or S3 bucket to discover records, then partition them into batches based on the configured `batchSize`.

- **`ProcessBatchWorker`**: Replace the simulated processing with your actual per-record logic (database upserts, API calls, file transformations, message queue publishes).

- **`SummarizeWorker`**: Aggregate real processing metrics (success/failure counts, total processing time, records written) and write them to a reporting table or send a Slack notification.

Each worker preserves the batch count and record range contract, so the DO_WHILE loop and summarizer work regardless of the underlying processing logic.

**Add new stages** by inserting tasks into the `loopOver` array in `workflow.json`, for example, a validation step after each batch, a checkpoint writer for resumability, or a rate limiter to throttle API calls between batches.

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
batch-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/batchprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BatchProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PrepareBatchesWorker.java
│       ├── ProcessBatchWorker.java
│       └── SummarizeWorker.java
└── src/test/java/batchprocessing/workers/
    ├── PrepareBatchesWorkerTest.java
    ├── ProcessBatchWorkerTest.java
    └── SummarizeWorkerTest.java

```
