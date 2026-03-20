# CQRS Pattern in Java with Conductor

CQRS pattern - Command side with validation, persistence, and read model update. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

CQRS (Command Query Responsibility Segregation) separates write operations from read operations. A command must be validated, its resulting event persisted to an event store, and the read model updated to reflect the new state. These steps must happen in order, the read model update depends on the persisted event.

Without orchestration, the command handler directly calls the event store and read-model updater in a single method, mixing concerns and making it hard to add new projections. If the read-model update fails after the event is persisted, the system is in an inconsistent state with no automatic retry.

## The Solution

**You just write the command validation, event persistence, and read-model projection workers. Conductor handles write-side sequencing, guaranteed projection updates via retries, and full command execution history.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Three command-side workers separate write concerns: ValidateCommandWorker enforces business rules, PersistEventWorker appends to the event store, and UpdateReadModelWorker projects the change into the read-optimized view.

| Worker | Task | What It Does |
|---|---|---|
| **PersistEventWorker** | `cqrs_persist_event` | Appends the validated event to the event store and returns the event ID. |
| **QueryReadModelWorker** | `cqrs_query_read_model` | Queries the read model to return the current state of an aggregate. |
| **UpdateReadModelWorker** | `cqrs_update_read_model` | Updates the read-side projection (denormalized view) to reflect the new event. |
| **ValidateCommandWorker** | `cqrs_validate_command` | Validates the incoming command against business rules and produces a domain event. |

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
cqrs_validate_command
    │
    ▼
cqrs_persist_event
    │
    ▼
cqrs_update_read_model
```

## Example Output

```
=== Example 308: CQRS Patter ===

Step 1: Registering task definitions...
  Registered: cqrs_validate_command, cqrs_persist_event, cqrs_update_read_model, cqrs_query_read_model

Step 2: Registering workflow 'cqrs_command_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [persist] Event stored:
  [query_read_model] Processing
  [read-model] Projection updated for
  [validate] Command:

  Status: COMPLETED
  Output: {eventId=..., persisted=..., result=..., updated=...}

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
java -jar target/cqrs-pattern-1.0.0.jar
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
java -jar target/cqrs-pattern-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cqrs_command_workflow \
  --version 1 \
  --input '{"command": "UPDATE_ITEM", "UPDATE_ITEM": "aggregateId", "aggregateId": "ITEM-100", "ITEM-100": "data", "data": {"key": "value"}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cqrs_command_workflow -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real event store (EventStoreDB, Kafka), read-model database (Elasticsearch, Postgres), and domain validation logic, the command-persist-project workflow stays exactly the same.

- **PersistEventWorker** (`cqrs_persist_event`): write to EventStoreDB, Kafka, or an append-only table in your relational database
- **QueryReadModelWorker** (`cqrs_query_read_model`): query Elasticsearch, a read-optimized Postgres view, or Redis for the current aggregate state
- **UpdateReadModelWorker** (`cqrs_update_read_model`): update a denormalized view in Elasticsearch, Redis, or a read-optimized Postgres table

Replacing the event store backend or adding a new read-model projection leaves the command pipeline definition intact.

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
cqrs-pattern/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/cqrspattern/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CqrsPatternExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PersistEventWorker.java
│       ├── QueryReadModelWorker.java
│       ├── UpdateReadModelWorker.java
│       └── ValidateCommandWorker.java
└── src/test/java/cqrspattern/workers/
    ├── PersistEventWorkerTest.java        # 2 tests
    ├── QueryReadModelWorkerTest.java        # 2 tests
    ├── UpdateReadModelWorkerTest.java        # 2 tests
    └── ValidateCommandWorkerTest.java        # 2 tests
```
