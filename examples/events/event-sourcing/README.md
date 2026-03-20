# Event Sourcing in Java Using Conductor

Event Sourcing .  load event log, append new event, rebuild aggregate state, and snapshot for a bank account aggregate. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to implement event sourcing for a domain aggregate (e.g., a bank account). Instead of storing current state, you store a log of all events that led to the current state. The workflow loads the existing event log for an aggregate, appends a new event, rebuilds the current state by replaying all events in order, and snapshots the rebuilt state for fast future lookups. Without event sourcing, you lose the history of how state was derived and cannot audit or debug state transitions.

Without orchestration, you'd build an event store with append-only writes, implement a replay loop to rebuild state, manage snapshot intervals, and handle concurrent appends with optimistic concurrency .  all in a single service that mixes storage, replay, and snapshot logic.

## The Solution

**You just write the event-log load, append, state-rebuild, and snapshot workers. Conductor handles append ordering, crash-safe state rebuild, and a durable record of every event-sourcing lifecycle.**

Each event-sourcing concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of loading the event log, appending the new event, rebuilding the aggregate state, and snapshotting ,  retrying if the event store is unavailable, tracking every state rebuild, and resuming if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers manage the event-sourced aggregate: LoadEventLogWorker reads the existing event history, AppendNewEventWorker adds a new domain event, RebuildStateWorker replays all events to compute current state, and SnapshotStateWorker persists the rebuilt state for fast hydration.

| Worker | Task | What It Does |
|---|---|---|
| **AppendNewEventWorker** | `ev_append_new_event` | Appends a new domain event to the existing event list. Assigns the next sequence number and a fixed timestamp, then r... |
| **LoadEventLogWorker** | `ev_load_event_log` | Loads the event log for a given aggregate. Returns a fixed set of four bank-account domain events plus metadata about... |
| **RebuildStateWorker** | `ev_rebuild_state` | Replays all events to rebuild the current aggregate state. Uses deterministic logic: for the standard 5-event bank ac... |
| **SnapshotStateWorker** | `ev_snapshot_state` | Takes a snapshot of the current aggregate state for fast future hydration. Returns a fixed snapshot ID and timestamp. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
ev_load_event_log
    │
    ▼
ev_append_new_event
    │
    ▼
ev_rebuild_state
    │
    ▼
ev_snapshot_state
```

## Example Output

```
=== Event Sourcing Demo ===

Step 1: Registering task definitions...
  Registered: ev_load_event_log, ev_append_new_event, ev_rebuild_state, ev_snapshot_state

Step 2: Registering workflow 'event_sourcing_wf'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [ev_append_new_event] Appending event seq
  [ev_load_event_log] Loading event log for
  [ev_rebuild_state] Rebuilding
  [ev_snapshot_state] Snapshotting

  Status: COMPLETED
  Output: {sequence=..., type=..., timestamp=..., data=...}

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
java -jar target/event-sourcing-1.0.0.jar
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
java -jar target/event-sourcing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_sourcing_wf \
  --version 1 \
  --input '{"aggregateId": "acct-1001", "acct-1001": "aggregateType", "aggregateType": "BankAccount", "BankAccount": "newEvent", "newEvent": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_sourcing_wf -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real event store (EventStoreDB, Postgres append-only table), aggregate replay logic, and snapshot storage, the load-append-rebuild-snapshot workflow stays exactly the same.

- **Event log loader**: read from your event store (EventStoreDB, Kafka, DynamoDB, PostgreSQL append-only table) with snapshot-based fast-forward
- **Event appender**: append new events with optimistic concurrency control (version checking) to prevent lost writes
- **State rebuilder**: replay events through your domain aggregate's apply() methods to reconstruct current state
- **Snapshot worker**: periodically snapshot aggregate state to your snapshot store (Redis, S3, DynamoDB) for faster future rebuilds

Replacing the in-memory event log with EventStoreDB or a Kafka topic leaves the load-append-rebuild-snapshot workflow intact.

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
event-sourcing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventsourcing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventSourcingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AppendNewEventWorker.java
│       ├── LoadEventLogWorker.java
│       ├── RebuildStateWorker.java
│       └── SnapshotStateWorker.java
└── src/test/java/eventsourcing/workers/
    ├── AppendNewEventWorkerTest.java        # 9 tests
    ├── LoadEventLogWorkerTest.java        # 9 tests
    ├── RebuildStateWorkerTest.java        # 10 tests
    └── SnapshotStateWorkerTest.java        # 9 tests
```
