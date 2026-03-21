# Event Dedup in Java Using Conductor

Event deduplication workflow. computes a hash of the event payload, checks if the event has been seen before, and either processes or skips the event via a SWITCH task. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to deduplicate events so that the same event is never processed twice. In distributed systems, at-least-once delivery guarantees mean consumers may receive duplicate messages. The workflow must compute a content hash of the event payload, check whether that hash has been seen before, and either process the event (if new) or skip it (if duplicate). Processing duplicates can lead to double charges, duplicate notifications, or corrupted state.

Without orchestration, you'd maintain a deduplication cache (in-memory set, Redis, database table), manually check before processing each event, handle race conditions in concurrent consumers, and deal with cache eviction that causes previously-seen events to be reprocessed.

## The Solution

**You just write the hash-computation, seen-check, event-processing, and skip workers. Conductor handles SWITCH-based duplicate routing, guaranteed dedup decisions, and a full record of every event's dedup outcome.**

Each deduplication concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of computing the hash, checking the dedup store, routing via a SWITCH task to process or skip, and tracking every event's dedup decision. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers enforce exactly-once event processing: ComputeHashWorker generates a content fingerprint, CheckSeenWorker queries the dedup store, ProcessEventWorker handles new events, and SkipEventWorker discards duplicates.

| Worker | Task | What It Does |
|---|---|---|
| **CheckSeenWorker** | `dd_check_seen` | Checks whether a given hash has been seen before (demo lookup). Always returns "duplicate" to demonstrate a previou... |
| **ComputeHashWorker** | `dd_compute_hash` | Computes a deterministic hash of the event payload for deduplication. Uses a fixed hash value for demonstration purpo... |
| **ProcessEventWorker** | `dd_process_event` | Processes a new (non-duplicate) event. |
| **SkipEventWorker** | `dd_skip_event` | Skips a duplicate (or unknown-status) event. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
dd_compute_hash
    │
    ▼
dd_check_seen
    │
    ▼
SWITCH (dedup_switch_ref)
    ├── new: dd_process_event
    ├── duplicate: dd_skip_event
    └── default: dd_skip_event

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
java -jar target/event-dedup-1.0.0.jar

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
java -jar target/event-dedup-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_dedup \
  --version 1 \
  --input '{"eventId": "TEST-001", "eventPayload": {"key": "value"}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_dedup -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real hash function, dedup store (Redis, DynamoDB), and event processing logic, the hash-check-process-or-skip deduplication workflow stays exactly the same.

- **DdComputeHashWorker** (`dd_compute_hash`): compute a content-addressable hash (SHA-256, MurmurHash) of the event payload for fingerprinting
- **DdCheckSeenWorker** (`dd_check_seen`): check a distributed dedup store (Redis SET with TTL, DynamoDB conditional writes, Bloom filter) for the computed hash
- **DdProcessEventWorker** (`dd_process_event`): execute your actual event processing logic and mark the hash as seen in the dedup store
- **DdSkipEventWorker** (`dd_skip_event`): log skipped duplicates to your metrics system for dedup rate monitoring

Migrating the dedup store from in-memory to Redis or DynamoDB does not alter the hash-check-process-or-skip workflow.

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
event-dedup/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventdedup/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventDedupExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckSeenWorker.java
│       ├── ComputeHashWorker.java
│       ├── ProcessEventWorker.java
│       └── SkipEventWorker.java
└── src/test/java/eventdedup/workers/
    ├── CheckSeenWorkerTest.java        # 8 tests
    ├── ComputeHashWorkerTest.java        # 8 tests
    ├── ProcessEventWorkerTest.java        # 8 tests
    └── SkipEventWorkerTest.java        # 9 tests

```
