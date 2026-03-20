# Event Versioning in Java Using Conductor

Event versioning workflow that detects event schema version, transforms older versions to the latest format via a SWITCH task, and processes the event uniformly. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to handle events with different schema versions in the same processing pipeline. As your event schema evolves (adding fields, changing types, renaming properties), older producers may still emit events in v1 format while newer ones emit v2. The workflow must detect the event's schema version, transform older versions to the latest format, and then process all events uniformly regardless of their original version. Without version handling, schema changes break consumers.

Without orchestration, you'd embed version detection and transformation logic in every consumer, maintain transformation functions for every version pair, handle unknown versions with fallback logic, and risk consumers silently processing events in the wrong format when a version check is missed.

## The Solution

**You just write the version-detection, schema-transform, and event-processing workers. Conductor handles version-based SWITCH routing, per-version transformation retries, and full version lineage tracking for every event.**

Each versioning concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of detecting the event version, routing via a SWITCH task to the appropriate version transformer, processing the event in its canonical format, and tracking every event's version and transformation. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle schema evolution: DetectVersionWorker identifies the event's schema version, TransformV1Worker and TransformV2Worker upgrade older formats to v3, PassThroughWorker skips transformation for current-version events, and ProcessEventWorker handles the canonical format.

| Worker | Task | What It Does |
|---|---|---|
| **DetectVersionWorker** | `vr_detect_version` | Detects the schema version of an incoming event. |
| **PassThroughWorker** | `vr_pass_through` | Passes through an event that is already at the latest version. |
| **ProcessEventWorker** | `vr_process_event` | Processes a versioned event after transformation. |
| **TransformV1Worker** | `vr_transform_v1` | Transforms a v1 event to the latest (v3) schema format. |
| **TransformV2Worker** | `vr_transform_v2` | Transforms a v2 event to the latest (v3) schema format. |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
vr_detect_version
    │
    ▼
SWITCH (switch_ref)
    ├── v1: vr_transform_v1
    ├── v2: vr_transform_v2
    └── default: vr_pass_through
    │
    ▼
vr_process_event
```

## Example Output

```
=== Event Versioning Demo ===

Step 1: Registering task definitions...
  Registered: vr_detect_version, vr_transform_v1, vr_transform_v2, vr_pass_through, vr_process_event

Step 2: Registering workflow 'event_versioning'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [vr_detect_version] Event version:
  [vr_pass_through] Event already at latest version
  [vr_process_event] Processing event (original version:
  [vr_transform_v1] Converting v1 event to v3 format
  [vr_transform_v2] Converting v2 event to v3 format

  Status: COMPLETED
  Output: {version=..., transformed=..., processed=..., originalVersion=...}

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
java -jar target/event-versioning-1.0.0.jar
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
java -jar target/event-versioning-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_versioning \
  --version 1 \
  --input '{"event": "sample-event", "version": "sample-version", "v1": "sample-v1", "type": "standard", "user.created": "sample-user.created", "name": "sample-name", "John": "sample-John", "email": "user@example.com"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_versioning -s COMPLETED -c 5
```

## How to Extend

Connect each transform worker to your real schema migration logic and the processor to your current event handler, the detect-transform-process version-management workflow stays exactly the same.

- **Version detector**: read schema version from event headers, envelope fields, or schema registry (Confluent Schema Registry, AWS Glue)
- **Version transformers**: implement version-specific upcasting logic (v1->v2, v2->v3) using schema evolution libraries or custom mappers
- **Event processor**: process events in their canonical (latest) format, decoupled from version concerns

Adding a v4 transform requires one new worker and a SWITCH case. Existing version handlers and processing logic remain unchanged.

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
event-versioning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventversioning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventVersioningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectVersionWorker.java
│       ├── PassThroughWorker.java
│       ├── ProcessEventWorker.java
│       ├── TransformV1Worker.java
│       └── TransformV2Worker.java
└── src/test/java/eventversioning/workers/
    ├── DetectVersionWorkerTest.java        # 9 tests
    ├── PassThroughWorkerTest.java        # 8 tests
    ├── ProcessEventWorkerTest.java        # 8 tests
    ├── TransformV1WorkerTest.java        # 8 tests
    └── TransformV2WorkerTest.java        # 8 tests
```
