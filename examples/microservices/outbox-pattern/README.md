# Outbox Pattern in Java with Conductor

Transactional outbox pattern for reliable event publishing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

When a service writes to its database and needs to publish an event, doing both atomically is impossible across different systems (database + message broker). The transactional outbox pattern writes the event to an outbox table in the same database transaction as the entity change, then a separate process polls the outbox, publishes events to the message broker, and marks them as published.

Without orchestration, outbox polling is implemented as a scheduled job with no visibility into which events are pending, which failed to publish, or how long events sit unpublished. Duplicate publishing is common without careful at-least-once/exactly-once handling.

## The Solution

**You just write the outbox-write, poll, event-publish, and mark-published workers. Conductor handles ordered outbox processing, guaranteed delivery via retries, and a durable record of every publish attempt.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers implement the transactional outbox: WriteWithOutboxWorker atomically persists the entity and an outbox entry, PollOutboxWorker reads unpublished events, PublishEventWorker delivers them to the broker, and MarkPublishedWorker prevents reprocessing.

| Worker | Task | What It Does |
|---|---|---|
| **MarkPublishedWorker** | `ob_mark_published` | Marks the outbox entry as published so it is not processed again. |
| **PollOutboxWorker** | `ob_poll_outbox` | Polls the outbox table for unpublished events and returns the event payload and destination topic. |
| **PublishEventWorker** | `ob_publish_event` | Publishes the event to the message broker (e.g., Kafka topic) and returns a message ID. |
| **WriteWithOutboxWorker** | `ob_write_with_outbox` | Writes the entity change and an outbox entry in a single atomic database transaction. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
ob_write_with_outbox
    │
    ▼
ob_poll_outbox
    │
    ▼
ob_publish_event
    │
    ▼
ob_mark_published
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
java -jar target/outbox-pattern-1.0.0.jar
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
java -jar target/outbox-pattern-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow outbox_pattern_workflow \
  --version 1 \
  --input '{"entityId": "TEST-001", "entityData": "test-value", "eventType": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w outbox_pattern_workflow -s COMPLETED -c 5
```

## How to Extend

Wire each worker to your real database outbox table, Kafka or RabbitMQ broker, and publish-tracking logic, the write-poll-publish-mark workflow stays exactly the same.

- **MarkPublishedWorker** (`ob_mark_published`): update the outbox table row to set published=true or delete it after successful broker delivery
- **PollOutboxWorker** (`ob_poll_outbox`): query the outbox table with SELECT .. FOR UPDATE SKIP LOCKED for concurrent-safe polling
- **PublishEventWorker** (`ob_publish_event`): publish to Kafka, RabbitMQ, or AWS SNS/SQS using your messaging client

Replacing the message broker from a mock to Kafka or RabbitMQ requires no changes to the write-poll-publish-mark flow.

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
outbox-pattern/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/outboxpattern/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OutboxPatternExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── MarkPublishedWorker.java
│       ├── PollOutboxWorker.java
│       ├── PublishEventWorker.java
│       └── WriteWithOutboxWorker.java
└── src/test/java/outboxpattern/workers/
    ├── MarkPublishedWorkerTest.java        # 2 tests
    ├── PollOutboxWorkerTest.java        # 2 tests
    ├── PublishEventWorkerTest.java        # 2 tests
    └── WriteWithOutboxWorkerTest.java        # 2 tests
```
