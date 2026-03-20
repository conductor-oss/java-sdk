# Ordered Message Processing in Java Using Conductor :  Receive, Sort by Sequence, Process in Order, Verify

A Java Conductor workflow example for ordered message processing .  receiving a batch of out-of-order messages, sorting them by their sequence number, processing them in strict order, and verifying that the processing order was correct. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Messages Arrive Out of Order, but Must Be Processed Sequentially

A financial trading system emits order updates .  placed, partially filled, fully filled, cancelled. These events arrive over a message queue that doesn't guarantee ordering. Processing a cancellation before the placement, or a fill before the partial fill, produces incorrect portfolio state. The sequence numbers are embedded in the messages, but reconstructing order from a shuffled batch requires buffering, sorting, and then processing each message one at a time.

Without guaranteed ordering, you'd build a resequencing buffer that holds messages until gaps are filled, implements timeout logic for missing sequences, and processes them strictly in order. That's a lot of state management for what should be a simple sort-then-process pipeline.

## The Solution

**You write the sorting and sequential processing logic. Conductor handles the ordering pipeline, retries, and sequence verification.**

`OprReceiveWorker` ingests the batch of out-of-order messages. `OprSortBySequenceWorker` resequences them by their partition key and sequence number, producing an ordered list. `OprProcessInOrderWorker` executes the business logic on each message strictly in sequence .  ensuring the placement is processed before the fill, and the fill before the cancellation. `OprVerifyOrderWorker` confirms that no sequence gaps exist and that the processing order matches the expected sequence. Conductor records the received order, sorted order, and verification result for every batch.

### What You Write: Workers

Four workers enforce sequential processing: message reception, sequence-number sorting, in-order execution, and order verification, ensuring events like placements and fills are never processed out of sequence.

| Worker | Task | What It Does |
|---|---|---|
| **OprProcessInOrderWorker** | `opr_process_in_order` | Processes messages sequentially in the sorted order and reports the processed sequence |
| **OprReceiveWorker** | `opr_receive` | Ingests out-of-order messages from input and counts them |
| **OprSortBySequenceWorker** | `opr_sort_by_sequence` | Reorders received messages by their sequence numbers to restore correct order |
| **OprVerifyOrderWorker** | `opr_verify_order` | Confirms that messages were processed in the correct sequence order |

Workers simulate the pattern behavior with realistic inputs and outputs so you can observe the advanced workflow mechanics. Replace with real implementations .  the pattern and Conductor orchestration stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
opr_receive
    │
    ▼
opr_sort_by_sequence
    │
    ▼
opr_process_in_order
    │
    ▼
opr_verify_order
```

## Example Output

```
=== Ordered Processing Demo ===

Step 1: Registering task definitions...
  Registered: opr_receive, opr_sort_by_sequence, opr_process_in_order, opr_verify_order

Step 2: Registering workflow 'opr_ordered_processing'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [process] Processing
  [receive] Processing
  [sort] Processing
  [verify] Processing

  Status: COMPLETED
  Output: {processedOrder=..., processedCount=..., receivedMessages=..., count=...}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/ordered-processing-1.0.0.jar
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
java -jar target/ordered-processing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow opr_ordered_processing \
  --version 1 \
  --input '{"messages": "Sample messages", "seq": "sample-seq", "data": "sample-data"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w opr_ordered_processing -s COMPLETED -c 5
```

## How to Extend

Each worker addresses one resequencing concern .  replace the simulated message sorting with real Kafka partition consumers or Redis sorted sets and the sort-then-process pipeline runs unchanged.

- **OprReceiveWorker** (`opr_receive`): consume real messages from a Kafka partition (where ordering is per-partition) or SQS FIFO queue with message group IDs
- **OprSortBySequenceWorker** (`opr_sort_by_sequence`): use a resequencing buffer backed by Redis sorted sets (`ZADD`/`ZRANGEBYSCORE`) for cross-batch ordering with gap detection
- **OprProcessInOrderWorker** (`opr_process_in_order`): execute real sequential business logic: apply financial transactions in order, replay event-sourced aggregates, or update database state in sequence-number order

The sorted-sequence output contract stays fixed. Swap the simulated message source for a real Kafka consumer or SQS reader and the sort-process-verify pipeline runs unchanged.

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
ordered-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/orderedprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OrderedProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── OprProcessInOrderWorker.java
│       ├── OprReceiveWorker.java
│       ├── OprSortBySequenceWorker.java
│       └── OprVerifyOrderWorker.java
└── src/test/java/orderedprocessing/workers/
    ├── OprProcessInOrderWorkerTest.java        # 4 tests
    ├── OprReceiveWorkerTest.java        # 4 tests
    ├── OprSortBySequenceWorkerTest.java        # 4 tests
    └── OprVerifyOrderWorkerTest.java        # 4 tests
```
