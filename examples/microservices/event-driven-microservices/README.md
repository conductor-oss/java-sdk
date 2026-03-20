# Event Driven Microservices in Java with Conductor

Event-driven microservices choreography via Conductor. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## The Problem

In an event-driven architecture, a domain event must be emitted, processed by business logic, used to update read-side projections, and fanned out to interested subscribers. Each step depends on the previous one. Subscribers cannot be notified until the event is processed, and the projection must reflect the latest state.

Without orchestration, event processing is scattered across message consumers with no unified view of the event lifecycle. If the projection update fails, the event is lost unless you build your own retry and dead-letter infrastructure.

## The Solution

**You just write the event-emit, processing, projection-update, and subscriber-notification workers. Conductor handles event pipeline sequencing, guaranteed projection updates, and full lifecycle visibility per event.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing .  your workers just make the service calls.

### What You Write: Workers

Four workers model the event lifecycle: EmitEventWorker publishes a domain event, ProcessEventWorker applies business logic, UpdateProjectionWorker refreshes the read-side view, and NotifySubscribersWorker fans out to downstream consumers.

| Worker | Task | What It Does |
|---|---|---|
| **EmitEventWorker** | `edm_emit_event` | Publishes a domain event with a type, payload, and source, returning a unique event ID. |
| **NotifySubscribersWorker** | `edm_notify_subscribers` | Sends notifications to all identified subscribers of the event. |
| **ProcessEventWorker** | `edm_process_event` | Processes the event by applying business logic and identifying downstream subscribers (e.g., billing, shipping, analytics). |
| **UpdateProjectionWorker** | `edm_update_projection` | Updates the read-side projection (materialized view) with the processed event data. |

Workers simulate service calls with realistic request/response shapes so you can see the coordination pattern without running the full service mesh. Replace with real HTTP clients .  the workflow coordination stays the same.

### The Workflow

```
edm_emit_event
    │
    ▼
edm_process_event
    │
    ▼
edm_update_projection
    │
    ▼
edm_notify_subscribers
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
java -jar target/event-driven-microservices-1.0.0.jar
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
java -jar target/event-driven-microservices-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow event_driven_microservices \
  --version 1 \
  --input '{"eventType": "test-value", "payload": "test-value", "source": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w event_driven_microservices -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real event bus (Kafka, RabbitMQ, SNS/SQS), domain logic layer, and notification services, the emit-process-project-notify workflow stays exactly the same.

- **EmitEventWorker** (`edm_emit_event`): publish to Kafka, RabbitMQ, AWS SNS/SQS, or your event bus
- **NotifySubscribersWorker** (`edm_notify_subscribers`): send notifications via webhook, email (SES/SendGrid), or push notification services
- **ProcessEventWorker** (`edm_process_event`): apply real business rules (e.g., calculate shipping cost, update loyalty points)

Switching the event bus from a mock to Kafka or RabbitMQ does not alter the emit-process-project-notify workflow.

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
event-driven-microservices/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/eventdrivenmicroservices/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── EventDrivenMicroservicesExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmitEventWorker.java
│       ├── NotifySubscribersWorker.java
│       ├── ProcessEventWorker.java
│       └── UpdateProjectionWorker.java
└── src/test/java/eventdrivenmicroservices/workers/
    ├── EmitEventWorkerTest.java        # 2 tests
    ├── NotifySubscribersWorkerTest.java        # 2 tests
    ├── ProcessEventWorkerTest.java        # 2 tests
    └── UpdateProjectionWorkerTest.java        # 2 tests
```
