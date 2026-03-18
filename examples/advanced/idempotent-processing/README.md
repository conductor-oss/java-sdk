# Idempotent Message Processing in Java Using Conductor: Check, Process-or-Skip, Record

Kafka delivers a payment event. Your service processes it, charges the customer $49.99, and then: network blip, the acknowledgment never reaches the broker. Kafka redelivers. Your service processes it again. The customer is now out $99.98 and your support queue has a new ticket. At-least-once delivery guarantees duplicates will arrive; the only question is whether your system charges twice or catches the replay. This example builds an idempotent processing pipeline with Conductor: check a dedup store before doing any work, route new messages to processing and duplicates to a skip path via a `SWITCH` task, and record every message ID so future replays are caught. The example submits the same message twice to prove both paths. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Duplicates Are Inevitable, Double-Processing Is Not

Message queues guarantee at-least-once delivery, which means your consumer will see the same message more than once. After a network timeout, a consumer restart, or a broker failover. If your processing logic charges a credit card, sends an email, or updates an inventory count, processing the same message twice produces incorrect results.

Idempotent processing solves this by checking a deduplication store before doing any work. If the message ID has been seen before, the workflow returns the previous result without re-executing the business logic. If it's new, the workflow processes it normally. Either way, the final state is recorded so future duplicates are caught. The `SWITCH` task makes this branch explicit. Unprocessed messages go to `idp_process`, already-processed messages go to `idp_skip`.

## The Solution

**You write the dedup check and processing logic. Conductor handles the process-or-skip routing, retries, and execution history.**

`CheckProcessedWorker` looks up the message ID in the deduplication store and returns the processing state. `unprocessed` if the message is new, `processed` if it's been seen before (along with the previous result hash). A `SWITCH` task routes accordingly: new messages go to `ProcessWorker` for execution, duplicates go to `SkipWorker` which returns the cached result. `RecordWorker` persists the message ID in the dedup store so future duplicates are caught. Conductor makes the dedup-then-route pattern declarative, and every execution records whether the message was processed or skipped.

### What You Write: Workers

Four workers implement the dedup-or-process pattern. Duplicate detection, business logic execution for new messages, skip-with-cached-result for duplicates, and dedup store recording.

| Worker | Task | What It Does |
|---|---|---|
| **CheckProcessedWorker** | `idp_check_processed` | Looks up the messageId in the in-memory `DedupStore`. Returns `processingState=unprocessed` for new messages, `processingState=processed` (with previous result hash) for duplicates. |
| **ProcessWorker** | `idp_process` | Executes business logic for new messages. Produces a deterministic SHA-256 hash of the messageId as the result. Same input always produces the same output. |
| **RecordWorker** | `idp_record` | Writes the messageId and resultHash to the `DedupStore` so future runs with the same messageId are detected as duplicates. |
| **SkipWorker** | `idp_skip` | Returns the skip reason and the previous result hash for messages that have already been processed. No business logic is re-executed. |

The `DedupStore` is a `ConcurrentHashMap` shared across workers within the same JVM. To go to production, swap it for Redis, DynamoDB, or a database, the worker interface stays the same, and no workflow changes are needed.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
idp_check_processed
    │
    ▼
SWITCH (idp_switch_ref)
    ├── unprocessed: idp_process
    ├── processed: idp_skip
    └── default: idp_process
    │
    ▼
idp_record
```

## Example Output

The example submits message `msg-abc-123` twice. The first run processes it, the second detects the duplicate and skips:

```
=== Idempotent Processing Demo ===

Step 1: Registering task definitions...
  Tasks registered.

Step 2: Registering workflow 'idp_idempotent_processing'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

=== Run 1: First submission of msg-abc-123 ===

[idp_check_processed] Message msg-abc-123: NOT previously processed
[idp_process] Processing message msg-abc-123...
[idp_record] Recording msg-abc-123 as processed
  Status: COMPLETED

=== Run 2: Duplicate submission of msg-abc-123 ===

[idp_check_processed] Message msg-abc-123: ALREADY processed. Skipping
[idp_skip] Skipping duplicate message msg-abc-123
[idp_record] Recording msg-abc-123 as processed
  Status: COMPLETED

Result: PASSED. Both runs completed, duplicate was detected on Run 2
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
java -jar target/idempotent-processing-1.0.0.jar
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
java -jar target/idempotent-processing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow idp_idempotent_processing \
  --version 1 \
  --input '{"messageId": "msg-abc-123", "payload": "order data"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w idp_idempotent_processing -s COMPLETED -c 5
```

## How to Extend

Each worker is a standalone class with a simple interface. The `DedupStore` is an in-memory `ConcurrentHashMap`. Swap it for a durable, distributed store and you're in production:

- **Redis for distributed dedup**: replace `DedupStore.isProcessed()` with `EXISTS messageId` and `DedupStore.record()` with `SET messageId resultHash`. Redis gives you sub-millisecond lookups and optional TTL-based expiry for old message IDs.
- **Database-backed processing log**: use a `processed_messages` table with columns `(message_id PRIMARY KEY, result_hash, processed_at)`. `CheckProcessedWorker` does `SELECT`, `RecordWorker` does `INSERT`. You get durability, queryability, and audit trails.
- **Message queue acknowledgment patterns**: combine with Kafka consumer offsets or SQS visibility timeouts. Process the message, record it in the dedup store, then acknowledge. If the consumer crashes before acknowledgment, the redelivered message hits the dedup check and is skipped.

The check-process-record interface contract stays fixed. Only the dedup store implementation behind each worker changes, not the workflow routing.

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
idempotent-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/idempotentprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── IdempotentProcessingExample.java  # Main entry point (submits same message twice)
│   └── workers/
│       ├── DedupStore.java          # In-memory ConcurrentHashMap dedup store
│       ├── CheckProcessedWorker.java # Looks up messageId in DedupStore
│       ├── ProcessWorker.java       # Deterministic processing (SHA-256 hash)
│       ├── RecordWorker.java        # Records messageId in DedupStore
│       └── SkipWorker.java          # Returns skip reason + previous result
└── src/test/java/idempotentprocessing/workers/
    ├── CheckProcessedWorkerTest.java # 4 tests: new, recorded, independent, task name
    ├── ProcessWorkerTest.java        # 4 tests: deterministic, different, fields, task name
    ├── RecordWorkerTest.java         # 3 tests: marks processed, verified via check, task name
    └── SkipWorkerTest.java           # 4 tests: reason, messageId, previousResult, task name
```
