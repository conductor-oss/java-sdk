# Event Schema Validation in Java Using Conductor

Event Schema Validation .  validate an incoming event against a named schema, then route valid events for processing or invalid events to a dead-letter queue via a SWITCH task. Uses [Conductor](https://github.## The Problem

You need to validate incoming events against a schema before processing them. Malformed events (missing required fields, wrong data types, extra fields) must be caught early and routed to a dead-letter queue rather than corrupting downstream systems. Valid events proceed to normal processing. Without schema validation, one malformed event can crash a consumer, corrupt a database, or produce silently incorrect results.

Without orchestration, you'd embed validation logic in every consumer, duplicate schema definitions across services, handle validation failures with try/catch and manual DLQ routing, and debug production issues caused by events that passed one consumer's loose validation but failed another's strict checks.

## The Solution

**You just write the schema-validation, valid-event processing, and dead-letter workers. Conductor handles valid/invalid SWITCH routing, guaranteed DLQ delivery for bad events, and schema compliance tracking for every event.**

Each validation concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of validating the event against the named schema, routing via a SWITCH task to processing (valid) or dead-letter (invalid), retrying if the schema registry is unavailable, and tracking every event's validation result. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Three workers enforce schema compliance: ValidateSchemaWorker checks the event against a named schema, ProcessValidWorker handles conforming events, and DeadLetterWorker routes malformed events to the DLQ with validation errors attached.

| Worker | Task | What It Does |
|---|---|---|
| **DeadLetterWorker** | `sv_dead_letter` | Sends an invalid event to the dead-letter queue along with its validation errors. |
| **ProcessValidWorker** | `sv_process_valid` | Processes a valid event that has passed schema validation. |
| **ValidateSchemaWorker** | `sv_validate_schema` | Validate Schema. Computes and returns errors, schema used |

Workers simulate event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources .  the workflow and routing logic stay the same.

### The Workflow

```
sv_validate_schema
    │
    ▼
SWITCH (route_ref)
    ├── valid: sv_process_valid
    ├── invalid: sv_dead_letter
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
java -jar target/event-schema-validation-1.0.0.jar
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
java -jar target/event-schema-validation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_schema_validation \
  --version 1 \
  --input '{"event": "test-value", "schemaName": "test"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_schema_validation -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real JSON Schema or Avro validator, event processing pipeline, and dead-letter queue, the validate-route-process-or-DLQ workflow stays exactly the same.

- **Schema validator**: validate against JSON Schema, Avro Schema Registry, or Protobuf descriptors using a real schema registry (Confluent, AWS Glue)
- **Processing worker**: implement your actual event processing logic for validated events
- **DLQ worker**: route invalid events to a dead-letter topic/queue with validation error details for debugging and remediation

Switching from simulated validation to a real schema registry (Confluent, Apicurio) requires no workflow changes.

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
event-schema-validation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventschemavalidation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventSchemaValidationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeadLetterWorker.java
│       ├── ProcessValidWorker.java
│       └── ValidateSchemaWorker.java
└── src/test/java/eventschemavalidation/workers/
    ├── DeadLetterWorkerTest.java        # 9 tests
    ├── ProcessValidWorkerTest.java        # 8 tests
    └── ValidateSchemaWorkerTest.java        # 11 tests
```
