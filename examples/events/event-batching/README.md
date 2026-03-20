# Event Batching in Java Using Conductor

Event Batching .  collects events, creates batches, then processes each batch in a DO_WHILE loop. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to batch high-volume events into manageable chunks before processing. When events arrive continuously, processing them one by one is inefficient .  database inserts, API calls, and network round trips are much cheaper in batches. The workflow must collect incoming events, split them into fixed-size batches, and process each batch in a loop until all events are handled.

Without orchestration, you'd build a buffering service with manual batch-size management, a processing loop with error handling per batch, and recovery logic for partially processed batches .  hoping the buffer does not overflow and that a failed batch does not block all subsequent batches.

## The Solution

**You just write the event-collection, batch-creation, and batch-processing workers. Conductor handles DO_WHILE batch iteration, per-batch retry on failure, and durable progress tracking across all batches.**

Each batching concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of collecting events, creating batches, processing each batch in a DO_WHILE loop, retrying failed batches without blocking subsequent ones, and tracking every batch's processing status. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Three workers handle batch processing: CollectEventsWorker gathers incoming events, CreateBatchesWorker splits them into fixed-size chunks, and ProcessBatchWorker processes each chunk in a DO_WHILE loop.

| Worker | Task | What It Does |
|---|---|---|
| **CollectEventsWorker** | `eb_collect_events` | Collects incoming events and returns them along with a total count. |
| **CreateBatchesWorker** | `eb_create_batches` | Creates batches of events from the collected events list. |
| **ProcessBatchWorker** | `eb_process_batch` | Processes a single batch of events by index. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Loop execution** | DO_WHILE repeats a set of tasks until a condition is met |

### The Workflow

```
eb_collect_events
    │
    ▼
eb_create_batches
    │
    ▼
DO_WHILE
    └── eb_process_batch
```

## Example Output

```
=== Event Batching Demo ===

Step 1: Registering task definitions...
  Registered: eb_collect_events, eb_create_batches, eb_process_batch

Step 2: Registering workflow 'event_batching'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [eb_collect_events] Collected
  [eb_create_batches] Created
  [eb_process_batch] Processed batch

  Status: COMPLETED
  Output: {events=..., totalCount=..., batches=..., batchCount=...}

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
java -jar target/event-batching-1.0.0.jar
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
java -jar target/event-batching-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_batching \
  --version 1 \
  --input '{"events": ["item-1", "item-2", "item-3"]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_batching -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real event source, batching logic, and batch-processing endpoint (bulk database insert, API batch call), the collect-batch-process loop workflow stays exactly the same.

- **EbCollectEventsWorker** (`eb_collect_events`): consume events from your message broker (Kafka consumer group, SQS batch receive) with configurable polling intervals
- **EbCreateBatchesWorker** (`eb_create_batches`): implement intelligent batching strategies (fixed-size, time-window, or adaptive based on system load)
- Add a batch processing worker that performs bulk operations (JDBC batch inserts, bulk API calls, S3 multipart uploads) for each batch

Changing the batch size or pointing ProcessBatchWorker at a real bulk-insert API requires no changes to the batching workflow.

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
event-batching/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventbatching/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventBatchingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectEventsWorker.java
│       ├── CreateBatchesWorker.java
│       └── ProcessBatchWorker.java
└── src/test/java/eventbatching/workers/
    ├── CollectEventsWorkerTest.java        # 9 tests
    ├── CreateBatchesWorkerTest.java        # 10 tests
    └── ProcessBatchWorkerTest.java        # 10 tests
```
