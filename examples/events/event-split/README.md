# Event Split in Java Using Conductor

Splits a composite event into multiple sub-events for parallel processing using FORK_JOIN. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to decompose a composite event into multiple independent sub-events for parallel processing. A single incoming event may contain data for multiple downstream systems .  analytics, billing, and notification. Splitting the composite event and processing each sub-event in parallel reduces latency compared to sequential processing, and isolates failures so a billing error does not block notification delivery.

Without orchestration, you'd manually parse the composite event, extract sub-payloads, spawn threads for parallel processing, synchronize completion with barriers, and aggregate results .  handling the case where one sub-event processor fails while others succeed.

## The Solution

**You just write the event-receive, split, per-sub-event processing, and result-combination workers. Conductor handles parallel sub-event processing, per-processor retry isolation, and automatic result combination after all branches complete.**

Each sub-event processor is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of splitting the composite event, processing all sub-events in parallel via FORK_JOIN, aggregating results after all complete, retrying any failed sub-event processor independently, and tracking the entire split-and-process operation. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Six workers decompose and process composite events: ReceiveCompositeWorker ingests the event, SplitEventWorker breaks it into sub-events, ProcessSubAWorker, ProcessSubBWorker, and ProcessSubCWorker each handle one piece in parallel via FORK_JOIN, and CombineResultsWorker merges the outcomes.

| Worker | Task | What It Does |
|---|---|---|
| **CombineResultsWorker** | `sp_combine_results` | Combines results from all three parallel sub-event processors. |
| **ProcessSubAWorker** | `sp_process_sub_a` | Processes sub-event A (order details). |
| **ProcessSubBWorker** | `sp_process_sub_b` | Processes sub-event B (customer info). |
| **ProcessSubCWorker** | `sp_process_sub_c` | Processes sub-event C (shipping info). |
| **ReceiveCompositeWorker** | `sp_receive_composite` | Receives a composite event and passes it through. |
| **SplitEventWorker** | `sp_split_event` | Splits a composite event into three sub-events. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
sp_receive_composite
    │
    ▼
sp_split_event
    │
    ▼
FORK_JOIN
    ├── sp_process_sub_a
    ├── sp_process_sub_b
    └── sp_process_sub_c
    │
    ▼
JOIN (wait for all branches)
sp_combine_results
```

## Example Output

```
=== Event Split Demo ===

Step 1: Registering task definitions...
  Registered: sp_receive_composite, sp_split_event, sp_process_sub_a, sp_process_sub_b, sp_process_sub_c, sp_combine_results

Step 2: Registering workflow 'event_split'...
  Workflow registered.

Step 3: Starting workers...
  6 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [sp_combine_results] Combined results:
  [sp_process_sub_a] Processing:
  [sp_process_sub_b] Processing:
  [sp_process_sub_c] Processing:
  [sp_receive_composite] Composite event received
  [sp_split_event] Split into 3 sub-events: order_details, customer_info, shipping_info

  Status: COMPLETED
  Output: {results=..., result=..., subType=..., event=...}

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
java -jar target/event-split-1.0.0.jar
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
java -jar target/event-split-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_split \
  --version 1 \
  --input '{"compositeEvent": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_split -s COMPLETED -c 5
```

## How to Extend

Connect each sub-event processor to your real order service, customer system, and shipping API, the receive-split-parallel-process-combine workflow stays exactly the same.

- **Splitter worker**: implement domain-specific splitting logic (extract line items from an order, parse multi-tenant payloads, decompose batch uploads)
- **Sub-event processors**: implement real processing for each sub-event type (billing API calls, analytics ingestion, notification dispatch)
- **Aggregator**: merge processing results and handle partial failures (some sub-events succeeded, others failed)

Adding a new sub-event processor means one worker and a FORK_JOIN branch. Existing processors remain unchanged.

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
event-split/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventsplit/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventSplitExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CombineResultsWorker.java
│       ├── ProcessSubAWorker.java
│       ├── ProcessSubBWorker.java
│       ├── ProcessSubCWorker.java
│       ├── ReceiveCompositeWorker.java
│       └── SplitEventWorker.java
└── src/test/java/eventsplit/workers/
    ├── CombineResultsWorkerTest.java        # 8 tests
    ├── ProcessSubAWorkerTest.java        # 8 tests
    ├── ProcessSubBWorkerTest.java        # 8 tests
    ├── ProcessSubCWorkerTest.java        # 8 tests
    ├── ReceiveCompositeWorkerTest.java        # 8 tests
    └── SplitEventWorkerTest.java        # 8 tests
```
