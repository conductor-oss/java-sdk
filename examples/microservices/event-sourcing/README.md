# Event Sourcing in Java with Conductor

Event sourcing with append-only event log and state rebuild. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

Event sourcing persists every state change as an immutable event rather than overwriting the current state. When a new event arrives, it must be validated against business rules, appended to the event log, the current state rebuilt by replaying all events, and the event published to downstream consumers.

Without orchestration, the validate-append-rebuild-publish pipeline is implemented in a single service method with no separation of concerns. If the state rebuild fails after the event is appended, downstream consumers see a stale projection, and there is no retry mechanism.

## The Solution

**You just write the event validation, append, state-rebuild, and publish workers. Conductor handles append ordering, state-rebuild retry on failure, and a durable record of every event lifecycle.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers maintain the event-sourced aggregate: ValidateEventWorker checks business rules, AppendEventWorker writes to the immutable log, RebuildStateWorker replays events to compute current state, and PublishEventWorker notifies downstream consumers.

| Worker | Task | What It Does |
|---|---|---|
| **AppendEventWorker** | `es_append_event` | Appends the validated event to the append-only event log and returns the event ID and version number. |
| **PublishEventWorker** | `es_publish_event` | Publishes the persisted event to downstream subscribers for async processing. |
| **RebuildStateWorker** | `es_rebuild_state` | Replays all events for the aggregate to rebuild the current state (e.g., balance, status). |
| **ValidateEventWorker** | `es_validate_event` | Validates the incoming event against business rules for the aggregate and produces a timestamped event payload. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
es_validate_event
    │
    ▼
es_append_event
    │
    ▼
es_rebuild_state
    │
    ▼
es_publish_event
```

## Example Output

```
=== Example 309: Event Sourcing ===

Step 1: Registering task definitions...
  Registered: es_validate_event, es_append_event, es_rebuild_state, es_publish_event

Step 2: Registering workflow 'event_sourcing_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [append] Event
  [publish] Published
  [rebuild] State rebuilt from
  [validate]

  Status: COMPLETED
  Output: {eventId=..., version=..., appended=..., published=...}

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
  --workflow event_sourcing_workflow \
  --version 1 \
  --input '{"aggregateId": "ACCT-200", "ACCT-200": "eventType", "eventType": "DEPOSIT", "DEPOSIT": "eventData", "eventData": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_sourcing_workflow -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real event store (EventStoreDB, Kafka), aggregate replay logic, and downstream consumers, the validate-append-rebuild-publish workflow stays exactly the same.

- **AppendEventWorker** (`es_append_event`): write to EventStoreDB, a Kafka topic, or an append-only table in Postgres
- **PublishEventWorker** (`es_publish_event`): publish to Kafka, RabbitMQ, or AWS EventBridge for downstream consumer processing
- **RebuildStateWorker** (`es_rebuild_state`): replay events from the event store to compute current aggregate state with real domain logic

Replacing the event store or adding a new downstream consumer leaves the validate-append-rebuild-publish chain unchanged.

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
│       ├── AppendEventWorker.java
│       ├── PublishEventWorker.java
│       ├── RebuildStateWorker.java
│       └── ValidateEventWorker.java
└── src/test/java/eventsourcing/workers/
    ├── AppendEventWorkerTest.java        # 2 tests
    ├── PublishEventWorkerTest.java        # 2 tests
    ├── RebuildStateWorkerTest.java        # 2 tests
    └── ValidateEventWorkerTest.java        # 2 tests
```
