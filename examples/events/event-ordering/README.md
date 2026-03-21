# Event Ordering in Java Using Conductor

Event Ordering. buffers incoming events, sorts them by sequence number, and processes each in order using a DO_WHILE loop. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to process events in strict sequence order, even when they arrive out of order. In distributed systems, events with sequence numbers 1, 3, 2, 5, 4 may arrive in any order due to network jitter or parallel producers. The workflow must buffer incoming events, sort them by sequence number, and process each one in order using a loop. Processing out-of-order events corrupts state in systems that depend on causal ordering (e.g., bank transactions, state machines).

Without orchestration, you'd build a reordering buffer with a priority queue, manage watermarks to know when all events for a window have arrived, handle gaps in sequence numbers, and process each event in a loop. manually ensuring the buffer does not grow unbounded and that late arrivals are handled correctly.

## The Solution

**You just write the event-buffering, sorting, and sequential-processing workers. Conductor handles DO_WHILE sequential processing, durable buffer state, and per-event retry within the ordered loop.**

Each ordering concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of buffering events, sorting them by sequence number, processing each in order via a DO_WHILE loop, retrying any failed processing step, and tracking the entire ordering operation. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers enforce causal ordering: BufferEventsWorker collects incoming events, SortEventsWorker arranges them by sequence number, and ProcessNextWorker handles each one sequentially in a DO_WHILE loop.

| Worker | Task | What It Does |
|---|---|---|
| **BufferEventsWorker** | `oo_buffer_events` | Buffers incoming events. accepts a list of events and outputs them as a buffered collection along with the total count. |
| **ProcessNextWorker** | `oo_process_next` | Processes the next event from the sorted list based on the current DO_WHILE iteration index. Gets the event at positi... |
| **SortEventsWorker** | `oo_sort_events` | Sorts buffered events by the "seq" field in ascending order. For determinism, always returns the fixed sorted order: ... |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
oo_buffer_events
    │
    ▼
oo_sort_events
    │
    ▼
DO_WHILE
    └── oo_process_next

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
java -jar target/event-ordering-1.0.0.jar

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
java -jar target/event-ordering-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_ordering \
  --version 1 \
  --input '{"events": "sample-events"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_ordering -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real event buffer, sequence-sorting logic, and ordered-processing handler, the buffer-sort-process ordering workflow stays exactly the same.

- **Buffer worker**: consume events from your message broker with configurable watermark and late-arrival policies
- **Sort worker**: implement sorting by sequence number, timestamp, or custom ordering keys with gap detection
- **Processing worker**: execute order-dependent business logic (apply state transitions, process financial transactions, update materialized views)

Changing the sort key or processing logic inside any worker preserves the buffer-sort-process ordering pipeline.

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
event-ordering/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventordering/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventOrderingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BufferEventsWorker.java
│       ├── ProcessNextWorker.java
│       └── SortEventsWorker.java
└── src/test/java/eventordering/workers/
    ├── BufferEventsWorkerTest.java        # 8 tests
    ├── ProcessNextWorkerTest.java        # 9 tests
    └── SortEventsWorkerTest.java        # 9 tests

```
